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

package de.d3web.we.kdom;

import java.util.ArrayList;
import java.util.List;

import de.d3web.we.kdom.sectionFinder.SectionFinder;
import de.d3web.we.kdom.sectionFinder.SectionFinderResult;

/**
 * @author Jochen
 * 
 *         The MultiSectionFinder allows to combine multiple SectionFinder for
 *         one type (i.e. alternative syntax). It contains a list of
 *         SectionFinders, call them all, and returns all FindingResults to the
 *         parser
 * 
 *         WARNING: As the different SectionFinder are called independently with
 *         the same text, they possibly might allocate overlapping sections. The
 *         resulting (invalid) SectionFinderResult-Set will be returned to the
 *         parsing-algorithm which THEN reject all these findings (if any
 *         invalid allocations contained).
 * 
 */

public class MultiSectionFinder implements SectionFinder {

	private List<SectionFinder> finders = null;

	public MultiSectionFinder() {
		this.finders = new ArrayList<SectionFinder>();
	}

	public MultiSectionFinder(SectionFinder first) {
		this();
		this.addSectionFinder(first);
	}

	public MultiSectionFinder(List<SectionFinder> initialList) {
		this.finders = initialList;
	}

	public void addSectionFinder(SectionFinder f) {
		this.finders.add(f);
	}

	@Override
	public List<SectionFinderResult> lookForSections(String text, Section<?> father, Type type) {
		List<SectionFinderResult> results = new ArrayList<SectionFinderResult>();

		lookForSectionsOfType(text, father, type, 0, 0, results);

		// // iterates all finders and gathers together all SectionFinderResults
		// for (ISectionFinder finder : finders) {
		// List<SectionFinderResult> singleResult = finder.lookForSections(text,
		// father,
		// type);
		// if (singleResult != null) {
		// results.addAll(singleResult);
		// }
		// }
		return results;
	}

	private void lookForSectionsOfType(String text, Section<?> father, Type type, int finderNum, int offset, List<SectionFinderResult> results) {

		if (finderNum >= finders.size()) return;

		SectionFinder finder = finders.get(finderNum);
		
		finderNum++;

		List<SectionFinderResult> singleFinderResults = finder.lookForSections(text, father, type);
		
		int lastEnd = 0;
		if (singleFinderResults != null) {
			for (SectionFinderResult r : singleFinderResults) {
				if (r == null) {
					continue;
				}

				if (r.getStart() < lastEnd) {
					continue;
				}
				if (lastEnd < r.getStart()) {
					lookForSectionsOfType(text.substring(lastEnd, r.getStart()), father, type,
							finderNum, offset + lastEnd, results);
				}

				r.setStart(r.getStart() + offset);
				r.setEnd(r.getEnd() + offset);
				results.add(r);

				lastEnd = r.getEnd() - offset;
			}
		}
		if (lastEnd < text.length()) {
			lookForSectionsOfType(text.substring(lastEnd, text.length()), father, type, finderNum,
					offset + lastEnd, results);
		}
		
	}

}
