/*
 * Copyright (C) 2011 University Wuerzburg, Computer Science VI
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
package de.d3web.we.flow;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import de.knowwe.core.user.UserContext;


/**
 * 
 * @author Reinhard Hatko
 * @created 07.08.2011
 */
public class DiaFluxTraceHighlight implements DiaFluxDisplayEnhancement {

	public static final String[] SCRIPTS = new String[] { "cc/flow/trace.js" };
	public static final String[] CSSS = new String[] { "cc/flow/trace.css" };
	
	public static final String HIGHLIGHT_KEY = "FlowHighlighting";
	public static final String TRACE_HIGHLIGHT = "trace";
	public static final String NO_HIGHLIGHT = "none";

	@Override
	public boolean activate(UserContext user, String scope) {
		return checkForHighlight(user, TRACE_HIGHLIGHT);

	}

	/**
	 * 
	 * @created 10.10.2011
	 * @param user
	 * @return
	 */
	public static boolean checkForHighlight(UserContext user, String value) {
		HttpServletRequest request = user.getRequest();
		String highlight = user.getParameters().get("highlight");
		// can be null when booting KnowWE
		if (request != null) {
			HttpSession httpSession = request.getSession();
			// if highlight is null, use highlight from session
			if (highlight == null) {
				String temp = (String) httpSession.getAttribute(HIGHLIGHT_KEY);
				if (temp == null) return false;
				else return temp.equalsIgnoreCase(value);
			}
			else if (highlight.equalsIgnoreCase(value)) {
				// save in session
				httpSession.setAttribute(HIGHLIGHT_KEY, value);
				return true;
			}
			else if (highlight.equalsIgnoreCase(NO_HIGHLIGHT)) {
				// save in session
				httpSession.setAttribute(HIGHLIGHT_KEY, NO_HIGHLIGHT);
				return false;
			}
			else return false;

		}
		else return false;
	}

	@Override
	public String[] getScripts() {
		return SCRIPTS;
	}

	@Override
	public String[] getStylesheets() {
		return CSSS;
	}

}
