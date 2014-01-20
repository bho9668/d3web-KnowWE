/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package de.d3web.we.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import de.d3web.core.inference.LoopTerminator;
import de.d3web.core.inference.LoopTerminator.LoopStatus;
import de.d3web.core.inference.SessionTerminatedException;
import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.TerminologyObject;
import de.d3web.core.knowledge.ValueObject;
import de.d3web.core.knowledge.terminology.NamedObject;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.knowledge.terminology.Rating;
import de.d3web.core.knowledge.terminology.Solution;
import de.d3web.core.manage.KnowledgeBaseUtils;
import de.d3web.core.records.SessionConversionFactory;
import de.d3web.core.records.SessionRecord;
import de.d3web.core.records.io.SessionPersistenceManager;
import de.d3web.core.session.Session;
import de.d3web.core.session.Value;
import de.d3web.core.session.blackboard.Blackboard;
import de.d3web.core.session.blackboard.Fact;
import de.d3web.core.session.values.ChoiceValue;
import de.d3web.core.session.values.UndefinedValue;
import de.d3web.core.session.values.Unknown;
import de.d3web.scoring.Score;
import de.d3web.strings.Identifier;
import de.d3web.strings.Strings;
import de.d3web.utils.Log;
import de.d3web.we.knowledgebase.D3webCompiler;
import de.d3web.we.object.AnswerDefinition;
import de.d3web.we.object.D3webTerm;
import de.knowwe.core.ArticleManager;
import de.knowwe.core.Attributes;
import de.knowwe.core.Environment;
import de.knowwe.core.compile.AbstractPackageCompiler;
import de.knowwe.core.compile.Compiler;
import de.knowwe.core.compile.Compilers;
import de.knowwe.core.compile.packaging.PackageCompileType;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.event.EventManager;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.report.Message;
import de.knowwe.core.taghandler.TagHandlerType;
import de.knowwe.core.user.UserContext;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.d3web.event.FindingSetEvent;
import de.knowwe.notification.NotificationManager;
import de.knowwe.notification.StandardNotification;

public class D3webUtils {

	static ArrayList<String> possibleScorePoints;

	public static List<String> getPossibleScores() {
		if (possibleScorePoints == null) {

			possibleScorePoints = new ArrayList<String>();

			String n = "N";
			String p = "P";
			for (int i = 1; i <= 7; i++) {
				possibleScorePoints.add(n + i);
				possibleScorePoints.add(p + i);
			}
			possibleScorePoints.add("P5x");
			possibleScorePoints.add("N5x");
			possibleScorePoints.add("!");
			possibleScorePoints.add("?");
			possibleScorePoints.add("excluded");
			possibleScorePoints.add("established");
			possibleScorePoints.add("suggested");
		}
		return possibleScorePoints;
	}

	public static D3webCompiler getD3webCompiler(Section<?> section) {
		return Compilers.getCompiler(section, D3webCompiler.class);
	}

	@Deprecated
	public static D3webCompiler getD3webCompiler(Article master) {
		return Compilers.getCompiler(master, D3webCompiler.class);
	}

	public static void storeSessionRecordsAsAttachment(String user, Collection<Session> sessions, String attachmentArticle, String attachmentName) throws IOException {

		Collection<SessionRecord> sessionRecords = new LinkedList<SessionRecord>();
		for (Session session : sessions) {
			sessionRecords.add(SessionConversionFactory.copyToSessionRecord(session));
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		SessionPersistenceManager.getInstance().saveSessions(outputStream, sessionRecords);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

		Environment.getInstance().getWikiConnector()
				.storeAttachment(attachmentArticle, attachmentName, user, inputStream);
	}

	public static Score getScoreForString(String argument) {
		for (Score sc : Score.getAllScores()) {
			if (sc.getSymbol().equals(argument)) {
				return sc;
			}
		}
		if (argument.equals("!")) {
			return Score.P7;
		}
		if (argument.equals("?")) {
			return Score.P5;
		}
		if (argument.equalsIgnoreCase("excluded")) {
			return Score.N7;
		}
		if (argument.equalsIgnoreCase("established")) {
			return Score.P7;
		}
		if (argument.equalsIgnoreCase("suggested")) {
			return Score.P5;
		}

		return null;
	}

	/**
	 * Deletes a terminology object and all potential children from the
	 * knowledge base. Before the deletion the corresponding knowledge instances
	 * (KnowledgeSlices) are also removed.
	 * 
	 * @param object the object to be removed
	 */
	public static void removeRecursively(TerminologyObject object) {
		for (TerminologyObject to : object.getChildren()) {
			removeRecursively(to);
		}
		object.destroy();
	}

	public static KnowledgeBase getKnowledgeBase(Compiler compiler) {
		return getKnowledgeBase(((AbstractPackageCompiler) compiler).getCompileSection());
	}

	public static Collection<KnowledgeBase> getKnowledgeBases(ArticleManager manager) {
		Collection<D3webCompiler> compilers = Compilers.getCompilers(manager, D3webCompiler.class);
		Collection<KnowledgeBase> kbs = new ArrayList<KnowledgeBase>(compilers.size());
		for (D3webCompiler d3webCompiler : compilers) {
			kbs.add(d3webCompiler.getKnowledgeBase());
		}
		return kbs;
	}

	/**
	 * Utility method to get a {@link KnowledgeBase} for a specified article.
	 * 
	 * @created 15.12.2010
	 * @param article the article the knowledge base is compiled
	 * @return the knowledge base if such one exists, null otherwise
	 * @throws NullPointerException if the article is null
	 */
	@Deprecated
	public static KnowledgeBase getKnowledgeBase(Article article) {
		return getKnowledgeBase(article.getWeb(), article.getTitle());
	}

	/**
	 * Utility method to get a {@link KnowledgeBase} for an article specified by
	 * its web and topic. If no such knowledge base exists, a new knowledge base
	 * is created for the article and returned.
	 * 
	 * @created 15.12.2010
	 * @param web the web of the article the knowledge base is compiled
	 * @param title the title of the article the knowledge base is compiled
	 * @return the knowledge base if such one exists, null otherwise
	 * @throws NullPointerException if web or topic is null
	 * @deprecated it is possible that there are multiple knowledge bases on one
	 *             page, this method will always only return the first one. You
	 *             can use
	 */
	@Deprecated
	public static KnowledgeBase getKnowledgeBase(String web, String title) {
		Article article = Environment.getInstance().getArticle(web, title);
		if (article == null) return null;
		Section<PackageCompileType> compileSection = Sections.findSuccessor(
				article.getRootSection(),
				PackageCompileType.class);
		if (compileSection == null) return null;
		D3webCompiler d3webCompiler = getD3webCompiler(compileSection);
		if (d3webCompiler == null) return null;
		return d3webCompiler.getKnowledgeBase();
	}

	/**
	 * If the given section is compiled by a {@link D3webCompiler} or is a
	 * section of the type {@link PackageCompileType} belonging to a knowledge
	 * base markup, the knowledge base of the right {@link D3webCompiler} is
	 * returned. If a {@link TagHandlerType} is given, the {@link KnowledgeBase}
	 * of the same article is returned for compatibility reasons.
	 * 
	 * @created 06.01.2014
	 */
	public static KnowledgeBase getKnowledgeBase(Section<?> section) {
		if (section.get() instanceof TagHandlerType) {
			return D3webUtils.getKnowledgeBase(section.getArticle());
		}
		else {
			return getD3webCompiler(section).getKnowledgeBase();
		}
	}

	@SuppressWarnings("unchecked")
	public static <TermObject extends NamedObject> TermObject getTermObjectDefaultImplementation(D3webCompiler compiler, Section<? extends D3webTerm<TermObject>> section) {
		Identifier termIdentifier = section.get().getTermIdentifier(section);
		KnowledgeBase kb = D3webUtils.getKnowledgeBase(compiler);
		NamedObject termObject = kb.getManager().search(termIdentifier.getLastPathElement());
		if (termObject != null) {
			if (section.get().getTermObjectClass(section).isAssignableFrom(termObject.getClass())) {
				return (TermObject) termObject;
			}
			else {
				return null;
			}
		}
		Collection<NamedObject> foundTermObjects = getTermObjectsIgnoreTermObjectClass(
				compiler, section);
		if (foundTermObjects.size() == 1) {
			termObject = foundTermObjects.iterator().next();
			if (section.get().getTermObjectClass(section).isAssignableFrom(termObject.getClass())) {
				return (TermObject) termObject;
			}
		}
		return null;
	}

	public static <TermObject extends NamedObject> Collection<NamedObject> getTermObjectsIgnoreTermObjectClass(D3webCompiler compiler, Section<? extends D3webTerm<TermObject>> section) {
		Identifier termIdentifier = section.get().getTermIdentifier(section);
		TerminologyManager terminologyHandler = compiler.getTerminologyManager();
		KnowledgeBase kb = D3webUtils.getKnowledgeBase(compiler);
		Collection<Identifier> allTermsEqualIgnoreCase = terminologyHandler.getAllTermsEqualIgnoreCase(termIdentifier);
		List<NamedObject> foundTermObjects = new ArrayList<NamedObject>();
		for (Identifier termEqualIgnoreCase : allTermsEqualIgnoreCase) {
			NamedObject termObject = null;
			if (section.get() instanceof AnswerDefinition) {
				String[] pathElements = termEqualIgnoreCase.getPathElements();
				if (pathElements.length != 2) continue;
				TerminologyObject question = kb.getManager().search(pathElements[0]);
				if (question == null || !(question instanceof QuestionChoice)) continue;
				QuestionChoice questionChoice = (QuestionChoice) question;
				termObject = KnowledgeBaseUtils.findChoice(questionChoice, pathElements[1], false);
			}
			else {
				termObject = kb.getManager().search(termEqualIgnoreCase.getLastPathElement());
			}
			if (termObject != null) foundTermObjects.add(termObject);
		}
		return foundTermObjects;
	}

	/**
	 * Returns the current value of a specific {@link ValueObject} within the
	 * specified {@link Session}. If the value is currently being calculated,
	 * the method immediately returns with "null" as value, instead of waiting
	 * for the results of the current propagation.
	 * 
	 * @created 05.10.2012
	 * @param session the session to read the value from
	 * @param object the object to read the value for
	 * @return the value of the value object
	 */
	public static Value getValueNonBlocking(Session session, ValueObject object) {
		// variant 1: conservative
		// only access values if we are not in a propagation. This is very
		// secure to do, but shows no value at all, even if this value is
		// not propagated at all
		// if (session.getPropagationManager().isInPropagation()) return null;
		// synchronized (session) {
		// return session.getBlackboard().getValue((ValueObject) object);
		// }

		// variant 2: aggressive
		// access the value without synchronization but catch the expected error
		// if this access violates concurrency
		try {
			return session.getBlackboard().getValue(object);
		}
		catch (ConcurrentModificationException e) {
			return null;
		}
	}

	public static void setFindingSynchronized(Fact fact, Session session, UserContext context) {
		try {
			synchronized (session) {
				session.getBlackboard().addValueFact(fact);
				session.touch();
			}
		}
		catch (SessionTerminatedException e) {
			Log.warning("Unable to set fact, because the current session is " +
					"terminated (possibly due to a detected propagation loop).",
					e);
		}
		EventManager.getInstance().fireEvent(new FindingSetEvent(fact, session, context));
	}

	/**
	 * Returns the current value of a specific {@link Solution} within the
	 * specified {@link Session}. If the value is currently being calculated,
	 * the method immediately returns with "null" as value, instead of waiting
	 * for the results of the current propagation.
	 * 
	 * @created 05.10.2012
	 * @param session the session to read the value from
	 * @param solution the object to read the value for
	 * @return the value of the value object
	 */
	public static Rating getRatingNonBlocking(Session session, Solution solution) {
		// variant 1: conservative
		// only access values if we are not in a propagation. This is very
		// secure to do, but shows no value at all, even if this value is
		// not propagated at all
		// if (session.getPropagationManager().isInPropagation()) return null;
		// synchronized (session) {
		// return session.getBlackboard().getRating(solution);
		// }

		// variant 2: aggressive
		// access the value without synchronization but catch the expected error
		// if this access violates concurrency
		try {
			return session.getBlackboard().getRating(solution);
		}
		catch (ConcurrentModificationException e) {
			return null;
		}

	}

	/**
	 * Returns all {@link Solution} instances, that hold the specified
	 * {@link Rating} in the specified session. If the value of any solution
	 * currently is being calculated, the method immediately returns with "null"
	 * instead of the list, not waiting for the results of the current
	 * propagation.
	 * 
	 * @param session the session to read the current values from
	 * @param state the Rating the diagnoses must have to be returned
	 * @return a list of diagnoses in this case that have the state 'state' or
	 *         null if any of these solutions is currently being calculated
	 */
	public static List<Solution> getSolutionsNonBlocking(Session session, Rating.State state) {
		try {
			return session.getBlackboard().getSolutions(state);
		}
		catch (ConcurrentModificationException e) {
			return null;
		}
	}

	/**
	 * Returns all {@link Question} instances, that have been answered (or
	 * assigned with a value) in the specified session. If the value of any
	 * solution currently is being calculated, the method immediately returns
	 * with "null" instead of the list, not waiting for the results of the
	 * current propagation.
	 * 
	 * @param session the session to read the current answers from
	 * @return a list of diagnoses in this case that have the state 'state' or
	 *         null if any of these solutions is currently being calculated
	 */
	public static List<Question> getAnsweredQuestionsNonBlocking(Session session) {
		try {
			return session.getBlackboard().getAnsweredQuestions();
		}
		catch (ConcurrentModificationException e) {
			return null;
		}
	}

	public static String getSessionPath(UserContext context) {
		String user = context.getParameter(Attributes.USER);
		String web = context.getParameter(Attributes.WEB);
		ResourceBundle rb = KnowWEUtils.getConfigBundle();
		String sessionDir = rb.getString("knowwe.config.path.sessions");
		sessionDir = sessionDir.replaceAll("\\$web\\$", web);
		sessionDir = sessionDir.replaceAll("\\$user\\$", user);
		sessionDir = KnowWEUtils.getRealPath(sessionDir);
		return sessionDir;
	}

	public static String getWebEnvironmentPath(String web) {
		ResourceBundle rb = KnowWEUtils.getConfigBundle();
		String sessionDir = rb.getString("knowwe.config.path.currentWeb");
		sessionDir = sessionDir.replaceAll("\\$web\\$", web);
		sessionDir = KnowWEUtils.getRealPath(sessionDir);
		return sessionDir;
	}

	public static URL getKnowledgeBaseURL(String web, String id) {
		String varPath = getWebEnvironmentPath(web);
		varPath = varPath + id + ".jar";
		URL url = null;
		try {
			url = new File(varPath).toURI().toURL();
		}
		catch (MalformedURLException e) {
			Log.warning("Cannot identify url for knowledgebase", e);
		}
		return url;
	}

	public static ResourceBundle getD3webBundle() {
		return ResourceBundle.getBundle("KnowWE_plugin_d3web_messages");
	}

	public static ResourceBundle getD3webBundle(HttpServletRequest request) {
		if (request == null) return D3webUtils.getD3webBundle();
		Locale.setDefault(Environment.getInstance().getWikiConnector()
				.getLocale(request));
		return D3webUtils.getD3webBundle();
	}

	public static ResourceBundle getD3webBundle(UserContext user) {
		if (user == null) return getD3webBundle();
		Locale.setDefault(Environment.getInstance().getWikiConnector()
				.getLocale(user.getRequest()));
		return getD3webBundle();
	}

	public static void handleLoopDetectionNotification(ArticleManager manager, UserContext context,
			Session session) {
		LoopStatus loopStatus =
				LoopTerminator.getInstance().getLoopStatus(session);
		if (loopStatus.hasTerminated()) {
			String notificationId = generateNotificationId(session);
			Collection<TerminologyObject> loopObjects = loopStatus.getLoopObjects();
			String notificationText = getLoopNotificationText(manager, context, session,
					loopObjects);
			StandardNotification notification = new StandardNotification(
					notificationText, Message.Type.WARNING, notificationId);
			NotificationManager.addNotification(context, notification);
		}
	}

	private static String getLoopNotificationText(ArticleManager manager, UserContext user, Session session, Collection<TerminologyObject> loopObjects) {
		String kbName = session.getKnowledgeBase().getName();
		if (kbName == null) kbName = session.getKnowledgeBase().getId();
		String kbUrlLink = KnowWEUtils.getURLLinkToTermDefinition(manager, new Identifier(kbName));
		kbName = "<a href=\"" + toAbsolutURL(kbUrlLink) + "\">" + kbName + "</a>";
		Collection<String> renderedObjects = new ArrayList<String>(loopObjects.size());
		for (TerminologyObject loopObject : loopObjects) {
			String url = KnowWEUtils.getURLLinkToTermDefinition(manager, new Identifier(
					loopObject.getName()));
			renderedObjects.add("<a href=\"" + toAbsolutURL(url) + "\">" + loopObject.getName()
					+ "</a>");
		}
		String notificationText = "Endless loop detected in knowledge base '"
				+ kbName
				+ "'. The following object"
				+ (loopObjects.size() == 1 ? " is" : "s are")
				+ " mainly involved in the loop: " +
				Strings.concat(",  ", renderedObjects) + ".";
		return notificationText;
	}

	private static String toAbsolutURL(String url) {
		return Environment.getInstance().getWikiConnector().getBaseUrl() + url;
	}

	public static void removedLoopDetectionNotification(UserContext context, Session session) {
		String notificationId = generateNotificationId(session);
		NotificationManager.removeNotification(context, notificationId);
	}

	private static String generateNotificationId(Session session) {
		return session.getId() + "_loop_detected";
	}

	/**
	 * This is a utility method for dialogs. This method returns the
	 * {@link Value} you should set to the {@link Blackboard} if the Value
	 * already exists:<br/>
	 * If it is the same value, {@link Unknown} is returned.<br/>
	 * If it is equal but Unknown, <tt>null</tt> is returned since there is
	 * nothing to change.<br/>
	 * If it is a different Value, the Value is returned unaltered.
	 * 
	 * @created 11.08.2012
	 * @param newValue the newly created Value for the dialog
	 * @param existingValue the existing Value in the dialog
	 * @return the Value you should set to the dialog
	 */
	public static Value handleEqualChoiceValues(Value newValue, Value existingValue) {
		if (newValue instanceof ChoiceValue && newValue.equals(existingValue)) {
			newValue = Unknown.getInstance();
		}
		else if (Unknown.getInstance().equals(newValue)
				&& Unknown.getInstance().equals(existingValue)) {
			newValue = UndefinedValue.getInstance();
		}
		return newValue;
	}

	/**
	 * Checks if the knowledge base is empty. A knowledge base is empty, if
	 * there are no knowledge slices. no questions and at most one solution
	 * (root solution).
	 * 
	 * @created 19.04.2013
	 * @param kb
	 * @return
	 */
	public static boolean isEmpty(KnowledgeBase kb) {
		if (kb.getAllKnowledgeSlices().size() == 0
				&& kb.getManager().getQContainers().size() <= 1
				&& kb.getManager().getSolutions().size() <= 1) {
			return true;
		}
		else {
			return false;
		}
	}

}
