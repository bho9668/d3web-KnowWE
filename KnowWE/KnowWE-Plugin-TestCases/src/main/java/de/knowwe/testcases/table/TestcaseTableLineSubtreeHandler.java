package de.knowwe.testcases.table;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.knowledge.terminology.QuestionMC;
import de.d3web.core.manage.KnowledgeBaseUtils;
import de.d3web.core.session.QuestionValue;
import de.d3web.core.session.Value;
import de.d3web.core.session.ValueFactory;
import de.d3web.empiricaltesting.Finding;
import de.d3web.empiricaltesting.RatedTestCase;
import de.d3web.strings.Strings;
import de.d3web.we.knowledgebase.D3webCompiler;
import de.d3web.we.object.QuestionReference;
import de.d3web.we.reviseHandler.D3webHandler;
import de.d3web.we.utils.D3webUtils;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.testcases.NameType;
import de.knowwe.testcases.TimeStampType;

/**
 * @author Reinhard Hatko
 * @created 16.03.2011
 */
final class TestcaseTableLineSubtreeHandler implements D3webHandler<TestcaseTableLine> {

	@Override
	public Collection<Message> create(D3webCompiler article, Section<TestcaseTableLine> s) {

		KnowledgeBase kb = D3webUtils.getKnowledgeBase(article);

		Section<TimeStampType> timeStamp = Sections.findSuccessor(s, TimeStampType.class);

		RatedTestCase ratedTestCase = new RatedTestCase();
		if (timeStamp != null) {
			try {
				long time = TimeStampType.getTimeInMillis(timeStamp);
				ratedTestCase.setTimeStamp(new Date(time));
			}
			catch (NumberFormatException e) {
				// illegal timestamp... just continue, error message will be
				// handled in TimeStampType
			}
		}

		Section<NameType> nameSection = Sections.findSuccessor(s, NameType.class);
		if (nameSection != null) {
			String rtcName = nameSection.get().getRTCName(nameSection);
			ratedTestCase.setName(rtcName);
		}

		List<Section<ValueType>> values = Sections.findSuccessorsOfType(s, ValueType.class);
		for (Section<ValueType> valueSec : values) {
			// if value is unchanged, ignore it
			String valueString = Strings.trimQuotes(valueSec.getText());
			if (valueString.isEmpty()) continue;
			if (valueString.equals("-")) continue;

			Section<? extends HeaderCell> headerCell = TestcaseTable.findHeaderCell(valueSec);

			if (headerCell == null) {
				Messages.storeMessage(article, valueSec, getClass(),
						Messages.noSuchObjectError("No header found for answer '"
								+ valueSec.getText() + "'."));
				continue;
			}

			Section<QuestionReference> qRef =
					Sections.findSuccessor(headerCell, QuestionReference.class);
			if (qRef == null) continue;

			String qName = Strings.trimQuotes(qRef.getText().trim());
			Question question = kb.getManager().searchQuestion(qName);
			if (question == null) continue;

			Value value = KnowledgeBaseUtils.findValue(question, valueString, false);
			if (value != null) {
				Finding existingFinding = null;
				if (question instanceof QuestionMC) {
					List<Finding> findings = ratedTestCase.getFindings();
					for (Finding finding : findings) {
						if (finding.getQuestion() == question) {
							existingFinding = finding;
							break;
						}
					}
					if (existingFinding != null) {
						QuestionValue existingValue = existingFinding.getValue();
						Value newValue = ValueFactory.createQuestionChoiceValue((QuestionChoice) question, valueString, existingValue);
						existingFinding.setValue((QuestionValue) newValue);
					}
				}
				if (existingFinding == null) ratedTestCase.add(new Finding(question, (QuestionValue) value));

			}
		}

		KnowWEUtils.storeObject(article, s, TestcaseTableLine.TESTCASE_KEY, ratedTestCase);
		return Collections.emptyList();
	}
}