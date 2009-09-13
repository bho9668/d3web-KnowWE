/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 *                    Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package de.d3web.we.terminology.local;

import java.util.Collection;

import de.d3web.kernel.domainModel.IDObject;
import de.d3web.kernel.domainModel.KnowledgeBaseManagement;
import de.d3web.kernel.domainModel.QASet;
import de.d3web.kernel.domainModel.qasets.Question;
import de.d3web.we.alignment.AlignmentUtilRepository;

public class D3webLocalSymptomTerminology implements LocalTerminologyAccess<IDObject> {

	private final KnowledgeBaseManagement kbm;
	
	public D3webLocalSymptomTerminology(KnowledgeBaseManagement kbm) {
		super();
		this.kbm = kbm;
	}

	public IDObject getObject(String objectId, String valueId) {
		IDObject result = null;
		result = kbm.findQContainer(objectId);
		if(result == null) {
			result = kbm.findQuestion(objectId);
		}
		if(valueId == null) {
			return result;
		} else {
			if(result instanceof Question) {
				return kbm.findAnswer((Question) result, valueId);
			}
		}
		return result;
	}
	public LocalTerminologyHandler<IDObject, IDObject> getHandler(Collection<Class> include, Collection<Class> exclude) {
		LocalTerminologyHandler result = getHandler();
		result.setFilters(include);
		result.setExclusiveFilters(exclude);
		return result;
	}

	public LocalTerminologyHandler<IDObject, IDObject> getHandler() {
		Object root = kbm.getKnowledgeBase().getRootQASet();
		return AlignmentUtilRepository.getInstance().getLocalTerminogyHandler(root);
	}

	public Class getContext() {
		return QASet.class;
	}
	
}
