/*
 * Copyright (C) 2012 University Wuerzburg, Computer Science VI
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
package de.knowwe.core.compile.terminology;

import de.knowwe.core.compile.Priority;
import de.knowwe.core.kdom.parsing.Section;

/**
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 03.02.2012
 */
class TermLogEntry implements Comparable<TermLogEntry> {

	private final Priority priority;
	private final Section<?> section;
	private final Class<?> termClass;
	private final String termIdentifier;

	public TermLogEntry(Priority priority, Section<?> section, Class<?> termClass, String termIdentifier) {
		this.section = section;
		this.termClass = termClass;
		this.termIdentifier = termIdentifier;
		this.priority = priority;
	}

	public Priority getPriority() {
		return priority;
	}

	public Section<?> getSection() {
		return section;
	}

	public Class<?> getTermClass() {
		return termClass;
	}

	public String getTermIdentifier() {
		return termIdentifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((priority == null) ? 0 : priority.hashCode());
		result = prime * result + ((section == null) ? 0 : section.hashCode());
		result = prime * result + ((termClass == null) ? 0 : termClass.hashCode());
		result = prime * result + ((termIdentifier == null) ? 0 : termIdentifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		TermLogEntry other = (TermLogEntry) obj;
		if (priority == null) {
			if (other.priority != null) return false;
		}
		else if (!priority.equals(other.priority)) return false;
		if (section == null) {
			if (other.section != null) return false;
		}
		else if (!section.equals(other.section)) return false;
		if (termClass == null) {
			if (other.termClass != null) return false;
		}
		else if (!termClass.equals(other.termClass)) return false;
		if (termIdentifier == null) {
			if (other.termIdentifier != null) return false;
		}
		else if (!termIdentifier.equals(other.termIdentifier)) return false;
		return true;
	}

	@Override
	public int compareTo(TermLogEntry o) {
		if (this == o) return 0;
		if (o == null) return -1;

		int result = 0;
		if (priority == null) {
			if (o.priority != null) return 1;
		}
		else {
			result = -priority.compareTo(o.priority);
			if (result != 0) return result;
		}

		if (section == null) {
			if (o.section != null) return 1;
		}
		else {
			result = section.compareTo(o.section);
			if (result != 0) return result;
		}

		if (termClass == null) {
			if (o.termClass != null) return 1;
		}
		else {
			result = termClass.getName().compareTo(o.termClass.getName());
			if (result != 0) return result;
		}

		if (termIdentifier == null) {
			if (o.termIdentifier != null) return 1;
		}
		return termIdentifier.compareTo(o.termIdentifier);
	}

}
