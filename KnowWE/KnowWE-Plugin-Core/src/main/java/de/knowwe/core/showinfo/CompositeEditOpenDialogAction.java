/*
 * Copyright (C) 2014 denkbares GmbH, Germany
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
package de.knowwe.core.showinfo;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import de.d3web.strings.Identifier;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.objectinfo.ObjectInfoRenderer;

/**
 * @author stefan
 * @created 12.12.2013
 */
public class CompositeEditOpenDialogAction extends AbstractAction {

	@Override
	public void execute(UserActionContext context) throws IOException {
		Identifier identifier = Identifier.fromExternalForm(context.getParameter("termIdentifier"));

		RenderResult header = new RenderResult(context);
		ObjectInfoRenderer.renderHeader(identifier, context, header);

		RenderResult plainText = new RenderResult(context);
		ObjectInfoRenderer.renderPlainTextOccurrences(identifier, context, plainText);

		RenderResult result = new RenderResult(context);
		ObjectInfoRenderer.renderTermDefinitions(identifier, context, result);
		ObjectInfoRenderer.renderTermReferences(identifier, context, result);

		JSONObject response = new JSONObject();

		try {
			response.accumulate("header", header.toString());
			response.accumulate("result", result.toString());
			response.accumulate("plainText", plainText.toString());
			context.setContentType("text/html; charset=UTF-8");
			response.write(context.getWriter());

		}
		catch (JSONException e) {
			throw new IOException(e);
		}

	}

}