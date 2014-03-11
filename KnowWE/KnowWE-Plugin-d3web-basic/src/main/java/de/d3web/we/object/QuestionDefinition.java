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
package de.d3web.we.object;

import java.util.Collection;

import de.d3web.core.knowledge.terminology.QASet;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionDate;
import de.d3web.core.knowledge.terminology.QuestionMC;
import de.d3web.core.knowledge.terminology.QuestionNum;
import de.d3web.core.knowledge.terminology.QuestionOC;
import de.d3web.core.knowledge.terminology.QuestionText;
import de.d3web.core.knowledge.terminology.QuestionYN;
import de.d3web.core.knowledge.terminology.QuestionZC;
import de.d3web.strings.Identifier;
import de.d3web.we.knowledgebase.D3webCompiler;
import de.d3web.we.reviseHandler.D3webHandler;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.kdom.objects.Term;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.kdom.renderer.StyleRenderer;

/**
 * Abstract Type for the definition of questions. A question with a given type is created and hooked into the root QASet
 * of the knowledge base. The hierarchical position in the terminology needs to be handled by the subclass.
 *
 * @author Jochen/Albrecht
 * @created 26.07.2010
 */
public abstract class QuestionDefinition extends QASetDefinition<Question> {

	public static enum QuestionType {
		OC, MC, YN, NUM, DATE, TEXT, INFO;
	}

	public QuestionDefinition() {
		this.addCompileScript(Priority.HIGHER, new CreateQuestionHandler());
		this.addCompileScript(Priority.LOW, new TerminologyLoopDetectionHandler<Question>());
		this.addCompileScript(Priority.LOWER, new TerminologyLoopResolveHandler<Question>());
		this.setRenderer(new ValueTooltipRenderer(StyleRenderer.Question));
	}

	@Override
	public final Class<?> getTermObjectClass(Section<? extends Term> section) {
		QuestionType questionType = getQuestionType(Sections.cast(section, QuestionDefinition.class));
		if (questionType == null) return Question.class;
		switch (questionType) {
			case DATE:
				return QuestionDate.class;
			case INFO:
				return QuestionZC.class;
			case MC:
				return QuestionMC.class;
			case NUM:
				return QuestionNum.class;
			case OC:
				return QuestionOC.class;
			case TEXT:
				return QuestionText.class;
			case YN:
				return QuestionYN.class;
		}
		return Question.class;
	}

	public abstract QuestionType getQuestionType(Section<QuestionDefinition> s);

	static class CreateQuestionHandler extends D3webHandler<QuestionDefinition> {

		@Override
		@SuppressWarnings("unchecked")
		public Collection<Message> create(D3webCompiler compiler,
										  Section<QuestionDefinition> section) {

			Identifier identifier = section.get().getTermIdentifier(section);
			Class<?> termObjectClass = section.get().getTermObjectClass(section);
			TerminologyManager terminologyHandler = compiler.getTerminologyManager();
			terminologyHandler.registerTermDefinition(compiler, section, termObjectClass,
					identifier);

			AbortCheck abortCheck = section.get().canAbortTermObjectCreation(compiler, section);
			if (abortCheck.hasErrors()) return abortCheck.getErrors();

			if (abortCheck.termExist()) {
				section.get().storeTermObject(compiler, section, (Question) abortCheck.getNamedObject());
			} else {
				QASet rootQASet = getKB(compiler).getRootQASet();

				String name = section.get().getTermName(section);

				QuestionType questionType = section.get().getQuestionType(section);
				if (questionType == null) {
					return Messages.asList(Messages.objectCreationError(
							"No type found for question '" + name + "'"));
				}

				Question question = null;
				if (questionType.equals(QuestionType.OC)) {
					question = new QuestionOC(rootQASet, name);
				}
				else if (questionType.equals(QuestionType.MC)) {
					question = new QuestionMC(rootQASet, name);
				}
				else if (questionType.equals(QuestionType.NUM)) {
					question = new QuestionNum(rootQASet, name);
				}
				else if (questionType.equals(QuestionType.YN)) {
					question = new QuestionYN(rootQASet, name);
				}
				else if (questionType.equals(QuestionType.DATE)) {
					question = new QuestionDate(rootQASet, name);
				}
				else if (questionType.equals(QuestionType.INFO)) {
					question = new QuestionZC(rootQASet, name);
				}
				else if (questionType.equals(QuestionType.TEXT)) {
					question = new QuestionText(rootQASet, name);
				}
				else {
					return Messages.asList(Messages.error(
							"No valid question type found for question '" + identifier + "'"));
				}
				section.get().storeTermObject(compiler, section, question);
			}

			// return success message
			return Messages.asList(Messages.objectCreatedNotice(
					termObjectClass.getSimpleName() + " " + identifier));

		}
	}

}
