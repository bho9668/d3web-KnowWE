/*
 * Copyright (C) 2012 denkbares GmbH
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
package de.knowwe.testcases;

import java.io.IOException;
import java.util.Date;

import de.d3web.core.inference.SessionTerminatedException;
import de.d3web.core.session.Session;
import de.d3web.core.session.SessionFactory;
import de.d3web.testcase.TestCaseUtils;
import de.d3web.testcase.model.Check;
import de.d3web.testcase.model.TestCase;
import de.d3web.utils.Log;
import de.d3web.we.utils.D3webUtils;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

/**
 * Action that gets called when cases should be executed with the
 * SessionDebugger
 * 
 * @author Markus Friedrich (denkbares GmbH)
 * @created 19.01.2012
 */
public class ExecuteCasesAction extends AbstractAction {

	@Override
	public void execute(UserActionContext context) throws IOException {
		String sectionid = context.getParameter("id");
		String testCaseName = context.getParameter("testCaseName");
		Date endDate;
		try {
			endDate = new Date(Long.parseLong((context.getParameter("date"))));
		}
		catch (NumberFormatException e) {
			throw new IOException(e);
		}

		Section<?> section = Sections.getSection(sectionid);
		if (section == null) {
			context.sendError(409, "Section '" + sectionid
					+ "' could not be found, possibly because somebody else"
					+ " has edited the page.");
			return;
		}
		TestCaseProviderStorage providerStorage = de.knowwe.testcases.TestCaseUtils.getTestCaseProviderStorage(
				section);
		TestCaseProvider provider = providerStorage.getTestCaseProvider(testCaseName);
		Session session = provider.getActualSession(context);
		SessionDebugStatus status = provider.getDebugStatus(context);
		TestCase testCase = provider.getTestCase();
		// reset session
		if (session != status.getSession() || status.getLastExecuted() == null) {
			session = SessionFactory.createSession(session.getKnowledgeBase(),
					testCase.getStartDate());
			provider.storeSession(session, context);
			runTo(session, testCase, null, endDate, status);
			status.setLastExecuted(endDate);
		}
		else {
			runTo(session, testCase, status.getLastExecuted(), endDate, status);
			status.setLastExecuted(endDate);
		}
		D3webUtils.handleLoopDetectionNotification(section.getArticleManager(), context, session);
	}

	private static void runTo(Session session, TestCase testCase, Date startDate, Date endDate, SessionDebugStatus status) {
		try {

			for (Date date : testCase.chronology()) {
				if ((startDate == null || date.after(startDate))
						&& (date.before(endDate) || date.equals(endDate))) {
					TestCaseUtils.applyFindings(session, testCase, date);
					for (Check c : testCase.getChecks(date, session.getKnowledgeBase())) {
						status.addCheckResult(date, c, c.check(session));
					}
					status.finished(date);
				}
			}
		}
		catch (SessionTerminatedException e) {
			Log.warning("Propagation terminated due to detected loop.", e);
		}
	}
}
