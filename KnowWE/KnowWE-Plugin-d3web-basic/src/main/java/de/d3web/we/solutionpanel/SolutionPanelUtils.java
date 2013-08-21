package de.d3web.we.solutionpanel;

import de.d3web.core.knowledge.TerminologyObject;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.Rating;
import de.d3web.core.knowledge.terminology.Rating.State;
import de.d3web.core.knowledge.terminology.Solution;
import de.d3web.core.knowledge.terminology.info.MMInfo;
import de.d3web.core.session.Session;
import de.d3web.core.session.Value;
import de.d3web.core.session.values.MultipleChoiceValue;
import de.d3web.core.session.values.NumValue;
import de.d3web.core.session.values.UndefinedValue;
import de.d3web.core.session.values.Unknown;
import de.d3web.strings.Strings;
import de.d3web.we.utils.D3webUtils;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.taghandler.ObjectInfoTagHandler;

public class SolutionPanelUtils {

	public static boolean isShownObject(String[] allowedParents, String[] excludedParents, TerminologyObject to) {
		return (allowedParents.length == 0 || isOrHasParent(allowedParents, to))
				&& (excludedParents.length == 0 || !isOrHasParent(excludedParents, to));
	}

	private static boolean isOrHasParent(String[] allowedParents, TerminologyObject object) {

		if (object.getParents() == null) return false;
		if (arrayIgnoreCaseContains(allowedParents, object.getName())) return true;

		for (TerminologyObject parent : object.getParents()) {
			if (arrayIgnoreCaseContains(allowedParents, parent.getName())) return true;
			if (isOrHasParent(allowedParents, parent)) return true;
		}
		return false;
	}

	private static boolean arrayIgnoreCaseContains(String[] allowedParents, String name) {
		for (String string : allowedParents) {
			if (name.equalsIgnoreCase(string)) {
				return true;
			}
		}
		return false;
	}

	public static void renderSolution(Solution solution, Session session, boolean endUser, RenderResult content) {
		// TODO: look for internationalization and only print getName,
		// when no intlz is available
		// content.append("* ");

		String link = solution.getInfoStore().getValue(MMInfo.LINK);
		String prompt = solution.getInfoStore().getValue(MMInfo.PROMPT);
		String description = solution.getInfoStore().getValue(MMInfo.DESCRIPTION);
		String infoLink = "Wiki.jsp?page=ObjectInfoPage" +
				"&amp;" + ObjectInfoTagHandler.TERM_IDENTIFIER + "=" + solution.getName() +
				"&amp;" + ObjectInfoTagHandler.OBJECT_NAME + "=" + solution.getName();

		String tooltip = "";
		if (description != null) tooltip = description;

		String label = solution.getName();
		if (prompt != null) {
			tooltip = label + "\n" + tooltip;
			label = prompt;
		}
		tooltip = Strings.encodeHtml(tooltip.trim());

		// fetch derivation state icon
		Rating solutionRating = D3webUtils.getRatingNonBlocking(session, solution);
		appendImage(solutionRating, content);
		String stateName = String.valueOf(solutionRating);

		content.appendHtml("<span title=\"" + tooltip + "\" class=\"SOLUTION-" + stateName + "\">");
		if (endUser) {
			// show solution in end user mode
			if (prompt == null && description != null) {
				label = description;
			}
			if (link != null) {
				content.appendHtml("<a href='" + Strings.encodeHtml(link) + "'>");
				content.append(label);
				content.appendHtml("</a>");
			}
			else {
				content.append(label);
			}
		}
		else {
			// show solution in developer mode
			if (!tooltip.isEmpty()) tooltip = "title='" + tooltip.replace('\'', '"') + "' ";
			content.appendHtml("<a href='" + Strings.encodeHtml(infoLink) + "'>");
			content.append(label);
			content.appendHtml("</a>");

			if (link != null) {
				content.appendHtml(" (" + "<a href='" + Strings.encodeHtml(link)
						+ "' target='solutionLink'>link</a>"
						+ ")");
			}
		}
		content.appendHtml("</span>\n");
	}

	public static void appendImage(Rating solutionRating, RenderResult content) {
		if (solutionRating == null) {
			appendImage("KnowWEExtension/images/fsp_calculating.gif",
					"value in calculation, please reload later", content);
		}
		else if (solutionRating.hasState(State.ESTABLISHED)) {
			appendImage("KnowWEExtension/images/fsp_established.gif", "Established", content);
		}
		else if (solutionRating.hasState(State.SUGGESTED)) {
			appendImage("KnowWEExtension/images/fsp_suggested.gif", "Suggested", content);
		}
		else if (solutionRating.hasState(State.EXCLUDED)) {
			appendImage("KnowWEExtension/images/fsp_excluded.gif", "Excluded", content);
		}
	}

	private static void appendImage(String filename, String altText, RenderResult content) {
		content.appendHtml(" <img src='" + filename
				+ "' id='sstate-update' class='pointer'"
				+ " align='top' alt='" + Strings.encodeHtml(altText) + "'"
				+ " title='" + Strings.encodeHtml(altText) + "' "
				+ "/> ");
	}

	public static void renderAbstraction(Question question, Session session, int digits, RenderResult buffer) {
		// TODO: look for internationalization and only print getName,
		// when no intlz is available
		// buffer.append("* ");
		appendImage("KnowWEExtension/images/fsp_abstraction.gif", "Abstraction", buffer);
		buffer.appendHtml("<span class=\"ABSTRACTION\">");
		// render the abstraction question with value
		Value value = D3webUtils.getValueNonBlocking(session, question);
		if (value == null) {
			buffer.appendHtml("<i style='color:grey'>value in calculation, please reload later</i>");
		}
		else {
			buffer.append(question.getName()
					+ " = "
					+ formatValue(value, digits));
		}

		// add the unit name for num question, if available
		String unit = question.getInfoStore().getValue(MMInfo.UNIT);
		if (unit != null) {
			buffer.append(" " + unit);
		}

		buffer.appendHtml("</span>" + "\n");
	}

	/**
	 * Renders the string representation of the specified value. For a
	 * {@link NumValue} the float is truncated to its integer value, when
	 * possible.
	 * 
	 * @created 19.10.2010
	 * @param value the specified value
	 * @return A string representation of the specified value.
	 */
	public static String formatValue(Value value, int digits) {
		if (value instanceof NumValue) {
			Double numValue = (Double) value.getValue();
			// check, if we need to round the value

			if (digits >= 0) {
				double d = Math.pow(10, digits);
				numValue = (Math.round(numValue * d) / d);
			}
			// cut an ending .0 when appropriate
			if (Math.abs(numValue - Math.round(numValue)) > 0) {
				return numValue.toString();
			}
			else {
				return String.valueOf(Math.round(numValue));
			}
		}
		else if (value instanceof MultipleChoiceValue) {
			String mcText = value.toString();
			// remove the brackets
			return mcText.substring(1, mcText.length() - 1);
		}
		else if (value instanceof Unknown || value instanceof UndefinedValue) return "-";
		else return value.toString();
	}

}
