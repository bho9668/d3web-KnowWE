/*
 * Copyright (C) ${year} denkbares GmbH, Germany
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

package de.knowwe.core.compile.packaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.d3web.strings.Identifier;
import de.knowwe.core.compile.Compiler;
import de.knowwe.core.compile.Compilers;
import de.knowwe.core.compile.PackageCompiler;
import de.knowwe.core.compile.PackageRegistrationCompiler;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.core.user.UserContext;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupRenderer;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.tools.ToolMenuDecoratingRenderer;

/**
 * Renders a {@link DefaultMarkupType} section the does package compilation.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 13.10.2010
 */
public class DefaultMarkupPackageCompileTypeRenderer extends DefaultMarkupRenderer {

	public DefaultMarkupPackageCompileTypeRenderer(String icon) {
		super(icon);
	}

	public DefaultMarkupPackageCompileTypeRenderer() {
		this("KnowWEExtension/d3web/icon/knowledgebase24.png");
	}

	@Override
	protected void renderContents(Section<?> section, UserContext user, RenderResult string) {
		renderPackages(section, user, string);
	}

	private void renderPackages(Section<?> section, UserContext user, RenderResult string) {
		// render used packages and their erroneous pages
		string.appendHtml("<div style='padding-top:1em;'>");
		// string.append(KnowWEUtils.maskHTML("<hr>\n"));
		Section<DefaultMarkupPackageCompileType> compileSection = Sections.findSuccessor(section,
				DefaultMarkupPackageCompileType.class);
		String[] packagesToCompile = compileSection.get().getPackagesToCompile(
				compileSection);

		for (String packageName : packagesToCompile) {
			renderPackage(compileSection, section, packageName, string, user);
		}

		string.appendHtml("</div>");
	}

	private void renderPackage(Section<? extends PackageCompileType> compileSection, Section<?> section, String packageName, RenderResult string, UserContext user) {

		PackageManager packageManager =
				KnowWEUtils.getPackageManager(section);
		Collection<Section<?>> sectionsOfPackage = packageManager.getSectionsOfPackage(packageName);

		Collection<Message> kdomErrors = new LinkedList<Message>();
		Collection<Message> kdomWarnings = new LinkedList<Message>();

		Set<Article> errorArticles = new HashSet<Article>();
		Set<Article> warningArticles = new HashSet<Article>();

		for (Section<?> sectionOfPackage : sectionsOfPackage) {
			Collection<PackageCompiler> packageCompilers = compileSection.get().getPackageCompilers(
					compileSection);
			Collection<Message> errors = new ArrayList<Message>();
			Collection<Message> warnings = new ArrayList<Message>();
			Map<Compiler, Collection<Message>> allmsgs = Messages.getMessagesMapFromSubtree(
					sectionOfPackage, Message.Type.ERROR,
					Message.Type.WARNING);
			for (PackageCompiler packageCompiler : packageCompilers) {
				Collection<Message> compileMessages = allmsgs.get(packageCompiler);
				if (compileMessages == null) continue;
				errors.addAll(Messages.getErrors(compileMessages));
				warnings.addAll(Messages.getWarnings(compileMessages));
			}
			if (errors != null && errors.size() > 0) {
				kdomErrors.addAll(errors);
				errorArticles.add(sectionOfPackage.getArticle());
			}
			if (warnings != null && warnings.size() > 0) {
				kdomWarnings.addAll(warnings);
				warningArticles.add(sectionOfPackage.getArticle());
			}
		}

		int errorsCount = kdomErrors.size();
		int warningsCount = kdomWarnings.size();
		boolean hasErrors = errorsCount > 0;
		boolean hasWarnings = warningsCount > 0;

		String icon;
		if (hasErrors) {
			icon = "KnowWEExtension/d3web/icon/uses_error16.gif";
		}
		else if (hasWarnings) {
			icon = "KnowWEExtension/d3web/icon/uses_warn16.gif";
		}
		else {
			icon = "KnowWEExtension/images/package_obj.gif";
		}

		string.appendHtml("<img style='position:relative; top:2px;' class='packageOpacity' src='"
				+ icon
				+ "' />");
		string.append(" ");
		RenderResult subString = new RenderResult(string);
		subString.appendHtml("<span style='color:rgb(121,79, 64);'>");
		subString.append(packageName);
		subString.appendHtml("</span>");
		PackageRegistrationCompiler packageCompiler = Compilers.getCompiler(
				section.getArticleManager(), PackageRegistrationCompiler.class);
		Collection<Section<?>> termDefiningSections = packageCompiler.getTerminologyManager()
				.getTermDefiningSections(new Identifier(packageName));
		for (Section<?> termDefiningSection : termDefiningSections) {
			Section<PackageTerm> packageTermSection = Sections.findSuccessor(termDefiningSection,
					PackageTerm.class);
			if (packageTermSection != null) {
				ToolMenuDecoratingRenderer.renderToolMenuDecorator(subString.toStringRaw(),
						packageTermSection.getID(), true, string);
				break;
			}
		}
		if (hasErrors) {
			string.append(" (").append(errorsCount).append(" errors in ");
			string.append(errorArticles.size()).append(
					" article" + (errorArticles.size() > 1 ? "s" : "") + ")");
			renderDefectArticleNames(errorArticles, string);
			// renderDefectArticleNames(kdomErrors, icon, string);
			// renderDefectArticleNames(messagesErrors, icon, string);
		}
		else if (hasWarnings) {
			string.append(" (").append(warningsCount).append(" warnings in ");
			string.append(warningArticles.size()).append(
					" article" + (warningArticles.size() > 1 ? "s" : "") + ")");
			renderDefectArticleNames(warningArticles, string);
			// renderDefectArticleNames(kdomWarnings, icon, string);
			// renderDefectArticleNames(messagesWarnings, icon, string);
		}
		else {
			string.appendHtml("<br/>");
		}

	}

	private void renderDefectArticleNames(Set<Article> articles, RenderResult string) {
		// print all articles out as links (ordered alphabetically, duplicates
		// removed)
		List<String> names = new ArrayList<String>(articles.size());
		for (Article article : articles) {
			names.add(article.getTitle());
		}
		Collections.sort(names);

		string.appendHtml("<ul>");
		for (String name : names) {
			string.appendHtml("<li>");
			string.append("[").append(name).append("]");
			string.append("\n");
		}
		string.appendHtml("</ul>");
	}

}