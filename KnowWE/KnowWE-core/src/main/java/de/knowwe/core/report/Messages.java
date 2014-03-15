/*
 * Copyright (C) 2010 Chair of Artificial Intelligence and Applied Informatics
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

package de.knowwe.core.report;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeSet;

import de.d3web.strings.Strings;
import de.d3web.utils.Log;
import de.knowwe.core.Environment;
import de.knowwe.core.compile.CompileScript;
import de.knowwe.core.compile.Compiler;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.user.UserContext;
import de.knowwe.core.utils.KnowWEUtils;

public final class Messages {

	private static final String MESSAGE_KEY = "message_map_key";

	/**
	 * Wraps a single or more {@link Message}s into a Collection to be returned
	 * by the {@link CompileScript} implementations. {@link Message}
	 * 
	 * @created 16.08.2010
	 * @param messages the {@link Message}(s) to be wrapped
	 * @return the wrapped {@link Message}(s)
	 */
	public static Collection<Message> asList(Message... messages) {
		return Arrays.asList(messages);
	}

	/**
	 * Removes all {@link Message}s from the given source stored for this
	 * Section and article.
	 * 
	 * @created 01.12.2011
	 * @param compiler the {@link Compiler} the {@link Message}s are stored for
	 * @param section the {@link Section} the {@link Message}s are stored for
	 * @param source the source the {@link Message}s are stored for
	 */
	public static void clearMessages(Compiler compiler, Section<? extends Type> section, Class<?> source) {
		storeMessages(compiler, section, source, Messages.noMessage());
	}

	/**
	 * Removes all {@link Message}s from the given source stored for this
	 * Section and compiler independently.
	 * 
	 * @created 01.12.2011
	 * @param section is the Section the {@link Message}s are stored for
	 * @param source is the source the {@link Message}s are stored for
	 */
	public static void clearMessages(Section<? extends Type> section, Class<?> source) {
		storeMessages(null, section, source, Messages.noMessage());
	}

	/**
	 * Removes all {@link Message}s from all sources for stored for this Section
	 * and article.
	 * 
	 * @created 01.12.2011
	 * @param compiler the {@link Compiler} the {@link Message}s are stored for
	 * @param section is the {@link Section} the {@link Message}s are stored for
	 */
	public static void clearMessages(Compiler compiler, Section<?> section) {
		if (section.getSectionStore().isEmpty()) return;
		Map<String, Collection<Message>> msgsMap = getMessagesMapModifiable(compiler, section);
		if (msgsMap != null) msgsMap.clear();
	}

	/**
	 * Clears all {@link Message}s for the given article and subtree.
	 * 
	 * @param compiler is the article you want to clear the message for
	 * @param sec is the root of the subtree you want to clear the message for
	 */
	public static void clearMessagesRecursively(Compiler compiler, Section<?> sec) {
		clearMessages(compiler, sec);
		for (Section<?> child : sec.getChildren()) {
			clearMessagesRecursively(compiler, child);
		}
	}

	public static Message creationFailedWarning(String name) {
		return Messages.warning("Failed to create: " + name);
	}

	/**
	 * Creates and returns a {@link Message} of the
	 * {@link de.knowwe.core.report.Message.Type} ERROR with the given text.
	 * 
	 * @created 01.12.2011
	 * @param message is the text content of the created {@link Message}
	 */
	public static Message error(String message) {
		return new Message(Message.Type.ERROR, message);
	}

	/**
	 * Creates and returns a {@link Message} of the
	 * {@link de.knowwe.core.report.Message.Type} ERROR with the given
	 * exception.
	 * 
	 * @created 01.12.2011
	 * @param e the exception to take the message text from
	 */
	public static Message error(Exception e) {
		return new Message(Message.Type.ERROR, e.getMessage());
	}

	/**
	 * Filters the given Collection of {@link Message}s and returns a new
	 * Collection containing only {@link Message}s of the
	 * {@link de.knowwe.core.report.Message.Type} ERROR.
	 * 
	 * @created 01.12.2011
	 */
	public static Collection<Message> getErrors(Collection<Message> messages) {
		Collection<Message> errors = new ArrayList<Message>(messages.size());
		for (Message msg : messages) {
			if (msg.getType() == Message.Type.ERROR) errors.add(msg);
		}
		return errors;
	}

	/**
	 * Returns an unmodifiable {@link Map} with Collections of all
	 * {@link Message}s of the given {@link de.knowwe.core.report.Message.Type}s
	 * stored for the given {@link Section}. The Collections are mapped by the
	 * {@link Compiler} the {@link Message}s were stored for. If
	 * {@link Message}s were stored without an argument {@link Compiler} , the
	 * {@link Map} will contain this {@link Collection} with <tt>null</tt> as
	 * the <tt>key</tt>.
	 * 
	 * @created 16.02.2012
	 * @param section is the {@link Section} the {@link Message}s are stored for
	 * @param types is the {@link de.knowwe.core.report.Message.Type} of
	 *        {@link Message} you want (set to <tt>null</tt> if you want all)
	 */
	public static Map<Compiler, Collection<Message>> getMessagesMap(Section<? extends Type> section, Message.Type... types) {
		if (section.getSectionStore().isEmpty()) return Collections.emptyMap();
		Map<Compiler, Collection<Message>> allMsgsOfTitle = new HashMap<Compiler, Collection<Message>>();
		Map<Compiler, Object> msgsOfAllTypesBySourceByTitle = section.getSectionStore().getObjects(MESSAGE_KEY);
		for (Entry<Compiler, Object> entry : msgsOfAllTypesBySourceByTitle.entrySet()) {
			@SuppressWarnings("unchecked")
			Map<Compiler, Collection<Message>> msgsOfAllTypesBySource = (Map<Compiler, Collection<Message>>) entry.getValue();
			Collection<Message> msgsOfGivenTypesOfAllSourcesOfTitle = new ArrayList<Message>();
			for (Collection<Message> msgsOfAllTypesOfSource : msgsOfAllTypesBySource.values()) {
				addAllMessagesOfTypes(msgsOfAllTypesOfSource, msgsOfGivenTypesOfAllSourcesOfTitle,
						types);
			}
			if (!msgsOfGivenTypesOfAllSourcesOfTitle.isEmpty()) {
				allMsgsOfTitle.put(entry.getKey(), msgsOfGivenTypesOfAllSourcesOfTitle);
			}
		}
		return Collections.unmodifiableMap(allMsgsOfTitle);
	}

	/**
	 * Checks if there are any messages of the specified type or the specified
	 * types sub-tree, for any compiler and/or independent of any compiler.
	 * 
	 * @created 06.02.2014
	 * @param section the root section of the sub-tree to be checked
	 * @param types the error messages considered
	 * @return if there are any such messages
	 */
	public static boolean hasMessagesInSubtree(Section<? extends Type> section, Message.Type... types) {
		List<Section<?>> subtreeSections = Sections.getSubtreePreOrder(section);
		List<Compiler> compilers = new LinkedList<Compiler>();
		compilers.add(null);
		compilers.addAll(section.getArticleManager().getCompilerManager().getCompilers());
		for (Section<?> subtreeSection : subtreeSections) {
			for (Compiler compiler : compilers) {
				if (!getMessages(compiler, subtreeSection, types).isEmpty()) return true;
			}
		}
		return false;
	}

	/**
	 * Checks if there are any messages of the specified type, for any compiler
	 * and/or independent of any compiler.
	 * 
	 * @created 06.02.2014
	 * @param section the root section of the subtree to be checked
	 * @param types the error messages considered
	 * @return if there are any such messages
	 */
	public static boolean hasMessages(Section<? extends Type> section, Message.Type... types) {
		if (!getMessages(null, section, types).isEmpty()) return true;
		List<Compiler> compilers = section.getArticleManager().getCompilerManager().getCompilers();
		for (Compiler compiler : compilers) {
			if (!getMessages(compiler, section, types).isEmpty()) return true;
		}
		return false;
	}

	/**
	 * Returns an unmodifiable Collection containing all {@link Message}s of the
	 * given {@link de.knowwe.core.report.Message.Type}s stored for this article
	 * and Section.
	 * 
	 * @created 01.12.2011
	 * @param compiler the {@link Compiler} the {@link Message}s are stored for
	 * @param section is the {@link Section} the {@link Message}s are stored for
	 * @param types is the {@link de.knowwe.core.report.Message.Type} of
	 *        {@link Message} you want (set to <tt>null</tt> if you want all)
	 */
	public static Collection<Message> getMessages(Compiler compiler,
			Section<?> section,
			Message.Type... types) {
		if (section.getSectionStore().isEmpty()) return Collections.emptyList();
		Collection<Message> allMsgs = new ArrayList<Message>();
		Map<String, Collection<Message>> msgMapModifiable = getMessagesMapModifiable(compiler,
				section);
		if (msgMapModifiable != null) {
			for (Collection<Message> msgs : msgMapModifiable.values()) {
				addAllMessagesOfTypes(msgs, allMsgs, types);
			}
		}
		return Collections.unmodifiableCollection(allMsgs);
	}

	/**
	 * Returns an unmodifiable Collection containing all {@link Message}s of the
	 * given {@link de.knowwe.core.report.Message.Type}s stored for this article
	 * and Section.
	 * 
	 * @created 01.12.2011
	 * @param section is the {@link Section} the {@link Message}s are stored for
	 * @param types is the {@link de.knowwe.core.report.Message.Type} of
	 *        {@link Message} you want (set to <tt>null</tt> if you want all)
	 */
	public static Collection<Message> getMessages(Section<?> section,
			Message.Type... types) {
		return getMessages(null, section, types);
	}

	/**
	 * Returns an unmodifiable Collection containing all {@link Message}s of the
	 * given {@link de.knowwe.core.report.Message.Type}s stored for this
	 * article, section, and source.
	 * 
	 * @created 01.12.2011
	 * @param compiler is the article the {@link Message}s are stored for
	 * @param section is the Section the {@link Message}s are stored for
	 * @param source is the source the {@link Message}s are stored for
	 * @param types is the {@link de.knowwe.core.report.Message.Type} of
	 *        {@link Message} you want (set to <tt>null</tt> if you want all)
	 */
	public static Collection<Message> getMessages(Compiler compiler, Section<?> section,
			Class<?> source, Message.Type... types) {
		if (section.getSectionStore().isEmpty()) return Collections.emptyList();
		Map<String, Collection<Message>> msgsMap = getMessagesMapModifiable(compiler, section);
		List<Message> allMsgs = new ArrayList<Message>();
		if (msgsMap != null) {
			Collection<Message> msgs = msgsMap.get(source.getName());
			addAllMessagesOfTypes(msgs, allMsgs, types);
		}
		return Collections.unmodifiableCollection(allMsgs);
	}

	/**
	 * Returns an unmodifiable Collection containing all {@link Message}s of the
	 * given {@link de.knowwe.core.report.Message.Type}s stored for this section
	 * and source, independently of any {@link Compiler}
	 * 
	 * @created 01.12.2011
	 * @param section is the Section the {@link Message}s are stored for
	 * @param source is the source the {@link Message}s are stored for
	 * @param types is the {@link de.knowwe.core.report.Message.Type} of
	 *        {@link Message} you want (set to <tt>null</tt> if you want all)
	 */
	public static Collection<Message> getMessages(Section<?> section,
			Class<?> source, Message.Type... types) {
		return getMessages(null, section, source, types);
	}

	/**
	 * Returns an unmodifiable {@link Map} with Collections of all
	 * {@link Message}s of the given {@link de.knowwe.core.report.Message.Type}s
	 * stored in the KDOM subtree with the given {@link Section} as root. The
	 * Collections are mapped by the title of the {@link Article} the
	 * {@link Message}s were stored for. If {@link Message}s were stored without
	 * an argument {@link Article}, the {@link Map} will contain this
	 * {@link Collection} with <tt>null</tt> as the <tt>key</tt>.
	 * 
	 * @created 16.02.2012
	 * @param section is the {@link Section} the {@link Message}s are stored for
	 * @param types is the {@link de.knowwe.core.report.Message.Type} of
	 *        {@link Message} you want (set to <tt>null</tt> if you want all)
	 */
	public static Map<Compiler, Collection<Message>> getMessagesMapFromSubtree(Section<?> section, Message.Type... types) {
		Map<Compiler, Collection<Message>> allMessages = new HashMap<Compiler, Collection<Message>>();
		getMessagesMapFromSubtree(allMessages, section, types);
		return Collections.unmodifiableMap(allMessages);
	}

	private static void getMessagesMapFromSubtree(Map<Compiler, Collection<Message>> allMessages, Section<?> section, Message.Type... types) {
		Map<Compiler, Collection<Message>> messagesOfSectionByTitle = getMessagesMap(section, types);
		for (Entry<Compiler, Collection<Message>> entry : messagesOfSectionByTitle.entrySet()) {
			Collection<Message> allMsgsOfTitle = allMessages.get(entry.getKey());
			if (allMsgsOfTitle == null) {
				allMsgsOfTitle = new LinkedList<Message>();
				allMessages.put(entry.getKey(), allMsgsOfTitle);
			}
			allMsgsOfTitle.addAll(entry.getValue());
		}
		for (Section<?> child : section.getChildren()) {
			getMessagesMapFromSubtree(allMessages, child, types);
		}
	}

	/**
	 * Returns an unmodifiable Collection containing all {@link Message}s of the
	 * given {@link de.knowwe.core.report.Message.Type}s of the KDOM subtree
	 * with the given Section as root.
	 * 
	 * @param compiler the {@link Compiler} the {@link Message}s are stored for
	 * @param section is the root of the KDOM subtree you want the messages from
	 * @param types is the {@link de.knowwe.core.report.Message.Type} of
	 *        {@link Message} you want (set to <tt>null</tt> if you want all)
	 */
	public static Collection<Message> getMessagesFromSubtree(Compiler compiler,
			Section<?> section,	Message.Type... types) {

		Collection<Message> messages = new ArrayList<Message>();
		getMessagesFromSubtree(messages, compiler, section, types);
		return Collections.unmodifiableCollection(messages);
	}

	private static void getMessagesFromSubtree(Collection<Message> messages, Compiler compiler, Section<?> section, Message.Type... types) {
		messages.addAll(getMessages(compiler, section, types));
		for (Section<?> child : section.getChildren()) {
			getMessagesFromSubtree(messages, compiler, child, types);
		}
	}

	/**
	 * Returns an unmodifiable Collection containing all {@link Message}s of the
	 * given {@link de.knowwe.core.report.Message.Type}s of the KDOM subtree
	 * with the given Section as root, independently of any compiler.
	 * 
	 * @param section is the root of the KDOM subtree you want the messages from
	 * @param types is the {@link de.knowwe.core.report.Message.Type} of
	 *        {@link Message} you want (set to <tt>null</tt> if you want all)
	 */
	public static Collection<Message> getMessagesFromSubtree(Section<?> section, Message.Type... types) {

		Collection<Message> messages = new ArrayList<Message>();
		getMessagesFromSubtree(messages, null, section, types);
		return Collections.unmodifiableCollection(messages);
	}

	// /**
	// * Returns the an unmodifiable Map containing all {@link Message}s for the
	// * given Section and article. The Collections are mapped by the String
	// * <tt>source.getName()</tt> they were stored for.
	// *
	// * @param article is the article the {@link Message}s are stored for
	// * @param section is the Section you want the messages from
	// */
	// public static Map<String, Collection<Message>>
	// getMessagesMap(Compiler compiler,
	// Section<?> section) {
	// if (section.getSectionStore().isEmpty()) return Collections.emptyMap();
	// return Collections.unmodifiableMap(getMessagesMapModifiable(compiler,
	// section));
	// }

	/**
	 * This method is private to avoid misuse (this map is modifiable). The map
	 * contains all {@link Message}s for the given Section and article. The
	 * Collections are mapped by the String <tt>source.getName()</tt>.
	 */
	@SuppressWarnings("unchecked")
	private static Map<String, Collection<Message>> getMessagesMapModifiable(Compiler compiler,
			Section<?> sec) {
		return (Map<String, Collection<Message>>) sec.getSectionStore().getObject(compiler,
				MESSAGE_KEY);
	}

	private static void addAllMessagesOfTypes(Collection<Message> source, Collection<Message> target, Message.Type... types) {
		if (types.length == 0) {
			target.addAll(source);
		}
		else {
			for (Message msg : source) {
				for (Message.Type type : types) {
					if (msg.getType() == type) {
						target.add(msg);
					}
				}
			}
		}
	}

	/**
	 * Filters the given Collection of {@link Message}s and returns a new
	 * Collection containing only {@link Message}s of the
	 * {@link de.knowwe.core.report.Message.Type} INFO.
	 * 
	 * @created 01.12.2011
	 */
	public static Collection<Message> getNotices(Collection<Message> messages) {
		Collection<Message> notices = new ArrayList<Message>(messages.size());
		for (Message msg : messages) {
			if (msg.getType() == Message.Type.INFO) notices.add(msg);
		}
		return notices;
	}

	/**
	 * Filters the given Collection of {@link Message}s and returns a new
	 * Collection containing only {@link Message}s of the
	 * {@link de.knowwe.core.report.Message.Type} WARNING.
	 * 
	 * @created 01.12.2011
	 */
	public static Collection<Message> getWarnings(Collection<Message> messages) {
		Collection<Message> warnings = new ArrayList<Message>(messages.size());
		for (Message msg : messages) {
			if (msg.getType() == Message.Type.WARNING) warnings.add(msg);
		}
		return warnings;
	}

	public static Message invalidNumberError(String message) {
		return Messages.error("Invalid Number: " + message);
	}

	public static Message invalidNumberWarning(String message) {
		return Messages.warning("Invalid Number: " + message);
	}

	public static Message noSuchObjectError(String name) {
		return Messages.noSuchObjectError("Object", name);
	}

	public static Message noSuchObjectError(String type, String name) {
		if (name != null && name.isEmpty()) {
			name = "empty String";
		}
		name = Strings.trimQuotes(name);
		return Messages.error(type + " '" + name + "' not found");
	}

	/**
	 * Creates and returns a {@link Message} of the
	 * {@link de.knowwe.core.report.Message.Type} INFO with the given text.
	 * 
	 * @created 01.12.2011
	 * @param message is the text content of the created {@link Message}
	 */
	public static Message notice(String message) {
		return new Message(Message.Type.INFO, message);
	}

	public static Message objectAlreadyDefinedError(String text) {
		return Messages.objectAlreadyDefinedError(text, null);
	}

	public static Message objectAlreadyDefinedError(String text, Section<?> definition) {
		String result = "Object already defined: " + text;
		if (definition != null) {
			result += " in: " + definition.getTitle();
		}
		return Messages.error(result);
	}

	public static Message objectAlreadyDefinedWarning(String text) {
		return Messages.warning("Object already defined: " + text);
	}

	public static Message objectCreatedNotice(String text) {
		return Messages.notice("Object created: " + text);
	}

	public static Message objectCreationError(String text) {
		return Messages.error("Could not create Object: " + text);
	}

	public static Message ambiguousTermClassesError(String origTerm, Collection<Class<?>> termClasses) {
		TreeSet<String> termClassesString = new TreeSet<String>();
		for (Class<?> termClass : termClasses) {
			termClassesString.add(termClass.getSimpleName());
		}
		origTerm = Strings.trimQuotes(origTerm);
		return Messages.error("The term '" + origTerm
				+ "' is defined with ambiguous term classes: "
				+ termClassesString.toString());
	}

	public static Message ambiguousTermCaseWarning(Collection<?> termObjects) {
		TreeSet<String> sortedIdentifiers = new TreeSet<String>();
		for (Object object : termObjects) {
			sortedIdentifiers.add(object.toString());
		}
		return Messages.warning("There are different cases for the same term: "
				+ sortedIdentifiers.toString());
	}

	public static Message relationCreatedNotice(String name) {
		return Messages.notice("Created realation: " + name);
	}

	/**
	 * Stores a single Message for the given Section and source.
	 * <p/>
	 * <b>ATTENTION: For this method applies the same as for the method
	 * KnowWEUtils#storeMessages(Section, Class, Class, Collection) . It can
	 * only be used once for the given set of parameters. If you use this method
	 * a second time with the same parameters, the first Message gets
	 * overwritten!</b>
	 * 
	 * @param compiler the {@link Compiler} the {@link Message}s are stored for
	 * @param section is the {@link Section} the {@link Message}s are stored for
	 * @param source is the Class the message originate from
	 * @param msg is the message you want so store
	 */
	public static void storeMessage(Compiler compiler, Section<?> section,
			Class<?> source, Message msg) {
		if (msg != null) {
			storeMessages(compiler, section, source, Messages.asList(msg));
		}
	}

	/**
	 * Stores a single Message for the given Section and source independent from
	 * any compiler.
	 * <p/>
	 * <b>ATTENTION: For this method applies the same as for the method
	 * KnowWEUtils#storeMessages(Section, Class, Class, Collection) . It can
	 * only be used once for the given set of parameters. If you use this method
	 * a second time with the same parameters, the first Message gets
	 * overwritten!</b>
	 * 
	 * @param section is the section you want to store the message for
	 * @param source is the Class the message originate from
	 * @param msg is the message you want so store
	 */
	public static void storeMessage(Section<?> section,
			Class<?> source, Message msg) {
		if (msg != null) {
			storeMessages(null, section, source, Messages.asList(msg));
		}
	}

	/**
	 * Stores the given Collection of {@link Message}s <tt>m</tt> from the Class
	 * <tt>source</tt> for the Article <tt>article</tt> and the Section
	 * <tt>s</tt>.
	 * <p/>
	 * <b>ATTENTION: This method can only be used once for each article,
	 * section, and source. If you use this Method a second time with the same
	 * parameters, the first Collection gets overwritten!</b>
	 * 
	 * @param compiler the {@link Compiler} the {@link Message}s are stored for
	 * @param section is the is the {@link Section} the {@link Message}s are
	 *        stored for
	 * @param source is the Class the messages originate from
	 * @param msgs is the Collection of messages you want so store
	 */
	public static void storeMessages(Compiler compiler, Section<?> section,
			Class<?> source, Collection<Message> msgs) {
		if (msgs != null) {
			Map<String, Collection<Message>> msgsMap = getMessagesMapModifiable(compiler, section);
			if (msgsMap == null) {
				msgsMap = new HashMap<String, Collection<Message>>(4);
				KnowWEUtils.storeObject(compiler, section, MESSAGE_KEY, msgsMap);
			}
			msgsMap.put(source.getName(), Collections.unmodifiableCollection(msgs));
		}
	}

	/**
	 * Stores the given Collection of {@link Message}s for the given Class in
	 * the given Section independently from any {@link Compiler}s.
	 * <p/>
	 * <b>ATTENTION: This method can only be used once for each article,††
	 * section, and source. If you use this Method a second time with the same
	 * parameters, the first Collection gets overwritten!</b>
	 * 
	 * @param section is the section you want to store the messages for
	 * @param source is the Class the messages originate from
	 * @param msgs is the Collection of messages you want so store
	 */
	public static void storeMessages(Section<?> section,
			Class<?> source, Collection<Message> msgs) {
		storeMessages(null, section, source, msgs);
	}

	/**
	 * Returns a {@link Message} to be used when a SubtreeHandler recognizes an
	 * syntactical error within its markup.
	 * 
	 * @created 18.08.2010
	 * @param message the {@link Message} of the syntax error
	 */
	public static Message syntaxError(String message) {
		return Messages.error("Syntax Error: " + message);
	}

	/**
	 * Returns a {@link Message} to be used when a SubtreeHandler recognizes an
	 * unexpected internal error based on an exception. The error will also be
	 * logged as a warning.
	 * 
	 * @created 18.08.2010
	 * @param message the message of the error
	 * @param e the exception occurred
	 */
	public static Message internalError(String message, Throwable e) {
		Log.warning(message, e);
		return Messages.error(message + ": " + e);
	}

	/**
	 * Creates and returns a {@link Message} of the
	 * {@link de.knowwe.core.report.Message.Type} WARNING with the given text.
	 * 
	 * @created 01.12.2011
	 * @param message is the text content of the created {@link Message}
	 */
	public static Message warning(String message) {
		return new Message(Message.Type.WARNING, message);
	}

	private Messages() {
	}

	/**
	 * Creates an empty collection of messages.
	 * 
	 * @created 28.02.2012
	 * @return collection of no message at all
	 */
	public static Collection<Message> noMessage() {
		return Collections.emptyList();
	}

	public static ResourceBundle getMessageBundle(Locale locale) {
		return ResourceBundle.getBundle("KnowWE_messages", locale);
	}

	public static ResourceBundle getMessageBundle() {
		return getMessageBundle(Locale.getDefault());
	}

	public static ResourceBundle getMessageBundle(UserContext user) {
		return getMessageBundle(Environment.getInstance().getWikiConnector().getLocale(
				user.getRequest()));
	}
}
