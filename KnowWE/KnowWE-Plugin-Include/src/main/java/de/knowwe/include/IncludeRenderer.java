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
package de.knowwe.include;

import java.util.List;

import de.d3web.strings.Strings;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.report.Message.Type;
import de.knowwe.core.report.Messages;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupRenderer;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

class IncludeRenderer extends DefaultMarkupRenderer {

	public IncludeRenderer() {
	}

	@Override
	public void render(Section<?> includeMarkup, UserContext user, RenderResult result) {

		// check for errors in links and/or annotations
		Section<IncludeMarkup> section = Sections.cast(includeMarkup, IncludeMarkup.class);
		List<Section<InnerWikiReference>> references =
				Sections.successors(section, InnerWikiReference.class);
		for (Section<InnerWikiReference> ref : references) {
			ref.get().updateReferences(ref);
		}

		// show document title
		renderDocumentInfo(section, result);

		// parse parameters
		String frame = DefaultMarkupType.getAnnotation(section, IncludeMarkup.ANNOTATION_FRAME);
		boolean isFramed = Strings.equalsIgnoreCase(frame, "show")
				// TODO: deprecated, remove 'true' here after 2014
				|| Strings.equalsIgnoreCase(frame, "true");

		int zoom = 100;
		Section<?> zoomSection =
				DefaultMarkupType.getAnnotationContentSection(section,
						IncludeMarkup.ANNOTATION_ZOOM);
		if (zoomSection != null) {
			try {
				zoom = getZoomPercent(section);
				Messages.clearMessages(null, zoomSection, getClass());

			}
			catch (NumberFormatException e) {
				Messages.storeMessage(
						null, zoomSection, getClass(),
						Messages.error("Zoom value of '" + zoom
								+ "' is not a valid number or percentage"));
			}
		}

		Section<?> showDefSection =
				DefaultMarkupType.getAnnotationContentSection(section,
						IncludeMarkup.ANNOTATION_DEFINITION);
		String showDef = DefaultMarkupType.getAnnotation(section,
				IncludeMarkup.ANNOTATION_DEFINITION);
		boolean hasMessage = Messages.hasMessagesInSubtree(section, Type.ERROR, Type.WARNING);
		// show warning if an error message
		// forces a hidden definition to be shown
		if (hasMessage && Strings.equalsIgnoreCase(showDef, "hide")) {
			Messages.storeMessage(
					null, showDefSection, getClass(),
					Messages.warning("Annotation ignored due to error messages before"));
		}
		else if (showDefSection != null) {
			Messages.clearMessages(null, showDefSection, getClass());
		}

		// render default markup if requested or if there are errors
		if (hasMessage || Strings.equalsIgnoreCase(showDef, "show")) {
			super.render(section, user, result);
		}

		// render included sections
		for (Section<InnerWikiReference> include : references) {
			// find section and render it
			Section<?> referencedSection = include.get().getReferencedSection(include);
			int listLevel = include.get().getListMarks(include).length();
			// check for simple include
			if (listLevel == 0) {
				renderIncludedSections(user, referencedSection, result,
						zoom, isFramed, false);
				continue;
			}
			// if the include uses a explicit header declaration
			// with (*, #, or - in front), we render our own header
			// (if not preceded with "-")
			// and suppress the existing one
			if (!include.get().isSuppressHeader(include)) {
				String title = include.get().getLinkName(include);
				result.appendHtml("<h" + listLevel + ">")
						.append(title)
						.appendHtml("</h" + listLevel + ">\n");
			}
			renderIncludedSections(user, referencedSection, result,
					zoom, isFramed, true);
		}
	}

	private void renderDocumentInfo(Section<IncludeMarkup> section, RenderResult result) {
		String project = DefaultMarkupType.getAnnotation(section, IncludeMarkup.ANNOTATION_PROJECT);
		if (!Strings.isBlank(project)) {
			result.appendHtml("\n<div class='wikiBook-project'>")
					.append(project).appendHtml("</div>\n");
		}
		String title = DefaultMarkupType.getAnnotation(section, IncludeMarkup.ANNOTATION_TITLE);
		if (!Strings.isBlank(title)) {
			result.appendHtml("\n<div class='wikiBook-title'>")
					.append(title).appendHtml("</div>\n");
		}
		String author = DefaultMarkupType.getAnnotation(section, IncludeMarkup.ANNOTATION_AUTHOR);
		if (!Strings.isBlank(author)) {
			result.appendHtml("\n<div class='wikiBook-author'>")
					.append(author).appendHtml("</div>\n");
		}
	}

	private int getZoomPercent(Section<IncludeMarkup> section) throws NumberFormatException {
		String zoom = DefaultMarkupType.getAnnotation(section, IncludeMarkup.ANNOTATION_ZOOM);
		if (Strings.isBlank(zoom)) return 100;

		// parse value
		double factor = Double.parseDouble(zoom.replaceAll("%", "").trim());
		// if value indicates a factor instead of percentage (< 5)
		// and the percentage is not explicitly specified, correct
		if (factor < 5d && !zoom.contains("%")) factor *= 100;
		return (int) Math.round(factor);
	}

	public void renderIncludedSections(UserContext user, Section<?> targetSection, RenderResult result, int zoom, boolean framed, boolean skipHeader) {
		if (targetSection == null) return;
		if (zoom != 100) {
			result.appendHtml("<div style='zoom:" + zoom + "%; clear:both'>");
		}
		result.append("\n");
		if (framed) {
			// used the framing renderer
			new FramedIncludedSectionRenderer(skipHeader).render(
					targetSection, user, result);
		}
		else {
			// or simply render the sections belonging to the header
			FramedIncludedSectionRenderer.renderTargetSections(
					targetSection, skipHeader, user, result);
		}
		if (zoom != 100) {
			result.appendHtml("</div>");
		}
	}
}