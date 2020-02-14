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

package de.knowwe.core.kdom.parsing;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.sectionFinder.SectionFinderResult;
import de.knowwe.kdom.xml.AbstractXMLType;

public class DefaultSectionizerModule implements SectionizerModule {

	private static final DefaultSectionizerModule instance = new DefaultSectionizerModule();

	public static DefaultSectionizerModule getInstance() {
		return instance;
	}

	@Override
	@NotNull
	public Section<?> createSection(Section<?> parent, String parentText, Type childType, SectionFinderResult range) {
		Parser parser = childType.getParser();
		Section<? extends Type> section = parser.parse(parentText, parent);
		Map<String, String> parameterMap = range.getParameterMap();
		if (parameterMap != null) {
			section.storeObject(AbstractXMLType.ATTRIBUTE_MAP_STORE_KEY, parameterMap);
		}
		return section;
	}
}
