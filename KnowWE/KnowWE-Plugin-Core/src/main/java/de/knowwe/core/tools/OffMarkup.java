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

package de.knowwe.core.tools;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupRenderer;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

/**
 * Markup for another deactivated markup section.
 * Sectionizes markups like %%Off:Rule...
 *
 * Created by Veronika Sehne on 11.06.14.
 */
public class OffMarkup extends DefaultMarkupType {

	private static final DefaultMarkup MARKUP;

	private static final String MARKUP_NAME = "Off:\\w+";

	static {
		MARKUP = new DefaultMarkup(MARKUP_NAME);
		MARKUP.addRegexAnnotation(".*", false);
	}

	public OffMarkup() {
		super(MARKUP);
		this.setRenderer(new DefaultMarkupRenderer() {

			@Override
			protected String getTitleName(Section<?> section, UserContext user) {

				String text = section.getText();
				return text.substring(0, text.indexOf("\n")).replaceAll("^%%Off:", "");
			}

			@Override
			protected void renderContents(Section<?> section, UserContext user, RenderResult result) {
				result.appendHtmlTag("div", "style", "color: grey");
				RenderResult temp = new RenderResult(result);
				super.renderContents(section, user, temp);
				result.appendJSPWikiMarkup(temp);
				result.appendHtmlTag("/div");
			}
		});
	}

}
