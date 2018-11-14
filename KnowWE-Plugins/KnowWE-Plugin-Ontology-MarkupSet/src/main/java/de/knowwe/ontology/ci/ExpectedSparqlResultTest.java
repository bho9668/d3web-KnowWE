/*
 * Copyright (C) 2014 denkbares GmbH
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

package de.knowwe.ontology.ci;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.denkbares.collections.MultiMap;
import com.denkbares.semanticcore.CachedTupleQueryResult;
import com.denkbares.strings.Strings;
import de.d3web.testing.AbstractTest;
import de.d3web.testing.Message;
import de.d3web.testing.MessageObject;
import de.d3web.testing.TestParameter.Mode;
import de.d3web.testing.TestParser;
import de.d3web.testing.TestResult;
import de.d3web.we.ci4ke.test.ResultRenderer;
import de.knowwe.core.compile.Compilers;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.ontology.ci.provider.SparqlExpectedResultSection;
import de.knowwe.ontology.ci.provider.SparqlTestObjectProviderUtils;
import de.knowwe.ontology.sparql.SparqlContentType;
import de.knowwe.rdf2go.Rdf2GoCompiler;
import de.knowwe.rdf2go.Rdf2GoCore;
import de.knowwe.rdf2go.utils.Rdf2GoUtils;
import de.knowwe.rdf2go.utils.ResultTableModel;

/**
 * @author Jochen Reutelshöfer
 * @created 10.01.2014
 */
public class ExpectedSparqlResultTest extends AbstractTest<SparqlExpectedResultSection> implements ResultRenderer {

	public static final String AT_LEAST = "atLeast";
	public static final String EQUAL = "equal";

	public static final String WARNING = "warning";

	public ExpectedSparqlResultTest() {
		String[] comparators = new String[] {
				EQUAL, AT_LEAST };

		this.addParameter("comparator", Mode.Optional,
				"how to compare the result data against the expected table data: equal/atleast",
				comparators);
		this.addParameter("warning", Mode.Optional, "show warning instead of failure if this test fails", WARNING);
	}

	@Override
	public Message execute(SparqlExpectedResultSection testObject, String[] args, String[]... ignores) throws InterruptedException {
		boolean atLeastFlag = false;

		Message.Type messageTypeTestFailed = Message.Type.FAILURE;
		if (args.length > 1 && WARNING.equalsIgnoreCase(args[1])) {
			messageTypeTestFailed = Message.Type.WARNING;
		}

		if (args.length > 0) {
			String arg0 = args[0];
			if (arg0.equalsIgnoreCase(AT_LEAST)) {
				atLeastFlag = true;
			}
		}

		Section<ExpectedSparqlResultTable> expectedResultTableSection = testObject.getSection();
		Section<DefaultMarkupType> expectedResultDefaultMarkup = Sections.ancestor(
				expectedResultTableSection,
				DefaultMarkupType.class);
		String actualSparqlName = DefaultMarkupType.getAnnotation(expectedResultDefaultMarkup,
				ExpectedSparqlResultTableMarkup.SPARQL_ANNOTATION);
		if (actualSparqlName == null) {
			return new Message(
					messageTypeTestFailed,
					"No sparql query specified for test object, use annotation 'sparql' to set a sparql query to test against.");
		}

		Collection<Section<SparqlContentType>> actualSparqlSections = SparqlTestObjectProviderUtils.getSparqlQueryContentSection(actualSparqlName);
		if (actualSparqlSections.size() > 1) {
			return new Message(Message.Type.ERROR,
					"Multiple sparql queries in the wiki with name: " + actualSparqlName);
		}

		if (actualSparqlSections.isEmpty()) {
			return new Message(Message.Type.ERROR,
					"No sparql query in the wiki found for name: " + actualSparqlName);
		}

		Section<SparqlContentType> actualSparqlSection = actualSparqlSections.iterator().next();
		Rdf2GoCompiler compiler = Compilers.getCompiler(actualSparqlSection, Rdf2GoCompiler.class);
		assert compiler != null;
		Rdf2GoCore core = compiler.getRdf2GoCore();

		if (core == null) {
			return new Message(Message.Type.ERROR,
					"No repository found for section: " + actualSparqlSection);
		}

		String expectedSparqlName = DefaultMarkupType.getAnnotation(expectedResultDefaultMarkup, ExpectedSparqlResultTableMarkup.NAME_ANNOTATION);
		String actualSparqlString = Rdf2GoUtils.createSparqlString(core, actualSparqlSection.getText());
		CachedTupleQueryResult result;
		try {
			result = core.sparqlSelect(actualSparqlString);
		}
		catch (Exception e) {
			Message message = new Message(messageTypeTestFailed, "Exception while executing SPARQL query: " + e.getMessage());
			ArrayList<MessageObject> messageObjects = new ArrayList<>();
			messageObjects.add(new MessageObject(actualSparqlName, Section.class));
			messageObjects.add(new MessageObject(expectedSparqlName, Section.class));
			message.setObjects(messageObjects);
			return message;
		}
		ResultTableModel actualResultTable = new ResultTableModel(result);

		List<String> variables = actualResultTable.getVariables();

		ResultTableModel expectedResultTable = ExpectedSparqlResultTable.getResultTableModel(
				expectedResultTableSection, variables, compiler);

		MultiMap<String, Message> failures = ResultTableModel.checkEquality(core, expectedResultTable,
				actualResultTable, atLeastFlag);

		if (!failures.isEmpty()) {
			String errorsText = ResultTableModel.generateErrorsText(failures, false);
			errorsText += "Expected result: " + expectedSparqlName + ", actual result: " + actualSparqlName;
			Message message = new Message(messageTypeTestFailed, errorsText);
			ArrayList<MessageObject> messageObjects = new ArrayList<>();
			messageObjects.add(new MessageObject(actualSparqlName, Section.class));
			messageObjects.add(new MessageObject(expectedSparqlName, Section.class));
			message.setObjects(messageObjects);
			return message;
		}

		return Message.SUCCESS;
	}

	@Override
	public Class<SparqlExpectedResultSection> getTestObjectClass() {
		return SparqlExpectedResultSection.class;
	}

	@Override
	public String getDescription() {
		return "Test a expected sparql result set against the result set of the actually executed sparql query.";
	}

	@Override
	public void renderResult(TestResult testResult, RenderResult renderResult) {

		// prepare some information
		Message summary = testResult.getSummary();
		String text = (summary == null) ? null : summary.getText();
		String[] config = testResult.getConfiguration();
		boolean hasConfig = config != null && !(config.length == 0);
		boolean hasText = !Strings.isBlank(text);

		String name = "";

		String title = this.getDescription().replace("'", "&#39;");

		if (hasConfig || hasText) {
			if (hasConfig) {
				Collection<Section<ExpectedSparqlResultTable>> testObj = SparqlTestObjectProviderUtils.getExpectedQueryResultSection(config[0]);

				if (!testObj.isEmpty() && Sections.ancestor(testObj.iterator()
						.next(), ExpectedSparqlResultTableMarkup.class) != null) {
					Section<ExpectedSparqlResultTableMarkup> markup = Sections.ancestor(testObj.iterator()
							.next(), ExpectedSparqlResultTableMarkup.class);
					assert(markup != null);
					name = "Check SPARQL: " + "<a href = '" + KnowWEUtils.getURLLink(markup) + "'>" + config[0] + "</a> (" + TestParser
							.concatParameters(1, config) + ")";
				}
				else {
					name = "(" + TestParser.concatParameters(config) + " )";
				}
			}
			/*
			if (hasText) {
				name = name + ": " + text;
			}
			*/
		}
		else {
			name = testResult.getTestName();
		}

		renderResult.appendHtml("<span class='ci-test-title' title='" + title + "'>");
		renderResult.appendHtml(name);
		renderResult.appendHtml("</span>");
	}
}
