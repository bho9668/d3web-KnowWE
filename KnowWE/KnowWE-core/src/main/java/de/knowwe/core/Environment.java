/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg
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

package de.knowwe.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import de.d3web.plugin.JPFPluginManager;
import de.d3web.plugin.Plugin;
import de.d3web.plugin.PluginManager;
import de.d3web.plugin.Resource;
import de.knowwe.core.append.PageAppendHandler;
import de.knowwe.core.compile.packaging.PackageManager;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.event.EventManager;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.RootType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Sectionizer;
import de.knowwe.core.kdom.parsing.SectionizerModule;
import de.knowwe.core.kdom.rendering.Renderer;
import de.knowwe.core.taghandler.TagHandler;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.core.wikiConnector.WikiConnector;
import de.knowwe.event.InitEvent;
import de.knowwe.knowRep.KnowledgeRepresentationHandler;
import de.knowwe.knowRep.KnowledgeRepresentationManager;
import de.knowwe.plugin.Instantiation;
import de.knowwe.plugin.Plugins;

/**
 * This is the core class of KnowWE. It manages the {@link ArticleManager} and
 * provides methods to access {@link Article}s and other Managers. Further it is
 * connected to the used Wiki-engine, holding an instance of
 * {@link WikiConnector} and allows page saves.
 * 
 * @author Jochen
 */

public class Environment {

	/**
	 * Stores additional renderers if renderers are plugged via the plugin framework
	 * The renderer plugged with highest priority _might_ decided to look up in this list 
	 * and call other renderers
	 */
	private final Map<Type, List<Renderer>> additionalRenderers = new HashMap<Type, List<Renderer>>();

	/**
	 * An article manager for each web. In case of JSPWiki there is only on web
	 * ('default_web')
	 */
	private final Map<String, ArticleManager> articleManagers = new HashMap<String, ArticleManager>();

	/**
	 * A knowledge manager for each web. In case of JSPWiki there is only on web
	 * ('default_web')
	 */
	private final Map<String, KnowledgeRepresentationManager> knowledgeManagers = new HashMap<String, KnowledgeRepresentationManager>();

	/**
	 * A package manager for each web. In case of JSPWiki there is only on web
	 * ('default_web')
	 */
	private final Map<String, PackageManager> packageManagers = new HashMap<String, PackageManager>();

	/**
	 * A terminology handler for each web and article. In case of JSPWiki there
	 * is only on web ('default_web')
	 */
	private final Map<String, Map<String, TerminologyManager>> terminologyManagers = new HashMap<String, Map<String, TerminologyManager>>();

	/**
	 * This is the link to the connected Wiki-engine. Allows saving pages etc.
	 */
	private WikiConnector wikiConnector = null;

	/**
	 * Holding the default tag handlers of KnowWE
	 */
	private final Map<String, TagHandler> tagHandlers = new HashMap<String, TagHandler>();

	/**
	 * The {@link CompilationMode} of KnowWE:
	 */
	private CompilationMode currentCompilationMode = CompilationMode.DEFAULT;

	/**
	 * Hard coded name of the default web
	 */
	public static final String DEFAULT_WEB = "default_web";

	public enum CompilationMode {
		INCREMENTAL, DEFAULT
	}

	/**
	 * Singleton instance
	 */
	private static Environment instance;

	/**
	 * Singleton lazy factory
	 */
	public static synchronized Environment getInstance() {
		if (instance == null) {
			Logger.getLogger(Environment.class.getName()).severe(
					"Environment was not instantiated!");
		}
		return instance;
	}

	public static boolean isInitialized() {
		return instance != null;
	}

	public static void initInstance(WikiConnector wiki) {
		Logger.getLogger(Environment.class.getName()).info(
				"STARTING TO INITIALIZE KNOWWE ENVIRONMENT");

		instance = new Environment(wiki);
		instance.init();
		EventManager.getInstance().fireEvent(InitEvent.getInstance());

		Logger.getLogger(Environment.class.getName()).info(
				"INITIALIZED KNOWWE ENVIRONMENT");
	}

	/**
	 * private contructor
	 * 
	 * @see getInstance()
	 * 
	 * @param wiki Connector to the used core wiki engine
	 */
	private Environment(WikiConnector wiki) {
		this.wikiConnector = wiki;
	}

	private void init() {

		try {
			initProperties();
			initPlugins();
			initTagHandler();
			initSectionizerModules();

			// decorate types in breast-first-search
			decorateTypeTree();

			initKnowledgeRepresentationHandler();
			initInstantiations();
			Plugins.initJS();
			Plugins.initCSS();
		}
		catch (Throwable e) {
			String msg = "Invalid initialization of the wiki type tree. This is caused by an invalid wiki plugin. Wiki is in unstable state. Please exit and correct before using the wiki.";
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, msg, e);
			throw new IllegalStateException(msg, e);
		}
	}

	private void initProperties() {

		setCompilationMode();

		setLogLevels();
	}

	private void setLogLevels() {

		ResourceBundle config = KnowWEUtils.getConfigBundle();
		Collection<String> logLevelConfigs = getLogLevelConfigs(config);
		if (!logLevelConfigs.isEmpty()) {

			Properties loggingProperties = new Properties();

			// root logger loglevel
			loggingProperties.put(".level", "INFO");

			// specify root logger handler
			loggingProperties.put("handlers",
					"java.util.logging.ConsoleHandler");

			// configure ConsoleHandler
			loggingProperties.put("java.util.logging.ConsoleHandler.formatter",
					"java.util.logging.SimpleFormatter");

			// set ConsoleHandler's log level
			loggingProperties.put("java.util.logging.ConsoleHandler.level",
					"ALL");

			for (String logLevel : logLevelConfigs) {
				// if nothing follows 'loglvl', root logger's log level will
				// be changed
				if (logLevel.equals("loglvl")) {
					loggingProperties.put(".level", config.getString(logLevel));
				}
				// change specific logger's log level
				else {
					String logger = logLevel.substring(logLevel.indexOf('.') + 1);
					loggingProperties.put(logger + ".level",
							config.getString(logLevel));
				}
			}

			// forward properties to LogManager
			PipedOutputStream pos = new PipedOutputStream();
			try {
				PipedInputStream pis = new PipedInputStream(pos);

				loggingProperties.store(pos, "");
				pos.close();

				LogManager.getLogManager().readConfiguration(pis);
				pis.close();
			}
			catch (IOException ioe) {
				Logger.getLogger(this.getClass().getName()).severe(
						"Failed to set LogLevel: " + ioe.getMessage());
			}
		}
	}

	private ResourceBundle setCompilationMode() {
		ResourceBundle config = KnowWEUtils.getConfigBundle();
		if (config != null && config.getString("compilation.mode").contains("incremental")) {
			this.setCompilationMode(CompilationMode.INCREMENTAL);
		}
		return config;
	}

	private Collection<String> getLogLevelConfigs(ResourceBundle config) {
		Collection<String> logLevelConfigs = new ArrayList<String>();
		// loop config file
		for (String logLevel : config.keySet()) {
			// logLevel properties start with 'loglvl'
			if (logLevel.startsWith("loglvl")) {
				logLevelConfigs.add(logLevel);
			}
		}
		return logLevelConfigs;
	}

	private void initInstantiations() {
		for (Instantiation inst : Plugins.getInstantiations()) {
			inst.init();
		}
	}

	private void initTagHandler() {
		for (TagHandler tagHandler : Plugins.getTagHandlers()) {
			initTagHandler(tagHandler);
		}
	}

	private void initSectionizerModules() {
		for (SectionizerModule sm : Plugins.getSectionizerModules()) {
			Sectionizer.registerSectionizerModule(sm);
		}
	}

	private void initKnowledgeRepresentationHandler() {
		KnowledgeRepresentationManager manager = this.getKnowledgeRepresentationManager(DEFAULT_WEB);
		for (KnowledgeRepresentationHandler handler : Plugins.getKnowledgeRepresentationHandlers()) {
			handler.setWeb(DEFAULT_WEB);
			manager.registerHandler(handler);
		}
	}

	private void initPlugins() throws InstantiationError {
		File libDir = new File(KnowWEUtils.getApplicationRootPath() + "/WEB-INF/lib");
		// when testing, libDir doesn't exist, but the plugin framework is
		// initialized in junittest, so there is no problem
		// if libDir is doesn't exist in runtime, nothing will work, so this
		// code won't be reached ;-)
		if (libDir.exists()) {
			List<File> pluginFiles = getPluginFiles(libDir);
			JPFPluginManager.init(pluginFiles.toArray(new File[pluginFiles.size()]));
			extractPluginResources();
		}
	}

	private List<File> getPluginFiles(File libDir) {
		List<File> pluginFiles = new ArrayList<File>();
		for (File file : libDir.listFiles()) {
			if (file.getName().contains("KnowWE-Plugin-")
					|| file.getName().contains("d3web-Plugin-")) {
				pluginFiles.add(file);
			}
		}
		return pluginFiles;
	}

	private void extractPluginResources() throws InstantiationError {
		Plugin[] plugins = PluginManager.getInstance().getPlugins();

		for (Plugin plugin : plugins) {
			Resource[] resources = plugin.getResources();
			for (Resource resource : resources) {
				String pathName = resource.getPathName();
				if (!pathName.endsWith("/") && pathName.startsWith("webapp/")) {
					pathName = pathName.substring("webapp/".length());
					try {
						File file = new File(KnowWEUtils.getApplicationRootPath() + "/" + pathName);
						File parent = file.getParentFile();
						if (!parent.isDirectory()) {
							parent.mkdirs();
						}
						FileOutputStream out = new FileOutputStream(file);
						InputStream in = resource.getInputStream();
						try {
							byte[] buf = new byte[1024];
							int len;
							while ((len = in.read(buf)) != -1) {
								out.write(buf, 0, len);
							}
						}
						finally {
							in.close();
							out.close();
						}
					}
					catch (IOException e) {
						String msg = "Cannot instantiate plugin "
								+ plugin
								+ ", the following error occured while extracting its resources: "
								+ e.getMessage();
						throw new InstantiationError(msg);
					}
				}
			}
		}
	}

	/**
	 * Initialize all types by decorating them. The method makes sure that each
	 * instance is decorated only once. To do this a breath-first-search is
	 * used. Thus each item is initialized with the shortest path towards it.
	 * 
	 * @created 24.10.2013
	 */
	private void decorateTypeTree() {

		// queue the queue of paths to be initialized
		RootType root = RootType.getInstance();
		LinkedList<Type[]> queue = new LinkedList<Type[]>();
		queue.add(new Type[] { root });

		// queuedTypes the already queued instances
		HashSet<Type> queuedTypes = new HashSet<Type>();
		queuedTypes.add(root);

		while (!queue.isEmpty()) {
			// initialize the first element of the queue
			// until the queue is empty
			Type[] path = queue.pop();
			Type type = path[path.length - 1];

			// initialize plugged items
			Plugins.addChildrenTypesToType(type, path);
			Plugins.addSubtreeHandlersToType(type, path);
			Plugins.addRendererToType(type, path);
			Plugins.addAnnotations(type, path);

			// initialize type itself
			type.init(path);

			// recurse for child types
			for (Type childType : type.getChildrenTypes()) {
				// secure against malformed plugins
				if (childType == null) {
					throw new NullPointerException(
							"type '" + type.getClass().getName() + "' contains 'null' as child");
				}

				// avoid multiple decoration in recursive type declarations
				if (!queuedTypes.add(childType)) continue;

				// create path to this type
				Type[] childPath = new Type[path.length + 1];
				System.arraycopy(path, 0, childPath, 0, path.length);
				childPath[path.length] = childType;

				// add child to the end of the queue --> breadth first search
				queue.add(childPath);
			}
		}
	}

	private void initTagHandler(TagHandler tagHandler) {
		String tagName = tagHandler.getTagName();
		String key = tagName.toLowerCase();

		if (tagHandlers.containsKey(key)) {
			Logger.getLogger(this.getClass().getName()).warning(
					"TagHandler for tag '" + tagName
							+ "' had already been added.");
		}
		else {
			this.tagHandlers.put(key, tagHandler);
		}
	}

	public WikiConnector getWikiConnector() {
		return this.wikiConnector;
	}

	/**
	 * Returns from the prioritized list of plugged renderers for the given type
	 * the renderer positioned next in the priority list after the given
	 * renderer. If the passed renderer is not contained in the given list, the
	 * first renderer of the list is returned.
	 * 
	 * @created 04.11.2013
	 * @param t
	 * @param currentRenderer
	 * @return
	 */
	public Renderer getNextRendererForType(Type t, Renderer currentRenderer) {
		if (additionalRenderers.containsKey(t)) {
			List<Renderer> pluggedRenderersForType = additionalRenderers.get(t);
			if (pluggedRenderersForType != null) {
				if (pluggedRenderersForType.contains(currentRenderer)) {
					int currentIndex = pluggedRenderersForType.indexOf(currentRenderer);
					if (pluggedRenderersForType.size() > currentIndex) {
						return pluggedRenderersForType.get(currentIndex + 1);
					}
				}
				else {
					if (pluggedRenderersForType.size() > 0) {
						return pluggedRenderersForType.get(0);
					}
				}
			}
		}

		return null;
	}

	public void addRendererForType(Type t, Renderer r) {
		if (!this.additionalRenderers.containsKey(t)) {
			this.additionalRenderers.put(t, new ArrayList<Renderer>());
		}
		List<Renderer> renderersForType = this.additionalRenderers.get(t);
		renderersForType.add(0, r);
	}

	/**
	 * Builds an {@link Article} and registers it in the {@link ArticleManager}.
	 */
	public Article buildAndRegisterArticle(String content,
			String title, String web) {
		return buildAndRegisterArticle(content, title, web, false);
	}

	/**
	 * Builds an {@link Article} and registers it in the {@link ArticleManager}.
	 */
	public Article buildAndRegisterArticle(String content,
			String title, String web, boolean fullParse) {

		if (Article.isArticleCurrentlyBuilding(web, title)) {
			return Article.getCurrentlyBuildingArticle(web, title);
		}

		// create article with the new content
		Article article = Article.createArticle(content, title, web);

		this.getArticleManager(web).registerArticle(article);

		return article;
	}

	/**
	 * Returns the {@link Article} object for a given web and title
	 * 
	 * @param web the web of the {@link Article}
	 * @param title the title of the {@link Article}
	 */
	public Article getArticle(String web, String title) {
		return getArticleManager(web).getArticle(title);
	}

	/**
	 * Returns the {@link ArticleManager} for a given web.
	 * 
	 * @param web the web of the {@link ArticleManager}
	 */
	public ArticleManager getArticleManager(String web) {
		ArticleManager mgr = this.articleManagers.get(web);
		if (mgr == null) {
			mgr = new ArticleManager(this, web);
			articleManagers.put(web, mgr);
		}
		return mgr;
	}

	public KnowledgeRepresentationManager getKnowledgeRepresentationManager(String web) {
		KnowledgeRepresentationManager mgr = this.knowledgeManagers.get(web);
		if (mgr == null) {
			mgr = new KnowledgeRepresentationManager(web);
			knowledgeManagers.put(web, mgr);
		}
		return mgr;
	}

	/**
	 * Returns the {@link PackageManager} for a given web.
	 * 
	 * @param web the web of the {@link PackageManager}
	 */
	public PackageManager getPackageManager(String web) {
		PackageManager mgr = this.packageManagers.get(web);
		if (mgr == null) {
			mgr = new PackageManager(web);
			packageManagers.put(web, mgr);
		}
		return mgr;
	}

	/**
	 * Returns the TerminologyHandler for a given compiling article.
	 * 
	 * @param article the compiling article
	 * @return the {@link TerminologyManager} for that article
	 */
	public TerminologyManager getTerminologyManager(Article article) {
		return getTerminologyManager(article.getWeb(), article.getName());
	}

	/**
	 * Returns the TerminologyHandler for a given compiling article specified by
	 * its web and article name.
	 * 
	 * @param web the web of the compiling article
	 * @param title the name of the compiling article
	 * @return the {@link TerminologyManager} for that article
	 */
	public TerminologyManager getTerminologyManager(String web, String title) {
		Map<String, TerminologyManager> managersOfWeb = this.terminologyManagers.get(web);
		if (managersOfWeb == null) {
			managersOfWeb = new HashMap<String, TerminologyManager>();
			this.terminologyManagers.put(web, managersOfWeb);
		}
		TerminologyManager mgr = managersOfWeb.get(title);
		if (mgr == null) {
			mgr = new TerminologyManager(web, title);
			managersOfWeb.put(title, mgr);
		}
		return mgr;
	}

	public Collection<TerminologyManager> getTerminologyManagers(String web) {
		Map<String, TerminologyManager> managersOfWeb = this.terminologyManagers.get(web);
		if (managersOfWeb == null) {
			return Collections.emptyList();
		}
		else {
			return Collections.unmodifiableCollection(managersOfWeb.values());
		}
	}

	public ServletContext getContext() {
		return wikiConnector.getServletContext();
	}

	/**
	 * grants access on the default tag handlers of KnowWE
	 * 
	 * @return HashMap holding the default tag handlers of KnowWE
	 */
	public Map<String, TagHandler> getDefaultTagHandlers() {
		return tagHandlers;
	}

	public List<PageAppendHandler> getAppendHandlers() {
		return Plugins.getPageAppendHandlers();
	}

	public void setCompilationMode(CompilationMode mode) {
		currentCompilationMode = mode;
	}

	public CompilationMode getCompilationMode() {
		return currentCompilationMode;
	}

	/**
	 * Cloning is not allowed for the Environment of KnowWE.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
