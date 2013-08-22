/*
 * Copyright (C) 2010 Chair of Artificial Intelligence and Applied Informatics
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

package de.knowwe.diaflux;

import java.io.IOException;
import java.util.List;

import de.knowwe.core.Attributes;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.RootType;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.diaflux.type.FlowchartType;
import de.knowwe.kdom.xml.AbstractXMLType;

/**
 * @author Reinhard Hatko
 * 
 *         Created: 18.06.2010
 */
public class LoadFlowchartAction extends AbstractAction {

	@Override
	public void execute(UserActionContext context) throws IOException {

		String nodeID = context.getParameter(Attributes.SECTION_ID);
		Section<FlowchartType> section = Sections.getSection(nodeID, FlowchartType.class);

		if (section != null) {
			writeSource(context, section.getText());
		}
		// TODO error handling

	}

	/**
	 * 
	 * @created 25.10.2012
	 * @param child
	 * @return
	 */
	public static <T extends AbstractXMLType> String getFlowchartId(Section<T> child) {
		return AbstractXMLType.getAttributeMapFor(child).get("fcid");
	}

	public static Section<FlowchartType> findFlowInDifferentVersion(Section<FlowchartType> flow, List<Section<FlowchartType>> flows) {
		String id = getFlowchartId(flow);
		for (Section<FlowchartType> oldFlow : flows) {
			String oldId = getFlowchartId(oldFlow);
			if (oldId.equals(id)) return oldFlow;
		}
		return null;
	}

	// FIXME This method is a dirty hack and should be removed in the future:
	// It is used to sectionize old versions of an article to get the
	// diaflux-related stuff without altering the KB and so on
	//
	public static Section<RootType> sectionizeArticle(String text) {
		Section<RootType> rootSection = Section.createSection(text, RootType.getInstance(), null);
		Article article = Article.createArticle("", "fakeArticleforDiff", "default_web", true);
		rootSection.setArticle(article);
		RootType.getInstance().getParser().parse(text, rootSection);
		return rootSection;
	}

	/**
	 * 
	 * @created 23.10.2012
	 * @param context
	 * @param section
	 * @throws IOException
	 */
	private static void writeSource(UserActionContext context, String flowSource) throws IOException {
		String source = FlowchartUtils.removePreview(flowSource);

		// TODO fix xml-soup of source and set content type to xml
		// Problem: '<' and '>' in e.g. conditions that would have to be escaped
		// properly
		context.setContentType("text/xml");
		context.getWriter().write("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n");
		context.getWriter().write(source);
	}

}
