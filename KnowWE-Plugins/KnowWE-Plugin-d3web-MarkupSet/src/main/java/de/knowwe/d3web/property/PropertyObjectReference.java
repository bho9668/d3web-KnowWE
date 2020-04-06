/*
 * Copyright (C) 2010 denkbares GmbH
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
package de.knowwe.d3web.property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.denkbares.strings.Identifier;
import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.NamedObject;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.manage.KnowledgeBaseUtils;
import de.d3web.we.knowledgebase.D3webCompileScript;
import de.d3web.we.knowledgebase.D3webCompiler;
import de.d3web.we.object.AnswerReference;
import de.d3web.we.object.AnswerReferenceRegistrationHandler;
import de.d3web.we.object.D3webTerm;
import de.d3web.we.object.D3webTermReference;
import de.d3web.we.object.NamedObjectReference;
import de.d3web.we.object.QuestionReference;
import de.knowwe.core.compile.terminology.TermCompiler;
import de.knowwe.core.kdom.objects.SimpleReferenceRegistrationScript;
import de.knowwe.core.kdom.objects.Term;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.sectionFinder.AllTextFinderTrimmed;
import de.knowwe.core.kdom.sectionFinder.RegexSectionFinder;
import de.knowwe.core.report.Messages;
import de.knowwe.kdom.constraint.ConstraintSectionFinder;
import de.knowwe.kdom.constraint.NoChildrenOfOtherTypesConstraint;

/**
 * This is a type that represents a {@link NamedObject}. Depending on type of the NamedObject, there are different
 * renderings. getNamedObject(...) returns the matching NamedObject from the {@link KnowledgeBase}.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 02.08.2011
 */
public class PropertyObjectReference extends D3webTermReference<NamedObject> {

	public PropertyObjectReference() {

		String questionAnswerPattern =
				"^\\s*(" + PropertyDeclarationType.NAME + "\\s*(?:#\\s*"
						+ PropertyDeclarationType.NAME + ")?)\\s*";
		this.setSectionFinder(new RegexSectionFinder(Pattern.compile(questionAnswerPattern), 1));

		QuestionReference questionReference = new QuestionReference();
		questionReference.setSectionFinder(new RegexSectionFinder(
				Pattern.compile("^\\s*(" + PropertyDeclarationType.NAME + ")\\s*#"), 1));
		questionReference.addCompileScript(new WildcardQuestionErrorRemover());
		this.addChildType(questionReference);

		PropertyAnswerReference answerReference = new PropertyAnswerReference();
		answerReference.setSectionFinder(new RegexSectionFinder(
				Pattern.compile("^#\\s*(" + PropertyDeclarationType.NAME + ")\\s*"), 1));
		this.addChildType(answerReference);
		NamedObjectReference namedObjectReference = new NamedObjectReference();
		namedObjectReference.setSectionFinder(new ConstraintSectionFinder(
				new AllTextFinderTrimmed(), NoChildrenOfOtherTypesConstraint.getInstance()));
		this.addChildType(namedObjectReference);
	}

	@Override
	public NamedObject getTermObject(D3webCompiler compiler, Section<? extends D3webTerm<NamedObject>> s) {
		Section<PropertyAnswerReference> propertyAnswerReference = Sections.child(s,
				PropertyAnswerReference.class);
		if (propertyAnswerReference != null) {
			return propertyAnswerReference.get().getTermObject(compiler, propertyAnswerReference);
		}
		Section<NamedObjectReference> namedObjectReference = Sections.child(s,
				NamedObjectReference.class);
		if (namedObjectReference != null) {
			return namedObjectReference.get().getTermObject(compiler, namedObjectReference);
		}
		return null;
	}

	/**
	 * Returns the identifiers that are referenced by the property. Note that there can be referenced multiple
	 * identifiers, using wildcards for questions.
	 */
	public List<Identifier> getTermIdentifiers(TermCompiler compiler, Section<PropertyObjectReference> namendObjectSection) {
		List<Identifier> objects = new ArrayList<>(1);
		Section<PropertyObjectReference.PropertyAnswerReference> answerReferenceSection =
				Sections.child(namendObjectSection, PropertyObjectReference.PropertyAnswerReference.class);
		if (answerReferenceSection != null) {
			Section<QuestionReference> questionReferenceSection = Sections.child(
					namendObjectSection, QuestionReference.class);
			Identifier answerIdentifier = answerReferenceSection.get()
					.getTermIdentifier(compiler, answerReferenceSection);
			if (questionReferenceSection != null && questionReferenceSection.getText().isEmpty()) {
				// question is a wild card, get all questions with the given
				// answer.
				Collection<Identifier> choiceIdentifiers =
						compiler.getTerminologyManager().getAllDefinedTermsOfType(Choice.class);
				for (Identifier choiceIdentifier : choiceIdentifiers) {
					if (Objects.equals(choiceIdentifier.getLastPathElement(), answerIdentifier.getLastPathElement())) {
						objects.add(choiceIdentifier);
					}
				}
			}
			else {
				objects.add(answerIdentifier);
			}
		}
		if (objects.isEmpty()) {
			// if no answer is referenced, directly add the reference to the question, if there is any
			Section<NamedObjectReference> reference = Sections.child(namendObjectSection, NamedObjectReference.class);
			if (reference != null) {
				objects.add(reference.get().getTermIdentifier(compiler, reference));
			}
		}
		return objects;
	}

	/**
	 * Returns the NamedObjects that are referenced by the property. Note that there can be referenced multiple
	 * NamedObjects, using wildcards for questions.
	 */
	public List<NamedObject> getTermObjects(D3webCompiler compiler, Section<PropertyObjectReference> namendObjectSection) {
		List<NamedObject> objects = new ArrayList<>(1);
		NamedObject object = namendObjectSection.get().getTermObject(compiler, namendObjectSection);
		if (object == null) {
			Section<QuestionReference> questionReferenceSection = Sections.child(
					namendObjectSection, QuestionReference.class);
			if (questionReferenceSection != null && questionReferenceSection.getText().isEmpty()) {
				// question is a wild card, get all questions with the given
				// answer.
				Section<PropertyAnswerReference> answerReferenceSection =
						Sections.child(namendObjectSection, PropertyAnswerReference.class);
				objects = getAllChoices(compiler, answerReferenceSection);
			}
		}
		else {
			objects.add(object);
		}
		return objects;
	}

	private List<NamedObject> getAllChoices(D3webCompiler compiler, Section<PropertyAnswerReference> answerReference) {
		List<Question> questions = compiler.getKnowledgeBase().getManager().getQuestions();
		List<NamedObject> choices = new ArrayList<>(questions.size());
		for (Question question : questions) {
			if (!(question instanceof QuestionChoice)) continue;

			QuestionChoice questionChoice = (QuestionChoice) question;
			Choice choice = KnowledgeBaseUtils.findChoice(questionChoice,
					answerReference.getText());
			if (choice != null) choices.add(choice);
		}
		return choices;
	}

	static class WildcardQuestionErrorRemover implements D3webCompileScript<QuestionReference> {

		@Override
		public void compile(D3webCompiler compiler, Section<QuestionReference> section) {
			if (section.getText().isEmpty()) {
				Messages.clearMessages(compiler, section,
						SimpleReferenceRegistrationScript.class);
			}
		}
	}

	public static class PropertyAnswerReference extends AnswerReference {

		public PropertyAnswerReference() {
			this.addCompileScript(new WildcardAnswerErrorRemover());
		}

		@Override
		public Section<QuestionReference> getQuestionSection(Section<? extends AnswerReference> s) {
			return Sections.child(s.getParent(), QuestionReference.class);
		}
	}

	static class WildcardAnswerErrorRemover implements D3webCompileScript<PropertyAnswerReference> {

		@Override
		public void compile(D3webCompiler compiler, Section<PropertyAnswerReference> section) {

			Section<QuestionReference> questionSection = section.get().getQuestionSection(section);
			if (questionSection != null && questionSection.getText().isEmpty()) {
				Messages.clearMessages(compiler, section,
						AnswerReferenceRegistrationHandler.class);
			}
		}
	}

	@Override
	public Class<?> getTermObjectClass(@Nullable TermCompiler compiler, Section<? extends Term> section) {
		return NamedObject.class;
	}
}
