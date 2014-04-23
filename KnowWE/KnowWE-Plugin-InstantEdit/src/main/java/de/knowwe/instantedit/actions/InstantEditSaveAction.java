/*
 * Copyright (C) 2011 University Wuerzburg, Computer Science VI
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
package de.knowwe.instantedit.actions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.d3web.utils.Log;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.compile.Compilers;
import de.knowwe.core.kdom.parsing.Sections;

/**
 * Saves the changed Section.
 *
 * @author Stefan Mark
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 15.06.2011
 */
public class InstantEditSaveAction extends AbstractAction {

	@Override
	public void execute(UserActionContext context) throws IOException {

		String id = context.getParameter("KdomNodeId");
		String value = context.getParameter("data");

		if (value.equals("POST\n")) {
			value = "";
		}

		// errors and security are handled inside replaceKDOMNodesSaveAndBuild
		Map<String, String> nodesMap = new HashMap<String, String>();
		nodesMap.put(id, value);
		Sections.replaceSections(context, nodesMap).sendErrors(context);
		try {
			Compilers.getCompilerManager(context.getWeb()).awaitTermination();
		}
		catch (InterruptedException e) {
			Log.warning("Interrupted waiting for compilation", e);
		}
	}
}
