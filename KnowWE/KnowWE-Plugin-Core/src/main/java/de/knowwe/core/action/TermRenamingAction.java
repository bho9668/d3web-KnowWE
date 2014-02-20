/*
 * Copyright (C) 2010 University Wuerzburg, Computer Science VI
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
package de.knowwe.core.action;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import de.d3web.strings.Identifier;
import de.knowwe.core.ArticleManager;
import de.knowwe.core.Attributes;
import de.knowwe.core.Environment;
import de.knowwe.core.compile.terminology.RenamableTerm;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.utils.KnowWEUtils;

/**
 * Action which renames all Definitions and References of a given Term. The following parameters are mandatory! <ul>
 * <li>termname</li> <li>termreplacement</li> <li>web</li> </ul>
 *
 * @author Sebastian Furth
 * @created Dec 15, 2010
 */
public class TermRenamingAction extends AbstractAction {

	public static final String TERMNAME = "termname";
	public static final String REPLACEMENT = "termreplacement";

	@SuppressWarnings("unchecked")
	@Override
	public void execute(UserActionContext context) throws IOException {

		String web = context.getParameter(Attributes.WEB);
		String term = context.getParameter(TERMNAME);
		String replacement = context.getParameter(REPLACEMENT);
		String force = context.getParameter("force");

		Identifier termIdentifierOld = Identifier.fromExternalForm(context.getParameter("termIdentifier"));
		String[] pathElements = termIdentifierOld.getPathElements();
		pathElements[pathElements.length - 1] = replacement;
		Identifier replacementIdentifier = new Identifier(pathElements);

		if (force.equals("false")
				&& getTerms(web).contains(replacementIdentifier)) {
			JSONObject response = new JSONObject();
			try {
				response.append("alreadyexists", "true");
				boolean sameTerm = replacementIdentifier.toExternalForm().equals(
						new Identifier(term).toExternalForm());
				response.append("same", String.valueOf(sameTerm));
				response.write(context.getWriter());
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
			return;
		}

		Identifier termIdentifier = Identifier.fromExternalForm(term);
		Identifier replacmentIdentifier = createReplacingIdentifier(termIdentifier, replacement);

		Map<String, Set<Section<? extends RenamableTerm>>> allTerms = new HashMap<String, Set<Section<? extends RenamableTerm>>>();

		Iterator<Article> iter = Environment.getInstance()
				.getArticleManager(web).getArticleIterator();
		Article currentArticle;

		while (iter.hasNext()) {
			currentArticle = iter.next();
			Collection<TerminologyManager> terminologyManagers = KnowWEUtils.getTerminologyManagers(
					KnowWEUtils.getArticleManager(
							currentArticle.getWeb()));
			for (TerminologyManager terminologyManager : terminologyManagers) {

				// terminologyManager = KnowWEUtils
				// .getGlobalTerminologyManager(currentArticle.getWeb());
				// Check if there is a TermDefinition
				Collection<Section<?>> definingSections = terminologyManager
						.getTermDefiningSections(termIdentifier);
				for (Section<?> definition : definingSections) {
					if (definition.get() instanceof RenamableTerm) {
						getTermSet(definition.getTitle(), allTerms).add(
								(Section<? extends RenamableTerm>) definition);
					}
				}

				// Check if there are References
				Collection<Section<?>> references = terminologyManager
						.getTermReferenceSections(termIdentifier);
				for (Section<?> reference : references) {
					if (reference.get() instanceof RenamableTerm) {
						getTermSet(reference.getTitle(), allTerms).add(
								(Section<? extends RenamableTerm>) reference);
					}
				}
			}
		}

		ArticleManager mgr = Environment.getInstance().getArticleManager(web);
		Set<String> failures = new HashSet<String>();
		Set<String> success = new HashSet<String>();
		renameTerms(allTerms, termIdentifier, replacmentIdentifier, mgr, context, failures, success);
		writeResponse(failures, success, termIdentifier, replacmentIdentifier, context);
	}

	private Set<Section<? extends RenamableTerm>> getTermSet(String title,
															 Map<String, Set<Section<? extends RenamableTerm>>> allTerms) {
		Set<Section<? extends RenamableTerm>> terms = allTerms.get(title);
		if (terms == null) {
			terms = new HashSet<Section<? extends RenamableTerm>>();
			allTerms.put(title, terms);
		}
		return terms;
	}

	private void writeResponse(Set<String> failures, Set<String> success,
							   Identifier termIdentifier, Identifier replacement,
							   UserActionContext context) throws IOException {

		JSONObject response = new JSONObject();
		try {
			// the new external form of the TermIdentifier
			String[] pathElements = termIdentifier.getPathElements();
			String newLastPathElement = replacement.getLastPathElement();
			pathElements[pathElements.length - 1] = newLastPathElement;
			response.append("newTermIdentifier", new Identifier(
					pathElements).toExternalForm());

			// the new object name
			response.append("newObjectName", new Identifier(
					newLastPathElement).toExternalForm());

			// renamed Articles
			StringBuilder renamedArticles = new StringBuilder();
			// successes
			for (String article : success) {
				renamedArticles.append("##");
				renamedArticles.append(article);
			}
			renamedArticles.append("###");
			// failures
			for (String article : failures) {
				renamedArticles.append("##");
				renamedArticles.append(article);
			}
			response.append("alreadyexists", false);
			response.accumulate("renamedArticles", renamedArticles);

			response.write(context.getWriter());
		}
		catch (JSONException e) {
			throw new IOException(e.getMessage());
		}
	}

	private void renameTerms(
			Map<String, Set<Section<? extends RenamableTerm>>> allTerms, Identifier term,
			Identifier replacement, ArticleManager mgr, UserActionContext context,
			Set<String> failures, Set<String> success) throws IOException {
		mgr.open();
		try {
			for (String title : allTerms.keySet()) {
				if (Environment.getInstance().getWikiConnector()
						.userCanEditArticle(title, context.getRequest())) {
					Map<String, String> nodesMap = new HashMap<String, String>();
					for (Section<? extends RenamableTerm> termSection : allTerms.get(title)) {
						nodesMap.put(
								termSection.getID(),
								termSection.get().getSectionTextAfterRename(termSection, term,
										replacement));
					}
					Sections.replaceSections(context, nodesMap).sendErrors(context);
					success.add(title);
				}
				else {
					failures.add(title);
				}
			}
		}
		finally {
			mgr.commit();
		}
	}

	private Identifier createReplacingIdentifier(Identifier oldIdentifier, String text) {
		String[] elements = oldIdentifier.getPathElements();
		elements[elements.length - 1] = text;
		return new Identifier(elements);
	}

	public Set<Identifier> getTerms(String web) {
		// gathering all terms
		Set<Identifier> allTerms = new HashSet<Identifier>();
		Collection<TerminologyManager> terminologyManagers = KnowWEUtils.getTerminologyManagers(KnowWEUtils.getArticleManager(web));
		for (TerminologyManager terminologyManager : terminologyManagers) {
			Collection<Identifier> allDefinedTerms = terminologyManager
					.getAllDefinedTerms();
			allTerms.addAll(allDefinedTerms);
		}
		return allTerms;
	}
}
