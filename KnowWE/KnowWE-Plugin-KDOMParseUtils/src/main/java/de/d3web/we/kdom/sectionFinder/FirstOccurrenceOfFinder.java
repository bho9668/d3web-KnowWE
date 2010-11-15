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

package de.d3web.we.kdom.sectionFinder;

import java.util.List;

import de.d3web.we.kdom.KnowWEObjectType;
import de.d3web.we.kdom.Section;

public class FirstOccurrenceOfFinder extends AbstractSingleResultFinder {

	private SectionFinder finder = null;

	public FirstOccurrenceOfFinder(String regex) {
		finder = new RegexSectionFinder(regex);
	}

	public FirstOccurrenceOfFinder(RegexSectionFinder f) {
		this.finder = f;
	}

	public FirstOccurrenceOfFinder(StringSectionFinder f) {
		this.finder = f;
	}

	@Override
	public SectionFinderResult lookForSection(String text, Section father, KnowWEObjectType type) {
		List<SectionFinderResult> secs = finder.lookForSections(text, father, type);
		if (secs.size() >= 1) return secs.get(0);
		return null;
	}

}
