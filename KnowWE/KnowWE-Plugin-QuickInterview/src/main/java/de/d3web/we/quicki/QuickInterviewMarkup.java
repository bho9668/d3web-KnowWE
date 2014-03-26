package de.d3web.we.quicki;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.knowwe.core.Environment;
import de.knowwe.core.ResourceLoader;
import de.knowwe.core.compile.packaging.MasterAnnotationWarningHandler;
import de.knowwe.core.compile.packaging.PackageAnnotationNameType;
import de.knowwe.core.compile.packaging.PackageManager;
import de.knowwe.core.compile.packaging.PackageTerm;
import de.knowwe.core.kdom.basicType.PlainText;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.rendering.NothingRenderer;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.user.UserContext;
import de.knowwe.core.wikiConnector.WikiAttachment;
import de.knowwe.core.wikiConnector.WikiConnector;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupRenderer;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

/**
 * 
 * @author Benedikt Kaemmerer
 * @created 28.07.2012
 */

public class QuickInterviewMarkup extends DefaultMarkupType {

	private static DefaultMarkup m = null;

	public static final String MARKUP_NAME = "QuickInterview";

	public static final String UNKNOWN_KEY = "unknown";

	public static final String ABSTRACTIONS_KEY = "abstractions";

	public static final String ANSWERS_KEY = "answers";

	public static final String SAVE_KEY = "save";

	static {
		m = new DefaultMarkup(MARKUP_NAME);
		m.addAnnotation(UNKNOWN_KEY, false, "true", "false");
		m.addAnnotationRenderer(UNKNOWN_KEY, NothingRenderer.getInstance());

		m.addAnnotation(ABSTRACTIONS_KEY, false, "true", "false");
		m.addAnnotationRenderer(ABSTRACTIONS_KEY, NothingRenderer.getInstance());

		m.addAnnotation(ANSWERS_KEY, false);
		m.addAnnotationRenderer(ANSWERS_KEY, NothingRenderer.getInstance());

		m.addAnnotation(SAVE_KEY, false);
		m.addAnnotationRenderer(SAVE_KEY, NothingRenderer.getInstance());

		m.addAnnotation(PackageManager.PACKAGE_ATTRIBUTE_NAME, false);
		m.addAnnotationNameType(PackageManager.PACKAGE_ATTRIBUTE_NAME,
				new PackageAnnotationNameType());
		m.addAnnotationContentType(PackageManager.PACKAGE_ATTRIBUTE_NAME,
				new PackageTerm());

	}

	public QuickInterviewMarkup() {
		this(m);
	}

	public QuickInterviewMarkup(DefaultMarkup markup) {
		super(markup);
		ResourceLoader.getInstance().add("quickiNeutral.css",
				ResourceLoader.RESOURCE_STYLESHEET);
		ResourceLoader.getInstance().add("quicki.js",
				ResourceLoader.RESOURCE_SCRIPT);
		this.setRenderer(new QuickInterviewMarkupRenderer());
		this.addCompileScript(new MasterAnnotationWarningHandler());
	}

	private static class QuickInterviewMarkupRenderer extends DefaultMarkupRenderer {

		@Override
		public void renderContents(Section<?> section, UserContext user, RenderResult string) {
			String unknown = DefaultMarkupType.getAnnotation(section, UNKNOWN_KEY);
			String abstractions = DefaultMarkupType.getAnnotation(section,
					ABSTRACTIONS_KEY);
			String answers = DefaultMarkupType.getAnnotation(section, ANSWERS_KEY);
			Map<String, String> parameters = user.getParameters();
			if (unknown != null) {
				parameters.put(UNKNOWN_KEY, unknown);
			}
			if (abstractions != null) {
				parameters.put(ABSTRACTIONS_KEY, abstractions);
			}
			if (answers != null) {
				parameters.put(ANSWERS_KEY, answers);
			}

			String annotation = DefaultMarkupType.getAnnotation(section,
					QuickInterviewMarkup.SAVE_KEY);

			boolean saveSession = false;
			if (annotation != null && annotation.equalsIgnoreCase("true")) {
				saveSession = true;
			}

			String savehtml = "";
			if (saveSession) {
				savehtml = "<div id=\"sessionsave\"><form name=\"loadsave\"> "
						+
						"<select name=\"savedsessions\"  size=\"1\" width=\"30\"><option>-Load Session-</option>"
						+ getSavedSessions(user)
						+ "</select><input name=\"load\" type=\"button\" value=\"Load\" onclick=\"loadQuicki('"
						+ section.getID()
						+ "')\"/>"
						+
						"<input name=\"name\" type=\"text\" size=\"20\" maxlength=\"30\" />"
						+
						"<input name=\"save\" type=\"button\" value=\"Save\" onclick=\"saveQuicki()\"/></form></div>";

			}
			string.appendHtml(savehtml);

			string.appendHtmlTag("div", "class", "quickinterview", "sectionId", section.getID(),
					"id", "quickinterview_" + section.getID());
			QuickInterviewRenderer.renderInterview(section, user, string);
			string.appendHtmlTag("/div");

			List<Section<?>> subsecs = section.getChildren();
			Section<?> first = subsecs.get(0);
			Section<?> last = subsecs.get(subsecs.size() - 1);
			for (Section<?> subsec : subsecs) {
				if (subsec == first && subsec.get() instanceof PlainText) {
					continue;
				}
				if (subsec == last && subsec.get() instanceof PlainText) {
					continue;
				}
				subsec.get().getRenderer().render(subsec, user, string);
			}
		}

		/**
		 * Finds previously saved QuickInterview Sessions
		 * 
		 * @created 30.11.2012
		 * @return String with html code containing options of .xml files
		 */
		private static String getSavedSessions(UserContext user) {
			WikiConnector wikiConnector = Environment.getInstance()
					.getWikiConnector();
			StringBuilder builder = new StringBuilder();
			try {
				List<WikiAttachment> attachments = wikiConnector
						.getAttachments(user.getTitle());
				for (WikiAttachment wikiAttachment : attachments) {
					String fileName = wikiAttachment.getFileName();

					if (fileName.endsWith("xml")) {
						builder.append("<option value=\"" + fileName + "\">"
								+ fileName + "</option>");
					}
				}

			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return builder.toString();
		}

	}

}
