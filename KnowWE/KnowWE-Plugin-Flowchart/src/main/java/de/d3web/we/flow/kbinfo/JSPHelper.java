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

package de.d3web.we.flow.kbinfo;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import de.d3web.we.flow.type.FlowchartType;
import de.knowwe.core.KnowWEAttributes;
import de.knowwe.core.KnowWEEnvironment;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.kdom.xml.AbstractXMLType;

public class JSPHelper {

	private final UserContext userContext;

	public JSPHelper(UserContext userContext) {
		this.userContext = userContext;
		if (this.userContext.getWeb() == null) {
			this.userContext.getParameters().put(KnowWEAttributes.WEB,
					KnowWEEnvironment.DEFAULT_WEB);
		}
	}

	private static List<String> getAllMatches(String className, String web) {
		return SearchInfoObjects.searchObjects(
				KnowWEEnvironment.getInstance(),
				web,
				null, className, 65535);
	}

	public String getArticleIDsAsArray() {
		List<String> matches = getAllMatches("Article", this.userContext.getWeb());
		Collections.sort(matches);
		StringBuffer buffer = new StringBuffer();
		buffer.append("[");
		boolean first = true;
		for (String id : matches) {
			if (first) {
				first = false;
			}
			else {
				buffer.append(", ");
			}
			buffer.append("'").append(id).append("'");
		}
		buffer.append("]");
		return buffer.toString();
	}

	public String getKDOMNodeContent(String kdomID) {
		return KnowWEEnvironment.getInstance().getSectionText(kdomID);
	}

	public String getArticleInfoObjectsAsXML() {
		// search for matches
		List<String> matches = getAllMatches("Article", this.userContext.getWeb());

		// fill the response buffer
		StringBuilder bob = new StringBuilder();
		GetInfoObjects.appendHeader(bob);
		for (String id : matches) {
			GetInfoObjects.appendInfoObject(this.userContext.getWeb(), id, bob);
		}
		GetInfoObjects.appendFooter(bob);

		// and done
		return bob.toString();
	}

	public String getReferredInfoObjectsAsXML() {
		return getReferrdInfoObjectsAsXML(this.userContext.getWeb());
	}

	public static String getReferrdInfoObjectsAsXML(String web) {
		// TODO: extract used object ids from flowchart as a list
		// for now we simply use all existing objects
		List<String> matches = getAllMatches(null, web);

		// fill the response buffer
		StringBuilder bob = new StringBuilder();
		GetInfoObjects.appendHeader(bob);
		for (String id : matches) {
			GetInfoObjects.appendInfoObject(web, id, bob);
		}
		GetInfoObjects.appendFooter(bob);

		// and done
		return bob.toString();

	}

	@SuppressWarnings("unchecked")
	public String loadFlowchart(String kdomID) {

		Section<DefaultMarkupType> diaFluxSection = (Section<DefaultMarkupType>) Sections.getSection(kdomID);

		if (diaFluxSection == null) {
			throw new IllegalArgumentException("Could not find flowchart at: "
					+ kdomID);
		}

		Section<FlowchartType> flowchart = Sections.findSuccessor(diaFluxSection,
				FlowchartType.class);

		if (flowchart == null) {
			return getEmptyFlowchart();
		}

		String nodeData = KnowWEEnvironment.getInstance().getSectionText(flowchart.getID());
		return nodeData;
	}

	/**
	 * 
	 * @created 25.11.2010
	 * @return
	 */
	private String getEmptyFlowchart() {
		String id = createNewFlowchartID();
		return "<flowchart fcid=\"flow_"
				+ id
				+ "\" name=\"New Flowchart\" icon=\"sanduhr.gif\" width=\"750\" height=\"500\" idCounter=\"1\">"
				+ "</flowchart>";
	}

	/**
	 * 
	 * @created 23.02.2011
	 * @return
	 */
	public static String createNewFlowchartID() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	public String getFlowchartID() {
		return getFlowchartAttributeValue("fcid");
	}

	public String getFlowchartWidth() {
		return getFlowchartAttributeValue("width");
	}

	public String getFlowchartHeight() {
		return getFlowchartAttributeValue("height");
	}

	private String getFlowchartAttributeValue(String attributeName) {
		Section<FlowchartType> section = Sections.findSuccessor(
				Sections.getSection(userContext.getParameter("kdomID")), FlowchartType.class);

		return AbstractXMLType.getAttributeMapFor(section).get(attributeName);
	}
}
