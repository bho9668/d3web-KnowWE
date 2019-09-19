/*
 * Copyright (C) 2019 denkbares GmbH, Germany
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

package org.apache.wiki.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.search.QueryItem;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.jetbrains.annotations.NotNull;

import com.denkbares.utils.Log;

import static org.apache.wiki.providers.GitVersioningUtils.addUserInfo;

/**
 * @author Josua Nürnberger
 * @created 2019-01-02
 */
public class GitVersioningFileProvider extends AbstractFileProvider {

	static final String JSPWIKI_GIT_VERSIONING_FILE_PROVIDER_REMOTE_GIT = "jspwiki.gitVersioningFileProvider.remoteGit";
	protected Repository repository;
	private static final String GIT_DIR = ".git";
	private static final String JSPWIKI_FILESYSTEMPROVIDER_PAGEDIR = "jspwiki.fileSystemProvider.pageDir";
	private static final Logger log = Logger.getLogger(GitVersioningFileProvider.class);
	private String filesystemPath;
	private final ReadWriteLock pushLock = new ReentrantReadWriteLock();
	private final ReentrantLock commitLock = new ReentrantLock();

	private AtomicLong commitCount;

	Map<String, Set<String>> openCommits = new ConcurrentHashMap<>();
	private final Set<String> refreshCacheList = new HashSet<>();

	public boolean isRemoteRepo() {
		return this.remoteRepo;
	}

	private boolean remoteRepo = false;

	/**
	 * {@inheritDoc}
	 *
	 * @throws IOException
	 */
	@Override
	public void initialize(final WikiEngine engine, final Properties properties) throws NoRequiredPropertyException, IOException {
		this.commitCount = new AtomicLong();
		super.initialize(engine, properties);
		this.filesystemPath = TextUtil.getCanonicalFilePathProperty(properties, JSPWIKI_FILESYSTEMPROVIDER_PAGEDIR,
				System.getProperty("user.home") + File.separator + "jspwiki-files");
		final File pageDir = new File(this.filesystemPath);
		final File gitDir = new File(pageDir.getAbsolutePath() + File.separator + GIT_DIR);

		if (!RepositoryCache.FileKey.isGitRepository(gitDir, FS.DETECTED)) {
			final String remoteURL = TextUtil.getStringProperty(properties, JSPWIKI_GIT_VERSIONING_FILE_PROVIDER_REMOTE_GIT, "");
			if (!"".equals(remoteURL)) {
				try {
					final Git git = Git.cloneRepository()
							.setURI(remoteURL)
							.setDirectory(pageDir)
							.setGitDir(gitDir)
							.setBare(false)
							.call();
					this.repository = git.getRepository();
				}
				catch (final GitAPIException e) {
					throw new IOException(e);
				}
			}
			else {
				this.repository = FileRepositoryBuilder.create(gitDir);
				this.repository.create();
			}
		}
		else {
			this.repository = new FileRepositoryBuilder()
					.setGitDir(gitDir)
					.build();
		}
		final Set<String> remoteNames = this.repository.getRemoteNames();
		if (!remoteNames.isEmpty()) {
			this.remoteRepo = true;
		}

		final Git git = new Git(this.repository);
		try {
			log.info("Beginn Git gc");
			final StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			final Properties gcRes = git.gc().setAggressive(true).call();
			stopWatch.stop();
			log.info("Init gc took " + stopWatch);
			for (final Map.Entry<Object, Object> entry : gcRes.entrySet()) {
				log.info("Git gc result: " + entry.getKey() + " " + entry.getValue());
			}
		}
		catch (final GitAPIException e) {
			log.error("Git GC not successful: " + e.getMessage());
		}
		this.repository.autoGC(new TextProgressMonitor());
	}

	public Repository getRepository() {
		return this.repository;
	}

	@Override
	public String getProviderInfo() {
		return GitVersioningFileProvider.class.getSimpleName();
	}

	@Override
	public void putPageText(final WikiPage page, final String text) throws ProviderException {
		try {
			canWriteFileLock();
			final File changedFile = findPage(page.getName());
			final boolean addFile = !changedFile.exists();

			super.putPageText(page, text);
			final boolean isChanged = false;
			final Git git = new Git(this.repository);
			try {
				if (addFile) {
					git.add().addFilepattern(changedFile.getName()).call();
				}

				if (this.openCommits.containsKey(page.getAuthor())) {
					this.openCommits.get(page.getAuthor()).add(changedFile.getName());
				}
				else {
					final CommitCommand commit = git
							.commit()
							.setOnly(changedFile.getName());
					if (page.getAttributes().containsKey(WikiPage.CHANGENOTE)) {
						commit.setMessage((String) page.getAttribute(WikiPage.CHANGENOTE));
					}
					else if (addFile) {
						commit.setMessage("Added page");
					}
					else {
						commit.setMessage("-");
					}
					addUserInfo(this.m_engine, page.getAuthor(), commit);
					commit.setAllowEmpty(true);
					try {
						commitLock();
						final RevCommit revCommit = commit.call();
						WikiEventManager.fireEvent(this, new GitVersioningWikiEvent(this, GitVersioningWikiEvent.DELETE,
								page.getAuthor(),
								page.getName(),
								revCommit.getId().getName()));
						periodicalGitGC(git);
					}
					finally {
						commitUnlock();
					}
				}
			}
			catch (final GitAPIException e) {
				log.error(e.getMessage(), e);
				throw new ProviderException("File " + page.getName() + " could not be committed to git");
			}
		}
		finally {
			writeFileUnlock();
		}
	}

	void periodicalGitGC(final Git git) {
		final long l = this.commitCount.incrementAndGet();
		if (l % 2000 == 0) {
			doGC(git, true);
		}
		else if (l % 500 == 0) {
			doGC(git, false);
		}
		else if (l % 100 == 0) {
			this.repository.autoGC(new TextProgressMonitor());
		}
	}

	private void doGC(final Git git, final boolean aggressive) {
		final StopWatch stopwatch = new StopWatch();
		if (log.isDebugEnabled()) {
			stopwatch.start();
		}
		try {
			git.gc().setAggressive(aggressive).call();
		}
		catch (final GitAPIException e) {
			log.warn("Git gc not successful: " + e.getMessage());
		}
		if (log.isDebugEnabled()) {
			stopwatch.stop();
			log.debug("gc took " + stopwatch);
		}
	}

	@Override
	public boolean pageExists(final String page) {
		return super.pageExists(page);
	}

	@Override
	public boolean pageExists(final String page, final int version) {
		try {
			canWriteFileLock();
			if (pageExists(page)) {
				try {
					if (version == WikiPageProvider.LATEST_VERSION) {
						return true;
					}
					else {
						final List<WikiPage> versionHistory = getVersionHistory(page);
						return (version > 0 && version <= versionHistory.size());
					}
				}
				catch (final ProviderException e) {
					return false;
				}
			}
			else {
				return false;
			}
		}
		finally {
			writeFileUnlock();
		}
	}

	@Override
	public Collection findPages(final QueryItem[] query) {
		return super.findPages(query);
	}

	@Override
	public WikiPage getPageInfo(final String pageName, final int version) throws ProviderException {
		try {
			canWriteFileLock();
			if (pageExists(pageName)) {
				// is necessary to get the right version of the current file
				final List<WikiPage> versionHistory = getVersionHistory(pageName);
				// this first block is only needed, if a file was renamed in a larger commit transaction
				// should never be called in normal JSPWiki work
				if (versionHistory.isEmpty() && version == LATEST_VERSION) {
					final WikiPage page = new WikiPage(this.m_engine, pageName);
					page.setVersion(LATEST_VERSION);
					final File file = findPage(pageName);
					page.setSize(file.length());
					page.setLastModified(new Date(file.lastModified()));
					this.refreshCacheList.add(pageName);
					Log.info("File not in repo but getPageInfo " + pageName);
					return page;
				}
				else if (version == WikiPageProvider.LATEST_VERSION) {
					return versionHistory.get(versionHistory.size() - 1);
				}
				else if (version > 0 && version <= versionHistory.size()) {
					return versionHistory.get(version - 1);
				}
				else {
					throw new ProviderException("Version " + version + " of page " + pageName + " does not exist");
				}
			}
			else {
				return null;
			}
		}
		finally {
			writeFileUnlock();
		}
	}

	@Override
	public Collection getAllPages() throws ProviderException {
		try {
			canWriteFileLock();
			log.debug("Getting all pages...");

			final Map<String, WikiPage> resultingPages = new HashMap<>();

			final File wikipagedir = new File(this.filesystemPath);
			final File[] wikipages = wikipagedir.listFiles(new WikiFileFilter());

			if (wikipages == null) {
				log.error("Wikipages directory '" + this.filesystemPath + "' does not exist! Please check " + PROP_PAGEDIR + " in jspwiki.properties.");
				throw new InternalWikiException("Page directory does not exist");
			}
			final Map<String, File> files = new HashMap<>();
			for (final File file : wikipages) {
				files.put(file.getName(), file);
			}
			try {
				final ObjectReader objectReader = this.repository.newObjectReader();
				final CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
				final CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
				final DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
				diffFormatter.setRepository(this.repository);
				final ObjectId ref = this.repository.resolve(Constants.HEAD);
				final RevWalk revWalk = new RevWalk(this.repository);
				revWalk.markStart(revWalk.lookupCommit(ref));
				RevCommit commit;
				while ((commit = revWalk.next()) != null) {
					final RevCommit[] parents = commit.getParents();
					if (parents.length > 0) {
						commit.getTree();
						oldTreeParser.reset(objectReader, commit.getParent(0)
								.getTree());
						newTreeParser.reset(objectReader, commit.getTree());
						final List<DiffEntry> diffs = diffFormatter.scan(oldTreeParser, newTreeParser);
						for (final DiffEntry diff : diffs) {
							String path = null;
							if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY) {
								path = diff.getOldPath();
							}
							else if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
								path = diff.getNewPath();
							}
							if (path != null) {
								mapCommit(resultingPages, files, commit, path);
							}
						}
					}
					else {
						final TreeWalk tw = new TreeWalk(this.repository);
						tw.reset(commit.getTree());
						tw.setRecursive(false);
						while (tw.next()) {
							mapCommit(resultingPages, files, commit, tw.getPathString());
						}
					}
				}
			}
			catch (final IOException e) {
				Log.severe(e.getMessage(), e);
				throw new ProviderException("Can't load all pages from repository: " + e.getMessage(), e);
			}
			return resultingPages.values();
		}
		finally {
			writeFileUnlock();
		}
	}

	private void mapCommit(final Map<String, WikiPage> resultingPages, final Map<String, File> files, final RevCommit commit, final String path) {
		if (files.containsKey(path)) {
			if (resultingPages.containsKey(path)) {
				final WikiPage wikiPage = resultingPages.get(path);
				wikiPage.setVersion(wikiPage.getVersion() + 1);
			}
			else {
				final WikiPage wikiPage = getWikiPage(commit.getFullMessage(),
						commit.getCommitterIdent().getName(),
						new Date(1000L * commit.getCommitTime()),
						path);
				wikiPage.setVersion(1);
				wikiPage.setSize(files.get(path).length());
				resultingPages.put(path, wikiPage);
			}
		}
	}

	@Override
	public Collection getAllChangedSince(final Date date) {
		try {
			canWriteFileLock();
			final Iterable<RevCommit> commits = GitVersioningUtils.getRevCommitsSince(date, this.repository);
			final List<WikiPage> pages = new ArrayList<>();

			try {
				ObjectId oldCommit = null;
				ObjectId newCommit;

				for (final RevCommit commit : GitVersioningUtils.reverseToList(commits)) {
					final String fullMessage = commit.getFullMessage();
					final String author = commit.getCommitterIdent().getName();
					final Date modified = new Date(1000L * commit.getCommitTime());

					if (oldCommit != null) {
						newCommit = commit.getTree();
						final List<DiffEntry> diffs = GitVersioningUtils.getDiffEntries(oldCommit, newCommit, this.repository);
						for (final DiffEntry diff : diffs) {
							String path = diff.getOldPath();
							if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
								path = diff.getNewPath();
							}
							if (path.endsWith(FILE_EXT) && !path.contains("/")) {
								final WikiPage page = getWikiPage(fullMessage, author, modified, path);
								pages.add(page);
							}
						}
					}
					else {
						try (final TreeWalk treeWalk = new TreeWalk(this.repository)) {
							treeWalk.reset(commit.getTree());
							treeWalk.setRecursive(true);
							treeWalk.setFilter(TreeFilter.ANY_DIFF);
							while (treeWalk.next()) {
								final String path = treeWalk.getPathString();
								if (path.endsWith(FILE_EXT) && !path.contains("/")) {
									final WikiPage page = getWikiPage(fullMessage, author, modified, path);
									pages.add(page);
								}
							}
						}
					}
					oldCommit = commit.getTree();
				}
			}
			catch (final IOException e) {
				log.error(e.getMessage(), e);
			}
			return pages;
		}
		finally {
			writeFileUnlock();
		}
	}

	@NotNull
	private WikiPage getWikiPage(final String fullMessage, final String author, final Date modified, final String path) {
		final int cutpoint = path.lastIndexOf(FILE_EXT);
		if (cutpoint == -1) {
			log.error("wrong page name " + path);
		}
		final WikiPage page = new WikiPage(this.m_engine, unmangleName(path.substring(0, cutpoint)));
		page.setAttribute(WikiPage.CHANGENOTE, fullMessage);
		page.setAuthor(author);
		page.setLastModified(modified);
		return page;
	}

	@Override
	public int getPageCount() {
		return super.getPageCount();
	}

	@Override
	public List<WikiPage> getVersionHistory(final String pageName) throws ProviderException {
		try {
			canWriteFileLock();
			final File page = findPage(pageName);
			final List<WikiPage> pageVersions = new ArrayList<>();
			if (page.exists()) {
				try {
					final Git git = new Git(this.repository);
					final List<RevCommit> revCommits = GitVersioningUtils.reverseToList(getRevCommits(page, git));
					int versionNr = 1;
					for (final RevCommit revCommit : revCommits) {
						final WikiPage version = getWikiPage(pageName, versionNr, revCommit);
						pageVersions.add(version);
						versionNr++;
					}
				}
				catch (final IOException | GitAPIException e) {
					log.error(e.getMessage(), e);
					throw new ProviderException("Can't get version history for page " + pageName + ": " + e.getMessage());
				}
			}
			return pageVersions;
		}
		finally {
			writeFileUnlock();
		}
	}

	@NotNull
	private WikiPage getWikiPage(final String pageName, final int versionNr, final RevCommit revCommit) {
		final WikiPage version = new WikiPage(this.m_engine, pageName);
		version.setAuthor(revCommit.getCommitterIdent().getName());
		version.setLastModified(new Date(1000L * revCommit.getCommitTime()));
		version.setVersion(versionNr);
		version.setAttribute(WikiPage.CHANGENOTE, revCommit.getFullMessage());
		return version;
	}

	@Override
	public String getPageText(final String pageName, final int version) throws ProviderException {
		try {
			canWriteFileLock();
			final File page = findPage(pageName);
			if (page.exists()) {
				try {
					if (version == LATEST_VERSION) {
						try (final FileInputStream fileInputStream = new FileInputStream(page)) {
							return FileUtil.readContents(fileInputStream, this.m_encoding);
						}
					}
					else {
						final Git git = new Git(this.repository);
						final List<RevCommit> revCommits = GitVersioningUtils.reverseToList(getRevCommits(page, git));
						if ((version > 0 && version <= revCommits.size())) {
							final RevCommit revCommit = revCommits.get(version - 1);
							final TreeWalk treeWalkDir = new TreeWalk(this.repository);
							treeWalkDir.reset(revCommit.getTree());
							treeWalkDir.setFilter(PathFilter.create(page.getName()));
							treeWalkDir.setRecursive(false);
							//here we have our file directly
							if (treeWalkDir.next()) {
								final ObjectId fileId = treeWalkDir.getObjectId(0);
								final ObjectLoader loader = this.repository.open(fileId);
								return new String(loader.getBytes(), this.m_encoding);
							}
							else {
								throw new ProviderException("Can't load Git object for " + pageName + " version " + version);
							}
						}
					}
				}
				catch (final IOException | GitAPIException e) {
					log.error(e.getMessage(), e);
				}
			}
			else {
				log.info("New file " + pageName);
			}
			return null;
		}
		finally {
			writeFileUnlock();
		}
	}

	private Iterable<RevCommit> getRevCommits(final File page, final Git git) throws GitAPIException, IOException {
		return git
				.log()
				.add(git.getRepository().resolve(Constants.HEAD))
				.addPath(page.getName())
				.call();
	}

	@Override
	public void deleteVersion(final WikiPage pageName, final int version) {
		// Can't delete version from git
	}

	@Override
	public void deletePage(final WikiPage page) throws ProviderException {
		try {
			canWriteFileLock();
			final File file = findPage(page.getName());
			file.delete();
			try {
				final Git git = new Git(this.repository);
				git.rm().addFilepattern(file.getName()).call();
				if (this.openCommits.containsKey(page.getAuthor())) {
					this.openCommits.get(page.getAuthor()).add(file.getName());
				}
				else {
					final CommitCommand commitCommand = git.commit()
							.setOnly(file.getName())
							.setMessage("removed page");
					addUserInfo(this.m_engine, page.getAuthor(), commitCommand);
					try {
						commitLock();
						final RevCommit revCommit = commitCommand.call();
						WikiEventManager.fireEvent(this, new GitVersioningWikiEvent(this, GitVersioningWikiEvent.DELETE,
								page.getAuthor(),
								page.getName(),
								revCommit.getId().getName()));
						periodicalGitGC(git);
					}
					finally {
						commitUnlock();
					}
				}
			}
			catch (final GitAPIException e) {
				log.error(e.getMessage(), e);
				throw new ProviderException("Can't delete page " + page + ": " + e.getMessage());
			}
		}
		finally {
			writeFileUnlock();
		}
	}

	@Override
	public void movePage(final WikiPage from, final String to) throws ProviderException {
		try {
			canWriteFileLock();
			final File fromFile = findPage(from.getName());
			final File toFile = findPage(to);
			try {
				Files.move(fromFile.toPath(), toFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
				final Git git = new Git(this.repository);
				git.add().addFilepattern(toFile.getName()).call();
				git.rm().addFilepattern(fromFile.getName()).call();
				if (this.openCommits.containsKey(from.getAuthor())) {
					this.openCommits.get(from.getAuthor()).add(fromFile.getName());
					this.openCommits.get(from.getAuthor()).add(toFile.getName());
				}
				else {
					final CommitCommand commitCommand = git.commit()
							.setOnly(toFile.getName())
							.setOnly(fromFile.getName())
							.setMessage("renamed page " + from + " to " + to);
					addUserInfo(this.m_engine, from.getAuthor(), commitCommand);
					try {
						commitLock();
						final RevCommit revCommit = commitCommand.call();
						WikiEventManager.fireEvent(this, new GitVersioningWikiEvent(this, GitVersioningWikiEvent.MOVED,
								from.getAuthor(),
								to,
								revCommit.getId().getName()));
						periodicalGitGC(git);
					}
					finally {
						commitUnlock();
					}
				}
			}
			catch (final IOException | GitAPIException e) {
				log.error(e.getMessage(), e);
				throw new ProviderException("Can't move page from " + from + " to " + to + ": " + e.getMessage());
			}
		}
		finally {
			writeFileUnlock();
		}
	}

	public void openCommit(final String user) {
		if (!this.openCommits.containsKey(user)) {
			this.openCommits.put(user, Collections.synchronizedSortedSet(new TreeSet<>()));
		}
	}

	public void commit(final String user, final String commitMsg) {
		Log.info("start commit");
		try {
			canWriteFileLock();
			commitLock();
			final Git git = new Git(this.repository);
			final CommitCommand commitCommand = git.commit().setMessage(commitMsg);
			addUserInfo(this.m_engine, user, commitCommand);
			if (this.openCommits.containsKey(user)) {
				final Set<String> paths = this.openCommits.get(user);
				for (final String path : paths) {
					commitCommand.setOnly(path);
				}
				try {
					commitCommand.setAllowEmpty(true);
					final RevCommit revCommit = commitCommand.call();
					WikiEventManager.fireEvent(this, new GitVersioningWikiEvent(this, GitVersioningWikiEvent.MOVED,
							user,
							this.openCommits.get(user),
							revCommit.getId().getName()));
					this.openCommits.remove(user);
					final PageManager pm = this.m_engine.getPageManager();
					Log.info("Start refresh");
					for (final String path : this.refreshCacheList) {
						// decide whether page or attachment
						refreshCache(pm, path);
					}
					this.refreshCacheList.clear();
				}
				catch (final GitAPIException e) {
					Log.severe(e.getMessage(), e);
				}
			}
		}
		finally {
			commitUnlock();
			writeFileUnlock();
		}
	}

	public void rollback(final String user) {
		try {
			canWriteFileLock();

			final Git git = new Git(this.repository);
			final ResetCommand reset = git.reset();
			final CleanCommand clean = git.clean();
			final CheckoutCommand checkout = git.checkout();
			final Set<String> paths = this.openCommits.get(user);
			clean.setPaths(paths);
			for (final String path : paths) {
				reset.addPath(path);
				checkout.addPath(path);
			}
			clean.setCleanDirectories(true);
			try {
				final PageManager pm = this.m_engine.getPageManager();
				// this could be done, because we only remove the page from the cache and the method of
				// GitVersioningFileProvider does nothing here
				// But we have to inform KnowWE also and Lucene
				for (final String path : paths) {
					// decide whether page or attachment
					refreshCache(pm, unmangleName(path));
				}
				reset.call();
				clean.call();
				final Status status = git.status().call();

				checkout.call();
				this.openCommits.remove(user);
			}
			catch (final GitAPIException e) {
				Log.severe(e.getMessage(), e);
			}
		}
		finally {
			writeFileUnlock();
		}
	}

	void refreshCache(final PageManager pm, final String pageName) {
		final WikiPage page = new WikiPage(this.m_engine, pageName);
		page.setVersion(WikiProvider.LATEST_VERSION);
		try {
			// this could be done, because we only remove the page from the cache and the method of
			// GitVersioningFileProvider does nothing here
			// But we have to inform KnowWE also and Lucene
			pm.deleteVersion(page);
		}
		catch (final ProviderException e) {
			Log.severe(e.getMessage(), e);
		}
	}

	public void commitLock() {
		//noinspection LockAcquiredButNotSafelyReleased
		this.commitLock.lock();
	}

	public void commitUnlock() {
		this.commitLock.unlock();
	}

	public void pushLock() {
		//noinspection LockAcquiredButNotSafelyReleased
		this.pushLock.writeLock().lock();
	}

	public void pushUnlock() {
		this.pushLock.writeLock().unlock();
	}

	public void canWriteFileLock() {
		//noinspection LockAcquiredButNotSafelyReleased
		this.pushLock.readLock().lock();
	}

	public void writeFileUnlock() {
		this.pushLock.readLock().unlock();
	}

	public void shutdown() {
		this.repository.close();
	}
}
