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

package de.d3web.we.kdom.questionTree.setValue;

import java.util.List;

import de.d3web.abstraction.ActionSetQuestion;
import de.d3web.abstraction.inference.PSMethodAbstraction;
import de.d3web.core.inference.Rule;
import de.d3web.core.inference.condition.Condition;
import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.manage.RuleFactory;
import de.d3web.strings.Strings;
import de.d3web.we.kdom.questionTree.QuestionDashTreeUtils;
import de.d3web.we.knowledgebase.D3webCompileScript;
import de.d3web.we.knowledgebase.D3webCompiler;
import de.d3web.we.object.AnswerReference;
import de.d3web.we.object.QuestionReference;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.sectionFinder.AllTextSectionFinder;
import de.knowwe.core.kdom.sectionFinder.SectionFinder;
import de.knowwe.core.kdom.sectionFinder.SectionFinderResult;
import de.knowwe.core.report.CompilerMessage;
import de.knowwe.core.report.Messages;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.kdom.dashtree.DashTreeUtils;
import de.knowwe.kdom.sectionFinder.AllBeforeTypeSectionFinder;
import de.knowwe.kdom.sectionFinder.ConditionalSectionFinder;

public class QuestionSetValueLine extends AbstractType {

	private static final String SETVALUE_ARGUMENT = "SetValueArgument";
	private static final String OPEN = "(";
	private static final String CLOSE = ")";

	public QuestionSetValueLine() {
		this.setSectionFinder(new ConditionalSectionFinder(AllTextSectionFinder.getInstance()) {

			@Override
			protected boolean condition(String text, Section<?> father) {
				return Strings.containsUnquoted(text, OPEN)
						&& Strings.containsUnquoted(text, CLOSE);

			}
		});

		AnswerPart argumentType = new AnswerPart();
		this.addChildType(argumentType);
		this.addChildType(createObjectRefTypeBefore(argumentType));

	}

	private Type createObjectRefTypeBefore(
			AbstractType typeAfter) {
		QuestionReference qid = new QuestionReference();
		qid.setSectionFinder(new AllBeforeTypeSectionFinder(typeAfter));
		qid.addCompileScript(new CreateSetValueRuleHandler());
		return qid;
	}

	static class CreateSetValueRuleHandler extends D3webCompileScript<QuestionReference> {

		@Override
		public void destroy(D3webCompiler compiler, Section<QuestionReference> section) {
			Rule kbr = (Rule) section.getSectionStore().getObject(compiler, SETVALUE_ARGUMENT);
			if (kbr != null) kbr.remove();
		}

		@Override
		public void compile(D3webCompiler compiler, Section<QuestionReference> section) throws CompilerMessage {

			Question q = section.get().getTermObject(compiler, section);

			Section<AnswerReference> answerSec = Sections.findSuccessor(
					section.getParent(), AnswerReference.class);

			String answerName = answerSec.get().getTermName(answerSec);

			if (q != null) {
				Choice a = null;
				if (q instanceof QuestionChoice) {
					QuestionChoice qc = (QuestionChoice) q;
					List<Choice> allAlternatives = qc.getAllAlternatives();
					for (Choice answerChoice : allAlternatives) {
						if (answerChoice.getName().equals(answerName)) {
							a = answerChoice;
							break;
						}
					}

					if (a != null) {
						Condition cond = QuestionDashTreeUtils.createCondition(compiler,
								DashTreeUtils.getAncestorDashTreeElements(section));
						if (cond == null) {
							throw new CompilerMessage(
									Messages.creationFailedWarning(
											Rule.class.getSimpleName() + ": check condition"));
						}

						ActionSetQuestion ac = null;
						if (q != null && a != null) {
							ac = new ActionSetQuestion();
							ac.setQuestion(q);
							ac.setValue(a);
						}

						Rule r = null;
						if (ac != null) {
							r = RuleFactory.createRule(ac, cond, null,
									PSMethodAbstraction.class);
						}

						if (r != null) {
							KnowWEUtils.storeObject(compiler, section, SETVALUE_ARGUMENT, r);
							throw new CompilerMessage(
									Messages.objectCreatedNotice(r.getClass().toString()));
						}

					}
				}
			}

			throw new CompilerMessage(
					Messages.creationFailedWarning(Rule.class.getSimpleName()));
		}

	}

	/**
	 * 
	 * A type for an AnswerReference in brackets like '(AnswerXY)'
	 * 
	 * @author Jochen
	 * @created 26.07.2010
	 */
	class AnswerPart extends AbstractType {

		public AnswerPart() {
			this.setSectionFinder(new SectionFinder() {

				@Override
				public List<SectionFinderResult> lookForSections(String text,
						Section<?> father, Type type) {

					return SectionFinderResult
							.createSingleItemList(new SectionFinderResult(
									Strings.indexOfUnquoted(text, OPEN),
									Strings.indexOfUnquoted(text, CLOSE) + 1));
				}
			});

			AnswerReferenceInsideBracket answerReferenceInsideBracket = new AnswerReferenceInsideBracket();
			answerReferenceInsideBracket.setSectionFinder(new SectionFinder() {

				@Override
				public List<SectionFinderResult> lookForSections(String text,
						Section<?> father, Type type) {

					return SectionFinderResult
							.createSingleItemList(new SectionFinderResult(
									1,
									text.length() - 1));
				}
			});
			this.addChildType(answerReferenceInsideBracket);
		}

		class AnswerReferenceInsideBracket extends AnswerReference {

			@Override
			public Section<QuestionReference> getQuestionSection(Section<? extends AnswerReference> s) {
				return Sections.findSuccessor(s.getParent().getParent(),
						QuestionReference.class);
			}

		}

	}

}
