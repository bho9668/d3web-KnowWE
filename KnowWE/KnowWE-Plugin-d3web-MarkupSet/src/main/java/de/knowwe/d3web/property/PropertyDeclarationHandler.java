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
import java.util.Locale;

import de.d3web.core.knowledge.InfoStore;
import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.NamedObject;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.knowledge.terminology.info.Property;
import de.d3web.core.manage.KnowledgeBaseUtils;
import de.d3web.strings.Strings;
import de.d3web.we.object.QuestionReference;
import de.d3web.we.reviseHandler.D3webSubtreeHandler;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.d3web.property.PropertyObjectReference.PropertyAnswerReference;

/**
 * Parses a line defining a property
 * 
 * Syntax of the line: IDOBjectName.Property(.Language(.Country)?)? = content
 * 
 * @author Markus Friedrich, Albrecht Striffler (denkbares GmbH)
 * @created 10.11.2010
 */
public class PropertyDeclarationHandler extends D3webSubtreeHandler<PropertyDeclarationType> {

	@Override
	public Collection<Message> create(Article article, Section<PropertyDeclarationType> section) {
		if (Strings.isBlank(section.getText())) return Messages.noMessage();
		// get NamedObject
		Section<PropertyObjectReference> namendObjectSection = Sections.findSuccessor(section,
				PropertyObjectReference.class);
		if (namendObjectSection == null) {
			return Messages.asList(Messages.syntaxError("No NamedObject found."));
		}
		List<NamedObject> objects = getNamedObjects(article, namendObjectSection);
		if (objects.isEmpty()) {
			return Messages.asList(Messages.error("No matching object(s) found for reference '"
					+ namendObjectSection.get().getTermIdentifier(namendObjectSection) + "'"));
		}

		// get Property
		Section<PropertyType> propertySection = Sections.findSuccessor(section,
				PropertyType.class);
		if (propertySection == null) {
			return Messages.asList(Messages.syntaxError("No property found."));
		}
		Property<?> property = propertySection.get().getProperty(propertySection);
		if (property == null) {
			return Messages.asList(Messages.noSuchObjectError(Property.class.getSimpleName(),
					propertySection.getText().trim()));
		}

		// get Locale
		Locale locale = getLocale(section);

		// get content
		Section<PropertyContentType> contentSection = Sections.findSuccessor(section,
				PropertyContentType.class);
		if (contentSection == null) {
			return Messages.asList(Messages.syntaxError("No property value found for property '"
					+ property + "'."));
		}
		String content = contentSection.get().getPropertyContent(contentSection);
		if (content == null || content.trim().isEmpty()) {
			return Messages.asList(Messages.syntaxError("No property value found for property '"
					+ property.getName() + "'."));
		}

		Object value;
		try {
			value = property.parseValue(content);
		}
		catch (NoSuchMethodException e) {
			return Messages.asList(Messages.syntaxError("The property '" + property.getName()
					+ "' is not supported by the %%Propery markup."));
		}
		catch (IllegalArgumentException e) {
			return Messages.asList(Messages.syntaxError("The property value '" + content
					+ "' is not compatible with the property '" + property + "'."));
		}
		for (NamedObject namedObject : objects) {
			try {
				namedObject.getInfoStore().addValue(property, locale, value);
			}
			catch (IllegalArgumentException e) {
				return Messages.asList(Messages.syntaxError("The property '" + property.getName() +
						"' cannot be localized."));
			}
		}

		// check for warning if using deprecated name
		String propName = propertySection.getText().trim();
		if (!property.getName().equalsIgnoreCase(propName)) {
			return Messages.asList(Messages.warning(
					"Property '" + propName + "' is deprecated. " +
							"Use '" + property.getName() + "' instead."));
		}
		return Messages.asList(Messages.notice("Property declaration successful."));
	}

	protected Locale getLocale(Section<PropertyDeclarationType> s) {
		Section<LocaleType> localeSection = Sections.findSuccessor(s, LocaleType.class);
		Locale locale = InfoStore.NO_LANGUAGE;
		if (localeSection != null) {
			locale = localeSection.get().getLocale(localeSection);
		}
		return locale;
	}

	protected List<NamedObject> getNamedObjects(Article article, Section<PropertyObjectReference> namendObjectSection) {
		List<NamedObject> objects = new ArrayList<NamedObject>(1);
		NamedObject object = namendObjectSection.get().getTermObject(article, namendObjectSection);
		if (object == null) {
			Section<QuestionReference> questionReferenceSection = Sections.findChildOfType(
					namendObjectSection, QuestionReference.class);
			if (questionReferenceSection != null && questionReferenceSection.getText().isEmpty()) {
				// question is a wild card, get all questions with the given
				// answer.
				Section<PropertyAnswerReference> answerReferenceSection =
						Sections.findChildOfType(namendObjectSection, PropertyAnswerReference.class);
				objects = getAllChoices(article, answerReferenceSection);
			}
		}
		else {
			objects.add(object);
		}
		return objects;
	}

	protected List<NamedObject> getAllChoices(Article article, Section<PropertyAnswerReference> answerReference) {
		List<Question> questions = getKB(article).getManager().getQuestions();
		List<NamedObject> choices = new ArrayList<NamedObject>(questions.size());
		for (Question question : questions) {
			if (!(question instanceof QuestionChoice)) continue;

			QuestionChoice questionChoice = (QuestionChoice) question;
			Choice choice = KnowledgeBaseUtils.findChoice(questionChoice,
					answerReference.getText());
			if (choice != null) choices.add(choice);

		}
		return choices;
	}

	@Override
	public void destroy(Article article, Section<PropertyDeclarationType> s) {
		Section<PropertyObjectReference> idobjectSection = Sections.findSuccessor(s,
				PropertyObjectReference.class);
		Section<PropertyType> propertySection = Sections.findSuccessor(s,
				PropertyType.class);
		if (idobjectSection == null) return;
		NamedObject object = idobjectSection.get().getTermObject(article, idobjectSection);
		if (object == null) return;
		if (propertySection == null) return;
		Property<?> property = propertySection.get().getProperty(propertySection);
		if (property == null) return;
		Section<LocaleType> localeSection = Sections.findSuccessor(s, LocaleType.class);
		Section<PropertyContentType> contentSection = Sections.findSuccessor(s,
				PropertyContentType.class);
		if (contentSection == null) return;
		String content = contentSection.getText();
		if (content == null || content.trim().isEmpty()) {
			return;
		}
		Locale locale = InfoStore.NO_LANGUAGE;
		if (localeSection != null) {
			locale = localeSection.get().getLocale(localeSection);
		}
		try {
			object.getInfoStore().remove(property, locale);
		}
		catch (IllegalArgumentException e) {
			return;
		}
		return;
	}
}
