/*
 * Copyright (C) 2010 University Wuerzburg, Computer Science VI
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
package de.knowwe.core.kdom.objects;

import de.d3web.strings.Identifier;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.compile.terminology.RenamableTerm;
import de.knowwe.core.compile.terminology.TermRegistrationScope;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.parsing.Section;

/**
 * 
 * @author Albrecht
 * @created 16.12.2010
 */
public abstract class SimpleReference extends AbstractType implements TermReference, RenamableTerm {

	private final Class<?> termObjectClass;

	public SimpleReference(TermRegistrationScope scope, Class<?> termObjectClass) {
		this(scope, termObjectClass, Priority.DEFAULT);
	}

	public SimpleReference(TermRegistrationScope scope, Class<?> termObjectClass, Priority registrationPriority) {
		this.termObjectClass = termObjectClass;
		this.addSubtreeHandler(registrationPriority, new SimpleTermReferenceRegistrationHandler(
				scope));
	}

	@Override
	public Class<?> getTermObjectClass(Section<? extends Term> section) {
		return termObjectClass;
	}

	@Override
	public String getTermName(Section<? extends Term> section) {
		return section.getText();
	}

	@Override
	public Identifier getTermIdentifier(Section<? extends Term> section) {
		return new Identifier(getTermName(section));
	}

	@Override
	public String getSectionTextAfterRename(Section<? extends RenamableTerm> section, String oldValue, String replacement) {
		return replacement;
	}

}
