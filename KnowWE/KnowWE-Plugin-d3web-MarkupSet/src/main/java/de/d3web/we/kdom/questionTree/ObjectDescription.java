package de.d3web.we.kdom.questionTree;

import java.util.Arrays;
import java.util.Collection;

import de.d3web.core.knowledge.TerminologyObject;
import de.d3web.core.knowledge.terminology.info.MMInfo;
import de.d3web.core.knowledge.terminology.info.Property;
import de.d3web.we.object.D3webTermDefinition;
import de.d3web.we.utils.D3webUtils;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.KnowWEArticle;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.subtreeHandler.SubtreeHandler;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.core.utils.SplitUtility;
import de.knowwe.kdom.renderer.StyleRenderer;
import de.knowwe.kdom.sectionFinder.MatchUntilEndFinder;
import de.knowwe.kdom.sectionFinder.StringSectionFinderUnquoted;

/**
 * A type to allow for the definition of (extended) question-text for a question
 * leaded by '~'
 * 
 * the subtreehandler creates the corresponding DCMarkup using
 * MMInfoSubject.PROMPT for the question object
 * 
 * @author Jochen
 * 
 */
public class ObjectDescription extends AbstractType {

	private static final String QTEXT_START_SYMBOL = "~";

	public ObjectDescription(final Property<?> prop) {
		this.sectionFinder = new MatchUntilEndFinder(new StringSectionFinderUnquoted(
					QTEXT_START_SYMBOL));

		this.setRenderer(StyleRenderer.PROMPT);
		this.addSubtreeHandler(new SubtreeHandler<ObjectDescription>() {

			@Override
			public Collection<Message> create(KnowWEArticle article, Section<ObjectDescription> sec) {

				@SuppressWarnings("rawtypes")
				Section<D3webTermDefinition> qDef = Sections.findSuccessor(
						sec.getFather(), D3webTermDefinition.class);

				if (qDef != null) {

					// get the object the information should be stored for
					@SuppressWarnings("unchecked")
					Object ob = qDef.get().getTermObject(article, qDef);
					TerminologyObject object = null;
					if (ob instanceof TerminologyObject) {
						object = (TerminologyObject) ob;
					}

					if (object != null) {
						// if its MMINFO then it a question, so set text as
						// prompt
						String objectDescriptionText = ObjectDescription.getObjectDescriptionText(sec);
						if (prop.equals(MMInfo.PROMPT)) {
							object.getInfoStore().addValue(MMInfo.PROMPT, objectDescriptionText);
							return Arrays.asList(Messages.objectCreatedNotice(
									D3webUtils.getD3webBundle()
											.getString(
													"KnowWE.questiontree.questiontextcreated")
											+ " " + objectDescriptionText));
						} // for any other properties (than MMINFO) set
							// information normally
						else {
							object.getInfoStore().addValue(
									prop,
									objectDescriptionText);
							return Messages.asList(Messages.objectCreatedNotice(
									"Explanation set: " + objectDescriptionText));
						}
					}
				}
				return Messages.asList(Messages.objectCreationError(
							D3webUtils.getD3webBundle()
									.getString("KnowWE.questiontree.questiontext")));
			}
		});
	}

	public static String getObjectDescriptionText(Section<ObjectDescription> s) {
		String text = s.getText();
		if (text.startsWith(QTEXT_START_SYMBOL)) {
			text = text.substring(1).trim();
		}

		return SplitUtility.unquote(text);
	}
}
