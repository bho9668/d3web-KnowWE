/*
 * Copyright (C) 2012 denkbares GmbH
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
package de.d3web.we.ci4ke.dashboard.rendering;

import de.knowwe.core.kdom.rendering.RenderResult;

/**
 * Interface for renderers allowing to render certain objects dependent by their
 * name of string representation. This can for example be used to render links
 * to the objects.
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 24.09.2012
 */
public interface ObjectNameRenderer {

	public static final String PLUGIN_ID = "KnowWE-Plugin-CI4KE";
	public static final String EXTENSION_POINT_ID = "ObjectNameRenderer";

	void render(String objectName, RenderResult result);

}
