package de.knowwe.testcases.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import de.d3web.core.session.Session;
import de.d3web.core.utilities.Triple;
import de.d3web.empiricaltesting.SequentialTestCase;
import de.d3web.empiricaltesting.TestPersistence;
import de.d3web.testcase.model.TestCase;
import de.knowwe.core.Environment;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.kdom.defaultMarkup.ContentType;
import de.knowwe.testcases.TestCasePlayerRenderer;
import de.knowwe.testcases.TestCasePlayerType;
import de.knowwe.testcases.TestCaseProvider;
import de.knowwe.testcases.TestCaseUtils;

public class CheckDownloadCaseAction extends AbstractAction {

	public static final String PARAM_FILENAME = "filename";

	@Override
	public void execute(UserActionContext context) throws IOException {

		Section<?> section = getPlayerSection(context);
		if (section == null) return; // error will already be added

		Triple<TestCaseProvider, Section<?>, Article> selectedTestCaseTriple = getSelectedTestCaseTriple(
				section, context);

		if (selectedTestCaseTriple == null) {
			sendJSON(context, "error", "No testcase found to download.");
			return;
		}

		Section<?> testCaseSection = selectedTestCaseTriple.getB();

		if (!userCanView(context, section) || !userCanView(context, testCaseSection)) {
			sendJSON(context, "error", "You are not allowed to download this TestCase");
			return;
		}

		TestCaseProvider provider = selectedTestCaseTriple.getA();
		TestCase testCase = provider.getTestCase();
		String testCaseName = provider.getName();
		Session session = provider.getActualSession(context);

		if (session == null) {
			sendJSON(context, "error", "Unable to download TestCase: No valid session found");
			return;
		}

		SequentialTestCase sequentialTestCase = null;
		try {
			sequentialTestCase = TestCaseUtils.transformToSTC(testCase, testCaseName,
					session.getKnowledgeBase());
		}
		catch (Exception e) {
			sendJSON(context,
					"error",
					"Exception while creating downloadable test case file: " + e.getMessage());
			return;
		}

		File caseFile = File.createTempFile("TestCase", null);
		caseFile.deleteOnExit();

		FileOutputStream out = new FileOutputStream(caseFile);

		TestPersistence.getInstance().writeCases(out, Arrays.asList(sequentialTestCase), false);

		out.flush();
		out.close();

		sendJSON(context, "path", caseFile.getAbsolutePath(), "file", toXMLFileName(testCaseName));
	}

	public static String toXMLFileName(String testCaseName) {
		if (!testCaseName.toLowerCase().endsWith(".xml")) testCaseName += ".xml";
		return testCaseName;
	}

	public static void sendJSON(UserActionContext context, String... keyAndValues) throws IOException {
		JSONObject response = new JSONObject();
		try {
			for (int i = 0; i + 2 <= keyAndValues.length; i += 2) {
				response.accumulate(keyAndValues[i], keyAndValues[i + 1]);
			}
			if (context.getWriter() != null) {
				context.setContentType("text/html; charset=UTF-8");
				response.write(context.getWriter());
			}
		}
		catch (JSONException e) {
			throw new IOException(e);
		}
	}

	public static Section<?> getPlayerSection(UserActionContext context) throws IOException {
		String playerid = context.getParameter("playerid");
		if (playerid == null) {
			playerid = context.getParameter("SectionID");
		}

		Section<?> section = Sections.getSection(playerid);
		if (section != null) {
			section = Sections.findChildOfType(section, ContentType.class);
		}
		if (section == null || !(section.getFather().get() instanceof TestCasePlayerType)) {

			CheckDownloadCaseAction.sendJSON(context, "error",
					"Unable to find TestCasePlayer with id '"
							+ playerid + "' , possibly because somebody else"
							+ " has edited the page.");
		}
		return section;
	}

	private boolean userCanView(UserActionContext context, Section<?> section) {
		return Environment.getInstance().getWikiConnector().userCanViewArticle(
				section.getTitle(),
				context.getRequest());
	}

	private Triple<TestCaseProvider, Section<?>, Article> getSelectedTestCaseTriple(Section<?> section, UserActionContext context) {
		Section<TestCasePlayerType> playerSection =
				Sections.cast(section.getFather(), TestCasePlayerType.class);
		List<Triple<TestCaseProvider, Section<?>, Article>> providers =
				de.knowwe.testcases.TestCaseUtils.getTestCaseProviders(playerSection);

		String selectedTestCaseId = TestCasePlayerRenderer.getSelectedTestCaseId(section,
				context);
		for (Triple<TestCaseProvider, Section<?>, Article> triple : providers) {
			if (triple.getA().getTestCase() != null) {
				String id = triple.getC().getTitle() + "/" + triple.getA().getName();
				if (id.equals(selectedTestCaseId)) {
					return triple;
				}
			}
		}
		if (!providers.isEmpty()) return providers.get(0);
		return null;
	}

}