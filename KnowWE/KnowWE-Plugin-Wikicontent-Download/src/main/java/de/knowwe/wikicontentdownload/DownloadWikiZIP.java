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
package de.knowwe.wikicontentdownload;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import de.d3web.utils.Files;
import de.d3web.utils.Streams;
import de.knowwe.core.ArticleManager;
import de.knowwe.core.Environment;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.Article;
import de.knowwe.fingerprint.Fingerprint;
import de.knowwe.jspwiki.JSPWikiConnector;
import de.knowwe.jspwiki.WikiFileProviderUtils;

/**
 * 
 * @author Johanna Latt
 * @created 16.04.2012
 */
public class DownloadWikiZIP extends AbstractAction {

	public static final String FINGERPRINT_ENTRY_PREFIX = "fingerprint/";

	public static final String PARAM_FILENAME = "filename";
	public static final String PARAM_FINGERPRINT = "fingerprint";
	public static final String PARAM_VERSIONS = "versions";

	@Override
	public void execute(UserActionContext context) throws IOException {

		if (!context.userIsAdmin()) {
			context.sendError(HttpServletResponse.SC_FORBIDDEN,
					"Administration access required to dowload wiki content.");
			return;
		}

		JSPWikiConnector con = (JSPWikiConnector) Environment.getInstance().getWikiConnector();
		File wikiFolder = new File(con.getWikiProperty("var.basedir"));

		String filename = wikiFolder.getName() + ".zip";
		context.setContentType("application/x-bin");
		context.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");

		boolean fingerprint = Boolean.valueOf(context.getParameter(PARAM_FINGERPRINT, "false"));
		boolean versions = Boolean.valueOf(context.getParameter(PARAM_VERSIONS, "false"));

		OutputStream outs = context.getOutputStream();
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(outs));
		try {
			zipDir(wikiFolder, zos, context, versions);
			if (fingerprint) zipFingerprint(zos, context);
			zos.close();
		}
		catch (Exception ioe) {
			ioe.printStackTrace();
			context.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, String.valueOf(ioe));
		}
		outs.flush();
		outs.close();
	}

	private void zipFingerprint(ZipOutputStream zos, UserActionContext context) throws IOException {
		File tempDir = Files.createTempDir();
		try {
			ArticleManager manager = Environment.getInstance().getDefaultArticleManager(
					Environment.DEFAULT_WEB);
			Collection<Article> articles = new LinkedList<Article>();
			for (Article article : manager.getArticles()) {
				if (checkRights(article, context)) articles.add(article);
			}
			Fingerprint.createFingerprint(articles, tempDir);
			File[] files = tempDir.listFiles();
			for (File file : files) {
				String relativePath = FINGERPRINT_ENTRY_PREFIX + file.getName();
				addZipEntry(file, relativePath, zos);
			}
		}
		finally {
			Files.recursiveDelete(tempDir);
		}
	}

	/**
	 * Zips the files in the given directory and writes the resulting zip-File
	 * to the ZipOutputStream.
	 * 
	 * @created 21.04.2012
	 * @param wikiRootFolder the folder to be zipped
	 * @param zos
	 */
	private void zipDir(File wikiRootFolder, ZipOutputStream zos, UserActionContext context, boolean includeOld) throws IOException {
		zipDir(wikiRootFolder, wikiRootFolder, zos, context, 0, includeOld);
	}

	private void zipDir(File wikiRootFolder, File file, ZipOutputStream zos, UserActionContext context, int level, boolean includeOld) throws IOException {

		// ignore all files if they belong to an article
		// we have no read access for
		Article article = WikiFileProviderUtils.getArticle(
				context.getWeb(), wikiRootFolder, file);
		if (article != null && !checkRights(article, context)) {
			return;
		}

		if (file.isFile()) {
			// relativize the savepath of the file against the savepath
			// of the parentfolder of the actual wiki-folder
			String relativePath = wikiRootFolder.getParentFile().toURI().relativize(file.toURI()).getPath();
			addZipEntry(file, relativePath, zos);
		}
		else {
			for (File child : file.listFiles()) {
				if (isHidden(child)) continue;
				if (!includeOld && level == 0 && child.getName().equals("OLD")) continue;
				zipDir(wikiRootFolder, child, zos, context, level + 1, includeOld);
			}
		}
	}

	private void addZipEntry(File file, String relativePath, ZipOutputStream zos) throws IOException {
		// if we reached here, the File object wiki was not
		// a directory
		FileInputStream fis = new FileInputStream(file);
		// create a new zip entry
		ZipEntry zip = new ZipEntry(relativePath);
		// place the zip entry in the ZipOutputStream object
		zos.putNextEntry(zip);
		// write the content of the file to the ZipOutputStream
		Streams.stream(fis, zos);
		// close the Stream
		fis.close();
	}

	private boolean checkRights(Article article, UserActionContext context) {
		JSPWikiConnector con = (JSPWikiConnector) Environment.getInstance().getWikiConnector();
		return con.userCanViewArticle(article.getTitle(), context.getRequest());
	}

	/**
	 * Returns if the file matches the regular expression
	 * "\.[\p{L}\d]*" (like ".svn") and should therefore not be zipped in the
	 * zipDir method.
	 * 
	 * @created 29.04.2012
	 * @param file the file to be checked
	 * @return if the file is a hidden file
	 */
	private boolean isHidden(File file) {
		return file.getName().matches("\\.[\\p{L}\\d]*");
	}
}
