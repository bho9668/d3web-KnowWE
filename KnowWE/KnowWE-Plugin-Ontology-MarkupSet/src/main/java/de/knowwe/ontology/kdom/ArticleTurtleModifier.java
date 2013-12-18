/*
 * Copyright (C) 2013 denkbares GmbH
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

package de.knowwe.ontology.kdom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.node.DatatypeLiteral;
import org.ontoware.rdf2go.model.node.LanguageTagLiteral;
import org.ontoware.rdf2go.model.node.Literal;
import org.ontoware.rdf2go.model.node.Node;
import org.ontoware.rdf2go.model.node.PlainLiteral;
import org.ontoware.rdf2go.model.node.Resource;
import org.ontoware.rdf2go.model.node.URI;

import de.d3web.strings.Strings;
import de.d3web.utils.EqualsUtils;
import de.d3web.utils.Pair;
import de.knowwe.core.Environment;
import de.knowwe.core.compile.packaging.PackageManager;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.RootType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.basicType.PlainText;
import de.knowwe.core.kdom.objects.Term;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.ontology.turtle.BlankNode;
import de.knowwe.ontology.turtle.Object;
import de.knowwe.ontology.turtle.ObjectList;
import de.knowwe.ontology.turtle.Predicate;
import de.knowwe.ontology.turtle.PredicateObjectSentenceList;
import de.knowwe.ontology.turtle.PredicateSentence;
import de.knowwe.ontology.turtle.Subject;
import de.knowwe.ontology.turtle.TurtleContent;
import de.knowwe.ontology.turtle.TurtleMarkup;
import de.knowwe.ontology.turtle.TurtleSentence;
import de.knowwe.rdf2go.Rdf2GoCore;

/**
 * This class allows to add and remove statements to/from the turtle markup of a
 * specific wiki article. The class is capable to collect a set of desired
 * changes and apply it to the wiki text of the article in a seamless manner. As
 * a result the configured object is capable to return the newly created wiki
 * article text and the statements changes that could not been applied to the
 * wiki page.
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 17.12.2013
 */
public class ArticleTurtleModifier {

	private class AddedStatement {

		private final Section<TurtleSentence> parentSentence;
		private final Section<PredicateSentence> parentPredicateSentence;
		private final Statement statement;

		public AddedStatement(Statement statement) {
			this.statement = statement;

			Pair<Section<TurtleSentence>, Section<PredicateSentence>> position =
					getInsertPosition(article, statement);
			this.parentSentence = position.getA();
			this.parentPredicateSentence = position.getB();
		}

		public Section<TurtleSentence> getSentence() {
			return parentSentence;
		}

		public Section<PredicateSentence> getPredicateSentence() {
			return parentPredicateSentence;
		}

		public Statement getStatement() {
			return statement;
		}
	}

	/*
	 * Some basic attributes for this writer to be initialized on startup
	 */
	private final Article article;
	private final boolean compactMode;
	private final String preferredIndent;

	/*
	 * Statements to be modified
	 */
	private final List<Statement> insertStatements = new LinkedList<Statement>();
	private final List<Statement> deleteStatements = new LinkedList<Statement>();

	/*
	 * Some fields required for traversing the tree to create the resulting wiki
	 * text
	 */
	private final transient List<AddedStatement> objectsToAdd = new LinkedList<AddedStatement>();
	private final transient Set<Section<Object>> objectsToRemove = new HashSet<Section<Object>>();
	private final transient List<Statement> ignoredStatements = new LinkedList<Statement>();
	private transient StringBuilder buffer = null;
	private final Rdf2GoCore core;

	/**
	 * Creates a new {@link TurtleWriter} to modify the turtle statements of the
	 * specified wiki article.
	 * 
	 * @param article the wiki article to be modified
	 */
	public ArticleTurtleModifier(Article article) {
		this(article, true);
	}

	/**
	 * Creates a new {@link TurtleWriter} to modify the turtle statements of the
	 * specified wiki article.
	 * 
	 * @param article the wiki article to be modified
	 * @param compactMode if the turtle markup should be created compact (prefer
	 *        single-line-mode) or verbose (prefer readability with line-breaks
	 *        for each property and value, using indenting).
	 */
	public ArticleTurtleModifier(Article article, boolean compactMode) {
		this(article, compactMode, "  ");
	}

	/**
	 * Creates a new {@link TurtleWriter} to modify the turtle statements of the
	 * specified wiki article. You can specify if the output shall be compact or
	 * expressive/verbose and how to indent the particular turtle sentences if
	 * newly created.
	 * 
	 * @param article the wiki article to be modified
	 * @param compactMode if the turtle markup should be created compact (prefer
	 *        single-line-mode) or verbose (prefer readability with line-breaks
	 *        for each property and value, using indenting).
	 * @param preferredIndent the preferred indent to be used, should consist of
	 *        spaces and tab characters only
	 */
	public ArticleTurtleModifier(Article article, boolean compactMode, String preferredIndent) {
		this(findCore(article), article, compactMode, preferredIndent);
	}

	/**
	 * Returns an instance that compiles any section in this article (taking the
	 * terms as a basis)
	 */
	private static Rdf2GoCore findCore(Article article) {
		String web = article.getWeb();
		PackageManager packageManager = Environment.getInstance().getPackageManager(web);
		for (Section<?> section : Sections.successors(article.getRootSection(), Term.class)) {
			Set<String> masters = packageManager.getCompilingArticles(section);
			if (masters.isEmpty()) continue;
			String master = masters.iterator().next();
			return Rdf2GoCore.getInstance(web, master);
		}
		return Rdf2GoCore.getInstance();
	}

	/**
	 * Creates a new {@link TurtleWriter} to modify the turtle statements of the
	 * specified wiki article. You can specify if the output shall be compact or
	 * expressive/verbose and how to indent the particular turtle sentences if
	 * newly created.
	 * 
	 * @param core the core to be used to compile the modified turtle contents
	 * @param article the wiki article to be modified
	 * @param compactMode if the turtle markup should be created compact (prefer
	 *        single-line-mode) or verbose (prefer readability with line-breaks
	 *        for each property and value, using indenting).
	 * @param preferredIndent the preferred indent to be used, should consist of
	 *        spaces and tab characters only
	 */
	public ArticleTurtleModifier(Rdf2GoCore core, Article article, boolean compactMode, String preferredIndent) {
		this.core = core;
		this.article = article;
		this.compactMode = compactMode;
		this.preferredIndent = preferredIndent;
	}

	/**
	 * Adds a number of statements that shall be inserted into the turtle
	 * markups the the wiki article of this turtle writer.
	 * 
	 * @created 09.12.2013
	 * @param statements the statements to be added to the article
	 */
	public void addInsert(Statement... statements) {
		if (statements != null && statements.length > 0) {
			Collections.addAll(insertStatements, statements);
			invalidate();
		}
	}

	/**
	 * Adds a number of statements that shall be inserted into the turtle
	 * markups the the wiki article of this turtle writer.
	 * 
	 * @created 09.12.2013
	 * @param statements the statements to be added to the article
	 */
	public void addInsert(List<Statement> statements) {
		if (statements != null && !statements.isEmpty()) {
			insertStatements.addAll(statements);
			invalidate();
		}
	}

	/**
	 * Defines a number of statements that shall be deleted from the turtle
	 * markups the the wiki article of this turtle writer.
	 * 
	 * @created 09.12.2013
	 * @param statements the statements to be deleted from the article
	 */
	public void addDelete(Statement... statements) {
		if (statements != null && statements.length > 0) {
			Collections.addAll(deleteStatements, statements);
			invalidate();
		}
	}

	/**
	 * Defines a number of statements that shall be deleted from the turtle
	 * markups the the wiki article of this turtle writer.
	 * 
	 * @created 09.12.2013
	 * @param statements the statements to be deleted from the article
	 */
	public void addDelete(List<Statement> statements) {
		if (statements != null && !statements.isEmpty()) {
			deleteStatements.addAll(statements);
			invalidate();
		}
	}

	/**
	 * Returns the indent of the line within this turtle writer's article text,
	 * pointed to by the specified index.
	 * 
	 * @created 26.11.2013
	 * @param index the character position to identify the line to get the
	 *        indent for
	 * @return the indent of the line specified by the index
	 */
	private String getIndent(int index) {
		if (article == null) return preferredIndent;
		String text = article.getText();
		// search start of line
		while (index > 0 && "\r\n".indexOf(text.charAt(index - 1)) == -1)
			index--;
		// proceed while having white-spaces
		int indent = 0;
		while (index + indent < text.length() && Strings.isBlank(text.charAt(index + indent)))
			indent++;
		return text.substring(index, index + indent);
	}

	/**
	 * Create turtle markup for a set of statements that share the same subject
	 * and predicate. The markup will be generated as a list of objects to be
	 * injected into an existing subject+predicate sentence.
	 * <p>
	 * Depending on 'compactMode' the objects will be separated by a line-break
	 * or not.
	 * 
	 * @created 26.11.2013
	 * @param statements the statements to be inserted, sharing the same subject
	 *        and predicate
	 * @param indent the indent to be used if not in compact mode
	 * @param turtle the buffer to append the created turtle text
	 */
	private void createObjectTurtle(List<Statement> statements, String indent, StringBuilder turtle) {
		boolean first = true;
		for (Statement statement : statements) {
			// add object separator
			if (first) first = false;
			else if (compactMode) turtle.append(", ");
			else turtle.append(",\n").append(indent);
			// add object itself
			turtle.append(toTurtle(statement.getObject()));
		}
	}

	/**
	 * Create turtle markup for a set of statements that share the same subject
	 * and predicate. The markup will be generated as a predicate and a list of
	 * objects to be injected into an existing subject sentence.
	 * <p>
	 * Depending on 'compactMode' the objects of the statement will be separated
	 * by a line-break or not.
	 * 
	 * @created 26.11.2013
	 * @param statements the statements to be inserted, sharing the same subject
	 *        and predicate
	 * @param indent the indent to be used if not in compact mode
	 * @param turtle the buffer to append the created turtle text
	 */
	private void createPredicateTurtle(List<Statement> statements, String indent, StringBuilder turtle) {
		String objectIndent = indent + "  ";

		// append predicate and spacing to objects
		URI predicate = statements.get(0).getPredicate();
		turtle.append(toTurtle(predicate));
		if (compactMode) turtle.append(" ");
		else turtle.append("\n").append(objectIndent);

		// append the list of objects
		createObjectTurtle(statements, objectIndent, turtle);
	}

	// private static String createSubjectTurtle(List<Statement> statements,
	// String indent) {
	// StringBuilder turtle = new StringBuilder();
	// createSubjectTurtle(statements, indent, turtle);
	// return turtle.toString();
	// }

	/**
	 * Create turtle markup for a set of statements that share the same subject.
	 * The markup will be generated as a list of predicates and a list of
	 * objects for each predicate. The result is intended to be injected into an
	 * existing turtle markup.
	 * <p>
	 * Depending on 'compactMode' the predicates and objects of the statement
	 * will be separated by a line-break or not.
	 * 
	 * @created 26.11.2013
	 * @param statements the statements to be inserted, sharing the same subject
	 * @param indent the indent to be used for turtle sentences
	 * @param turtle the buffer to append the created turtle text
	 */
	private void createSubjectTurtle(List<Statement> statements, String indent, StringBuilder turtle) {
		String predicateIndent = indent + "  ";

		// append subject and separator to first predicate
		Resource subject = statements.get(0).getSubject();
		turtle.append(toTurtle(subject));
		if (compactMode) turtle.append(" ");
		else turtle.append("\n").append(predicateIndent);

		boolean first = true;
		for (List<Statement> group : groupByPredicate(statements)) {
			// add object separator
			if (first) first = false;
			else if (compactMode) turtle.append(";\n").append(predicateIndent);
			else turtle.append(";\n\n").append(predicateIndent);
			// render each predicate
			createPredicateTurtle(group, predicateIndent, turtle);
		}
	}

	/**
	 * Create turtle markup for a set of statements. The statements will be
	 * grouped by subjects and predicates to create minimal verbose turtle
	 * markup. The markup will be generated as a list of turtle sentences. Each
	 * sentence has a list of predicates and a list of objects for each
	 * predicate. The result is intended to be injected into an existing turtle
	 * markup.
	 * <p>
	 * Depending on 'compactMode' the predicates and objects of the statement
	 * will be separated by a line-break or not. Each turtle sentence is created
	 * in a new line, but 'compactMode' also influences the spacing between the
	 * individual lines.
	 * 
	 * @created 26.11.2013
	 * @param statements the statements to be inserted
	 * @param indent the indent to be used for the turtle sentences
	 * @param turtle the buffer to append the created turtle text
	 */
	private void createTurtle(Section<?> target, List<Statement> statements, String indent, StringBuilder turtle) {
		boolean first = true;
		for (List<Statement> group : groupBySubject(statements)) {
			if (first) first = false;
			else if (compactMode) turtle.append(".\n").append(indent);
			else turtle.append(".\n\n").append(indent);
			createSubjectTurtle(group, indent, turtle);
		}
	}

	/**
	 * Group a set of statements by their predicates.
	 * 
	 * @created 26.11.2013
	 * @param statements the statements to be grouped
	 * @return a collection of non-empty statement lists sharing the same
	 *         predicate
	 */
	private Collection<List<Statement>> groupByPredicate(Collection<Statement> statements) {
		Map<Resource, List<Statement>> result = mapByPredicate(statements);
		return result.values();
	}

	private Map<Resource, List<Statement>> mapByPredicate(Collection<Statement> statements) {
		Map<Resource, List<Statement>> result = new LinkedHashMap<Resource, List<Statement>>();
		for (Statement statement : statements) {
			URI predicate = statement.getPredicate();
			List<Statement> list = result.get(predicate);
			if (list == null) {
				list = new LinkedList<Statement>();
				result.put(predicate, list);
			}
			list.add(statement);
		}
		return result;
	}

	/**
	 * Group a collection of statements by their subject.
	 * 
	 * @created 26.11.2013
	 * @param statements the statements to be grouped
	 * @return a collection of non-empty statement lists sharing the same
	 *         subject
	 */
	private Collection<List<Statement>> groupBySubject(Collection<Statement> statements) {
		Map<Resource, List<Statement>> result = mapBySubject(statements);
		return result.values();
	}

	private Map<Resource, List<Statement>> mapBySubject(Collection<Statement> statements) {
		Map<Resource, List<Statement>> result = new LinkedHashMap<Resource, List<Statement>>();
		for (Statement statement : statements) {
			Resource subject = statement.getSubject();
			List<Statement> list = result.get(subject);
			if (list == null) {
				list = new LinkedList<Statement>();
				result.put(subject, list);
			}
			list.add(statement);
		}
		return result;
	}

	/**
	 * Creates turtle markup for the specified node.
	 * 
	 * @created 26.11.2013
	 * @param section the section to create the turtle markup for
	 * @param node the node to create turtle markup for
	 * @return the turtle markup to represent the node's value
	 */
	private String toTurtle(Node node) {
		if (node instanceof URI) {
			String text = core.toShortURI(node.asURI()).toString();
			if (text.startsWith("lns:")) text = text.substring(3);
			return text;
		}
		if (node instanceof DatatypeLiteral) {
			DatatypeLiteral literal = node.asDatatypeLiteral();
			URI datatype = core.toShortURI(literal.getDatatype());
			return Strings.quote(literal.getValue()) + "^^" + datatype;
		}
		if (node instanceof LanguageTagLiteral) {
			LanguageTagLiteral literal = node.asLanguageTagLiteral();
			return Strings.quote(literal.getValue()) + "@" + literal.getLanguageTag();
		}
		if (node instanceof PlainLiteral) {
			return Strings.quote(node.asLiteral().getValue());
		}
		if (node instanceof Literal) {
			return node.asLiteral().toSPARQL();
		}
		throw new IllegalArgumentException(
				"non-implemented conversion method for " + node.getClass());
	}

	/**
	 * Processes the article of this writer and the statements to be added and
	 * deleted to create a new wiki page content.
	 * 
	 * @created 09.12.2013
	 * @return the page content after inserting and deleting the specified
	 *         statements
	 */
	public String getResultText() {
		process();
		return buffer.toString();
	}

	/**
	 * Returns a list of statements that have not been considered when creating
	 * the result wiki text using {@link #getResultText()}. These statements are
	 * always a (hopefully empty) subset of the Statements to be removed,
	 * because no statements to be added will be ignored. If a statement is
	 * ignored, the reason is that the particular statement is not found on this
	 * turtle writer's article.
	 * <p>
	 * To check if all statements are implemented into the resulting wiki page
	 * the returned list must be empty!
	 * 
	 * @created 06.12.2013
	 * @return the list the non-removed statements that should have been removed
	 */
	public List<Statement> getIgnoredStatements() {
		process();
		return Collections.unmodifiableList(ignoredStatements);
	}

	private void invalidate() {
		buffer = null;
	}

	private void process() {
		if (buffer != null) return;

		// clean transient fields, prepare for tree traversal
		buffer = new StringBuilder();
		objectsToAdd.clear();
		objectsToRemove.clear();
		ignoredStatements.clear();

		// prepare tree traversal information
		for (Statement statement : insertStatements) {
			objectsToAdd.add(new AddedStatement(statement));
		}
		for (Statement statement : deleteStatements) {
			Section<Object> sectionToDelete = getDeleteSection(article, statement);
			if (sectionToDelete == null) {
				ignoredStatements.add(statement);
			}
			else {
				objectsToRemove.add(sectionToDelete);
			}
		}

		// and process the tree to create the new wiki content
		traverse(article.getRootSection(), buffer);

		// and new turtle content if there are still non-added statements
		List<Statement> rest = extractStatements(null, null);
		if (!rest.isEmpty()) {
			// remove white-spaces at end of buffer
			while (buffer.length() > 0 && Strings.isBlank(buffer.charAt(buffer.length() - 1))) {
				buffer.setLength(buffer.length() - 1);
			}
			// and append new turtle markup
			buffer.append("\n\n%%TurtlePimped\n");
			createTurtle(article.getRootSection(), rest, preferredIndent, buffer);
			buffer.append(".\n%\n\n");
		}
	}

	private List<Statement> extractStatements(Section<TurtleSentence> sentence, Section<PredicateSentence> predicateSentence) {
		List<Statement> result = new LinkedList<Statement>();
		Iterator<AddedStatement> iterator = objectsToAdd.iterator();
		while (iterator.hasNext()) {
			AddedStatement toAdd = iterator.next();
			if (!EqualsUtils.equals(sentence, toAdd.getSentence())) continue;
			if (!EqualsUtils.equals(predicateSentence, toAdd.getPredicateSentence())) continue;
			iterator.remove();
			result.add(toAdd.getStatement());
		}
		return result;
	}

	/**
	 * Traverses the tree, building the result text. The method returns if it
	 * contains at least one object on interest that is not removed: object,
	 * predicate, subject; depending on the tree traversal level
	 * 
	 * @created 07.12.2013
	 */
	private boolean traverse(Section<?> node, StringBuilder result) {
		// check if we are reached an object to delete
		// the skip this node and tell that the node is not if interest
		if (objectsToRemove.contains(node)) return false;

		// check for some special nodes we only include
		// if they contain at least one node of interest
		Type type = node.get();

		// for now, we do not traverse into blank nodes,
		// thus leave them untouched
		if (type instanceof BlankNode) {
			result.append(node.getText());
			return true;
		}

		// check if we have any parent of
		// TurtleSentence, PredicateSentence or Object
		if (type instanceof TurtleContent
				|| type instanceof PredicateObjectSentenceList
				|| type instanceof ObjectList) {
			return traverseChildrenSkipping(node, result);
		}

		// if a turtle becomes empty, remove it completely
		if (type instanceof TurtleMarkup) {
			StringBuilder part = new StringBuilder();
			boolean interest = traverseChildren(node, part);
			if (interest) {
				result.append(part);
			}
			return interest;
		}

		// for nodes of non-interest (all non-observed turtle nodes)
		if (node.getChildren().isEmpty()) {
			// for leaves append them
			result.append(node.getText());
			return false;
		}
		// for all other nodes simply traverse their children
		// all sentence objects are of interest
		// and all parent having children of interest
		return traverseChildren(node, result);
	}

	/**
	 * Traverses and appends all children recursively.
	 * 
	 * @created 07.12.2013
	 */
	private boolean traverseChildren(Section<?> node, StringBuilder result) {
		boolean interest = (node.get() instanceof Object);
		for (Section<?> child : node.getChildren()) {
			interest |= traverse(child, result);
		}
		return interest;
	}

	/**
	 * Traverses and appends all children recursively. Skips the children that
	 * tells that they are not of interest. Also normalizes the separators in
	 * between the children if any are removed, by skipping the unnecessary
	 * ones.
	 * 
	 * @created 07.12.2013
	 */
	private boolean traverseChildrenSkipping(Section<?> node, StringBuilder result) {
		List<Section<?>> children = node.getChildren();
		List<Pair<Type, String>> childResults =
				new ArrayList<Pair<Type, String>>(children.size());
		for (Section<?> child : children) {
			StringBuilder part = new StringBuilder();
			if (traverse(child, part) || child.get() instanceof PlainText) {
				childResults.add(new Pair<Type, String>(child.get(), part.toString()));
			}
			else {
				childResults.add(null);
			}
		}

		// remove unrequired separators and childs of no interest
		cleanChildResults(childResults);

		// append final result to result-buffer
		for (Pair<Type, String> pair : childResults) {
			result.append(pair.getB());
		}

		// insert new statements if there are any for that node
		boolean added = appendAddedStatements(node, childResults.isEmpty(), result);

		// the node shall not be ignored
		// if at least one child has been added
		return childResults.size() > 0 | added;
	}

	/**
	 * Appends the statements to add to the specified node in the turtle kdom.
	 * The Node is one of the list-sections containing either TurtleSentence,
	 * PredicateSentence or Object as their children.
	 * 
	 * @created 09.12.2013
	 * @return returns if any statements were appended to the specified node
	 */
	private boolean appendAddedStatements(Section<?> node, boolean isFirst, StringBuilder result) {
		Section<PredicateSentence> predSentence = Sections.ancestor(node,
				PredicateSentence.class);
		Section<TurtleSentence> sentence = Sections.ancestor(node, TurtleSentence.class);

		List<Statement> toInsert = extractStatements(sentence, predSentence);
		if (toInsert.isEmpty()) return false;

		if (sentence == null) {
			String indent = preferredIndent;
			if (!isFirst) {
				// check if last "." is missing and insert if required
				List<Section<? extends Type>> siblings = node.getChildren();
				if (!siblings.get(siblings.size() - 1).getText().trim().equals(".")) {
					result.append(".");
				}
			}
			result.append(compactMode ? "\n" : "\n\n").append(indent);
			createTurtle(node, toInsert, indent, result);
			result.append(".");
		}
		else if (predSentence == null) {
			String indent = getIndent(sentence.getOffsetInArticle()) + "  ";
			if (!isFirst) result.append(";");
			result.append(compactMode ? "\n" : "\n\n").append(indent);
			createPredicateTurtle(toInsert, indent, result);
		}
		else {
			String indent = getIndent(predSentence.getOffsetInArticle()) + "  ";
			if (!isFirst) result.append(", ");
			result.append(compactMode ? "" : "\n" + indent);
			createObjectTurtle(toInsert, indent, result);
		}
		return true;
	}

	/**
	 * Cleans up a list of section and plain text separators. The sections to be
	 * removed are added as "null" to the list.
	 * 
	 * @created 07.12.2013
	 * @param childResults the list to be cleaned up
	 */
	private void cleanChildResults(List<Pair<Type, String>> list) {
		int index;
		while ((index = list.indexOf(null)) >= 0) {
			if (index == 0) {
				// for the first element remove the element
				// and the succeeding separator (if there is any)
				if (list.size() > 1 && list.get(1).getA() instanceof PlainText) {
					list.remove(1);
				}
				list.remove(0);
			}
			else {
				// otherwise remove the item and the separator before
				list.remove(index);
				if (list.get(index - 1).getA() instanceof PlainText) {
					list.remove(index - 1);
				}
			}
		}
	}

	private boolean hasSameSubject(Section<TurtleSentence> sentence, Statement statement) {
		if (sentence == null) return false;
		Section<Subject> subject = Sections.successor(sentence, Subject.class);
		if (subject == null) return false;
		Resource resource = subject.get().getResource(subject, core);
		return resource.equals(statement.getSubject());
	}

	private boolean hasSamePredicate(Section<PredicateSentence> predicateSentence, Statement statement) {
		if (predicateSentence == null) return false;
		Section<Predicate> predicate = Sections.successor(predicateSentence, Predicate.class);
		if (predicate == null) return false;
		URI uri = predicate.get().getURI(predicate, core);
		return uri.equals(statement.getPredicate());
	}

	private boolean hasSameObject(Section<Object> object, Statement statement) {
		if (object == null) return false;
		Node node = object.get().getNode(object, core);
		return node.equals(statement.getObject());
	}

	private Pair<Section<TurtleSentence>, Section<PredicateSentence>> getInsertPosition(Article article, Statement statement) {
		Section<RootType> root = article.getRootSection();
		for (Section<PredicateSentence> ps : Sections.successors(root, PredicateSentence.class)) {
			if (!hasSamePredicate(ps, statement)) continue;
			Section<TurtleSentence> ts = Sections.ancestor(ps, TurtleSentence.class);
			if (!hasSameSubject(ts, statement)) continue;
			return new Pair<Section<TurtleSentence>, Section<PredicateSentence>>(ts, ps);
		}
		for (Section<TurtleSentence> ts : Sections.successors(root, TurtleSentence.class)) {
			if (!hasSameSubject(ts, statement)) continue;
			return new Pair<Section<TurtleSentence>, Section<PredicateSentence>>(ts, null);
		}
		return new Pair<Section<TurtleSentence>, Section<PredicateSentence>>(null, null);
	}

	private Section<Object> getDeleteSection(Article article, Statement statement) {
		Section<RootType> root = article.getRootSection();
		for (Section<Object> object : Sections.successors(root, Object.class)) {
			if (!hasSameObject(object, statement)) continue;
			Section<PredicateSentence> ps = Sections.ancestor(object, PredicateSentence.class);
			if (!hasSamePredicate(ps, statement)) continue;
			Section<TurtleSentence> ts = Sections.ancestor(ps, TurtleSentence.class);
			if (!hasSameSubject(ts, statement)) continue;
			return object;
		}
		return null;
	}

	/**
	 * Returns the article this turtle modifier is working on.
	 * 
	 * @created 17.12.2013
	 * @return the article of this modifier
	 */
	public Article getArticle() {
		return this.article;
	}

}
