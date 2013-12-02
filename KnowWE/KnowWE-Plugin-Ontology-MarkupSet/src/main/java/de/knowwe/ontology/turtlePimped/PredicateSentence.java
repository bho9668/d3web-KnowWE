/*
 * Copyright (C) 2013 denkbares GmbH
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
package de.knowwe.ontology.turtlePimped;

import java.util.List;

import de.d3web.strings.StringFragment;
import de.d3web.strings.Strings;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.sectionFinder.SectionFinder;
import de.knowwe.core.kdom.sectionFinder.SectionFinderResult;


public class PredicateSentence extends AbstractType {

	public PredicateSentence() {
		this.setSectionFinder(new PredicateSentenceSectionFinder());
		this.addChildType(new Predicate());
		this.addChildType(new ObjectList());
	}

	class PredicateSentenceSectionFinder implements SectionFinder {


		@Override
		public List<SectionFinderResult> lookForSections(String text, Section<?> father, Type type) {
			List<StringFragment> sentences = Strings.splitUnquoted(text, ";", true,
					TurtleMarkup.TURTLE_QUOTES);
			return SectionFinderResult.createResultList(sentences);
		}

	}
}
