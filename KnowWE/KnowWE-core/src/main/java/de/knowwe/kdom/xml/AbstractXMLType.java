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

package de.knowwe.kdom.xml;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.sectionFinder.SectionFinderResult;
import de.knowwe.core.utils.KnowWEUtils;

public class AbstractXMLType extends AbstractType {

	public static final String HEAD = "head";
	public static final String TAIL = "tail";
	public static final String TAGNAME = "tagName";

	private final String xmlTagName;

	public static Section<XMLContent> getContentChild(Section<?> s) {
		if (s.get() instanceof AbstractXMLType) {
			Section<XMLContent> content = Sections.findSuccessor(s, XMLContent.class);
			return content;
		}
		return null;
	}

	@Override
	public String getName() {
		return getXMLTagName();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, String> getAttributeMapFor(Section<?> s) {
		return (Map<String, String>) KnowWEUtils.getStoredObject(s,
				SectionFinderResult.ATTRIBUTE_MAP_STORE_KEY);

	}

	// public static Map<String, String> getAttributeMapFor(
	// Section<? extends AbstractXMLObjectType> s) {
	// return (Map<String, String>) KnowWEUtils.getStoredObject(s.getWeb(), s
	// .getTitle(), s.getId(),
	// XMLSectionFinder.ATTRIBUTE_MAP_STORE_KEY);
	//
	// }

	public static String getTagName(Section<? extends AbstractXMLType> s) {
		Map<String, String> attributeMapFor = getAttributeMapFor(s);
		if (attributeMapFor != null) {
			return attributeMapFor.get(AbstractXMLType.TAGNAME);
		}
		else {
			return null;
		}
	}

	public static Section<AbstractXMLType> getXMLFatherElement(Section<? extends AbstractXMLType> s) {
		Section<AbstractXMLType> xmlFather = Sections.findAncestorOfType(s, AbstractXMLType.class);
		return xmlFather;
	}

	public static int getXMLDepth(Section<?> xmlSection) {

		return getXMLDepth(xmlSection, 0);
	}

	private static int getXMLDepth(Section<?> xmlSection, int depth) {
		Section<AbstractXMLType> xmlFather = Sections.findAncestorOfType(xmlSection,
				AbstractXMLType.class);
		if (xmlFather != null && xmlFather.get() instanceof AbstractXMLType) {
			return getXMLDepth(xmlFather, ++depth);
		}
		else {
			return depth;
		}

	}

	public static Section<? extends AbstractXMLType> findSubSectionOfTag(
			String tagname, Section<? extends AbstractXMLType> s) {
		String tagName2 = getTagName(s);
		if (tagname.equals(tagName2)) {
			return s;
		}

		List<Section<?>> children = s.getChildren();
		for (Section<?> section : children) {
			if (section.get() instanceof XMLContent) {
				List<Section<AbstractXMLType>> nodes = Sections
						.findChildrenOfType(section, AbstractXMLType.class);
				for (Section<? extends AbstractXMLType> section2 : nodes) {
					Section<? extends AbstractXMLType> found = findSubSectionOfTag(
							tagname, section2);
					if (found != null) return found;
				}

			}
		}
		return null;

	}

	@SuppressWarnings("unchecked")
	public static void findSubSectionsOfTag(
			String tagname, Section<? extends AbstractXMLType> s, Collection<Section<? extends AbstractXMLType>> c) {
		String tagName2 = getTagName(s);
		if (tagname.equals(tagName2)) {

			c.add(s);
		}
		List<Section<?>> children = s.getChildren();
		for (Section<?> section : children) {
			if (section.get() instanceof XMLContent) {
				Section<XMLContent> conSec = (Section<XMLContent>) section;
				List<Section<AbstractXMLType>> nodes = Sections
						.findChildrenOfType(conSec, AbstractXMLType.class);
				for (Section<AbstractXMLType> section2 : nodes) {
					findSubSectionsOfTag(
							tagname, section2, c);

				}
			}
		}
	}

	/**
	 * ObjectType for XML-Sections with tag name <code>tagName</code>.
	 */
	public AbstractXMLType(String tagName) {
		this(tagName, false);
	}

	/**
	 * ObjectType for XML-Sections with no specific tag name. Finds all
	 * XML-Sections independent of their tag names.
	 */
	public AbstractXMLType() {
		this("AnyXMLObjectType", true);
	}

	private AbstractXMLType(String tagName, boolean anyXML) {
		this.xmlTagName = tagName;
		this.addChildType(0, new XMLHead());
		this.addChildType(0, new XMLTail());
		this.setSectionFinder(new XMLSectionFinder(anyXML ? null : xmlTagName));
	}

	public String getXMLTagName() {
		return xmlTagName;
	}

}
