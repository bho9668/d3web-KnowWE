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
package de.knowwe.plugin;

import java.util.ArrayList;
import java.util.List;

import de.d3web.collections.PriorityList;
import de.d3web.plugin.Extension;
import de.d3web.plugin.JPFExtension;
import de.d3web.plugin.PluginManager;
import de.d3web.strings.Strings;
import de.d3web.utils.Log;
import de.knowwe.core.Environment;
import de.knowwe.core.ResourceLoader;
import de.knowwe.core.action.Action;
import de.knowwe.core.append.PageAppendHandler;
import de.knowwe.core.compile.CompileScript;
import de.knowwe.core.compile.Compiler;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.SectionizerModule;
import de.knowwe.core.kdom.rendering.Renderer;
import de.knowwe.core.taghandler.TagHandler;
import de.knowwe.core.utils.ScopeUtils;
import de.knowwe.kdom.defaultMarkup.AnnotationType;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup.Annotation;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.kdom.defaultMarkup.UnknownAnnotationType;

/**
 * Provides utilities methods for Plugins used in KnowWE
 *
 * @author Jochen Reutelshöfer, Volker Belli & Markus Friedrich (denkbares GmbH)
 */
public class Plugins {

	public static final String SCOPE_ROOT = "root";
	public static final String EXTENDED_PLUGIN_ID = "KnowWEExtensionPoints";
	public static final String EXTENDED_POINT_KnowWEAction = "Action";
	public static final String EXTENDED_POINT_Type = "Type";
	public static final String EXTENDED_POINT_CompileScript = "CompileScript";
	public static final String EXTENDED_POINT_SubtreeHandler = "SubtreeHandler";
	public static final String EXTENDED_POINT_ToolProvider = "ToolProvider";
	public static final String EXTENDED_POINT_TagHandler = "TagHandler";
	public static final String EXTENDED_POINT_PageAppendHandler = "PageAppendHandler";
	public static final String EXTENDED_POINT_Instantiation = "Instantiation";
	public static final String EXTENDED_POINT_SectionizerModule = "SectionizerModule";
	public static final String EXTENDED_POINT_SemanticCore = "SemanticCoreImpl";
	public static final String EXTENDED_POINT_EventListener = "EventListener";
	public static final String EXTENDED_POINT_Terminology = "Terminology";
	public static final String EXTENDED_POINT_IncrementalCompileScript = "IncrementalCompileScript";
	public static final String EXTENDED_POINT_Renderer = "Renderer";
	public static final String EXTENDED_POINT_Annotation = "Annotation";
	public static final String EXTENDED_POINT_SearchProvider = "SearchProvider";
	public static final String EXTENDED_POINT_Compiler = "Compiler";
	public static final String EXTENDED_POINT_StatusProvider = "StatusProvider";

	private static <T> List<T> getSingeltons(String point, Class<T> clazz) {
		PluginManager pm = PluginManager.getInstance();
		Extension[] extensions = pm.getExtensions(EXTENDED_PLUGIN_ID, point);
		List<T> result = new ArrayList<T>();
		for (Extension e : extensions) {
			result.add(clazz.cast(e.getSingleton()));
		}
		return result;
	}

	/**
	 * Returns all plugged Instantiations These are used to initialize plugins.
	 *
	 * @return List of all Instantiations
	 */
	public static List<Instantiation> getInstantiations() {
		return getSingeltons(EXTENDED_POINT_Instantiation, Instantiation.class);
	}

	/**
	 * Returns all plugged StatusProviders. These are used to find out, if the status of the wiki has changed.
	 *
	 * @return List of all StatusProvider
	 */
	public static List<StatusProvider> getStatusProviders() {
		return getSingeltons(EXTENDED_POINT_StatusProvider, StatusProvider.class);
	}

	/**
	 * Returns all plugged Instantiations These are used to initialize plugins.
	 *
	 * @return List of all Instantiations
	 */
	public static PriorityList<Double, Compiler> getCompilers() {
		PluginManager pm = PluginManager.getInstance();
		Extension[] extensions = pm.getExtensions(EXTENDED_PLUGIN_ID, EXTENDED_POINT_Compiler);
		PriorityList<Double, Compiler> result = new PriorityList<Double, Compiler>(5d);
		for (Extension e : extensions) {
			result.add(e.getPriority(),
					Compiler.class.cast(e.getSingleton()));
		}
		return result;
	}

	/**
	 * Returns a list of all plugged actions. Actions can be executed from the web. Usually be
	 * clicking on pregenerated links on the wiki pages.
	 */
	public static List<Action> getKnowWEAction() {
		return getSingeltons(EXTENDED_POINT_KnowWEAction, Action.class);
	}

	public static void addChildrenTypesToType(Type type, Type[] path) {
		Extension[] extensions = PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_Type);
		for (Extension extension : ScopeUtils.getMatchingExtensions(extensions, path)) {
			double priority = extension.getPriority();
			Type pluggedType = (Type) extension.getNewInstance();
			type.addChildType(priority, pluggedType);

			// add documentation for default markups
			if (pluggedType instanceof DefaultMarkupType) {
				DefaultMarkup markup = ((DefaultMarkupType) pluggedType).getMarkup();
				if (Strings.isBlank(markup.getDocumentation())) {
					markup.setDocumentation(extension.getParameter("description"));
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void addCompileScriptsToType(Type type, Type[] path) {
		Extension[] extensions = PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_CompileScript);
		for (Extension extension : ScopeUtils.getMatchingExtensions(extensions, path)) {
			int priorityValue = Integer.parseInt(extension.getParameter("compilepriority"));
			Priority priority = Priority.getPriority(priorityValue);
			if (type instanceof AbstractType) {
				((AbstractType) type).addCompileScript(priority,
						(CompileScript<Compiler, AbstractType>) extension.getSingleton());
			}
			else {
				Log.warning("Tried to plug CompileScript '"
						+ extension.getSingleton().getClass().getSimpleName()
						+ "' into an type '" + type.getClass().getSimpleName()
						+ "' which is not an AbstractType");
			}
		}
	}

	public static void addRendererToType(Type type, Type[] path) {
		Extension[] extensions = PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_Renderer);
		Extension match = ScopeUtils.getMatchingExtension(extensions, path);
		if (match != null) {
			if (type instanceof AbstractType) {
				Renderer currentRenderer = type.getRenderer();
				Environment.getInstance().addRendererForType(type, currentRenderer);
				((AbstractType) type).setRenderer((Renderer) match.getSingleton());
			}
			else {
				throw new ClassCastException(
						"renderers can only be plugged to type instances of 'AbstractType', but not to "
								+ type.getClass().getName());
			}
		}
	}

	public static void addAnnotations(Type type, Type[] path) {
		if (type instanceof DefaultMarkupType) {

			DefaultMarkupType markupType = (DefaultMarkupType) type;
			List<Type> childrenTypes = markupType.getChildrenTypes();
			DefaultMarkup markup = markupType.getMarkup();

			Extension[] extensions = PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
					EXTENDED_POINT_Annotation);
			for (Extension extension : ScopeUtils.getMatchingExtensions(extensions, path)) {

				// add the annotation itself to the markup
				String regex = extension.getParameter("regex");
				String name = extension.getName();
				if (Strings.isBlank(regex)) {
					markup.addAnnotation(name, false);
				}
				else {
					markup.addAnnotation(name, false, regex);
				}

				// add the documentation from the plugin definition
				markup.getAnnotation(name).setDocumentation(extension.getParameter("description"));

				// add children type(s) defined as class attribute(s)
				for (String subTypeClass : extension.getParameters("class")) {
					Type subType = (Type) getNewInstance(extension, subTypeClass);
					markup.addAnnotationContentType(name, subType);
				}

				// set renderer for the annotation
				String renderer = extension.getParameter("renderer");
				if (!Strings.isBlank(renderer)) {
					Renderer pluggedRenderer = (Renderer) getNewInstance(extension, renderer);
					markup.addAnnotationRenderer(name, pluggedRenderer);
				}

				for (Type childTyp : childrenTypes) {
					if (childTyp.getClass().equals(UnknownAnnotationType.class)) {
						Annotation annotation = markup.getAnnotation(name);
						type.addChildType(new AnnotationType(annotation));
						break;
					}
				}
			}
		}
	}

	private static Object getNewInstance(Extension extension, String clazz) {
		return ((JPFExtension) extension).getNewInstance(clazz);
	}

	public static List<SectionizerModule> getSectionizerModules() {
		return getSingeltons(EXTENDED_POINT_SectionizerModule, SectionizerModule.class);
	}

	/**
	 * Returns a List of all plugged TagHandlers
	 * <p/>
	 * COMMENT: Alternatively, those taghandlers can also be introduced separately using the
	 * taghandler.text file. There the class of the taghandler is listed and will be loaded on
	 * KnowWE initialization.
	 *
	 * @return List of TagHandlers
	 */
	public static List<TagHandler> getTagHandlers() {
		return getSingeltons(EXTENDED_POINT_TagHandler, TagHandler.class);
	}

	/**
	 * Returns a list of all plugged PageAppendHandlers.
	 * <p/>
	 * These handlers allow a module to append some content to the wiki-page content. There are 2
	 * kinds of appendHandlers one append content at top of the page, the other appends at the
	 * bottom
	 *
	 * @return List of PageAppendHandlers
	 */
	public static List<PageAppendHandler> getPageAppendHandlers() {
		return getSingeltons(EXTENDED_POINT_PageAppendHandler, PageAppendHandler.class);
	}

	/**
	 * Returns a list of all plugged Annotations
	 *
	 * @return List of Annotations
	 * @created 31/07/2012
	 */
	public static List<Annotation> getAnnotations() {
		return getSingeltons(EXTENDED_POINT_Annotation, Annotation.class);
	}

	/**
	 * Initializes the Javascript files
	 */
	public static void initJS() {
		PriorityList<Double, String> files = new PriorityList<Double, String>(5.0);
		addScripts(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_PageAppendHandler));
		addScripts(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_Type));
		addScripts(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_TagHandler));
		addScripts(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_ToolProvider));
		addScripts(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_Renderer));
		addScripts(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_KnowWEAction));
		for (String s : files) {
			ResourceLoader.getInstance().add(s, ResourceLoader.RESOURCE_SCRIPT);
		}
	}

	public static void initCSS() {
		PriorityList<Double, String> files = new PriorityList<Double, String>(5.0);
		addCSS(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_PageAppendHandler));
		addCSS(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_Type));
		addCSS(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_TagHandler));
		addCSS(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_ToolProvider));
		addCSS(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_Renderer));
		addCSS(files, PluginManager.getInstance().getExtensions(EXTENDED_PLUGIN_ID,
				EXTENDED_POINT_KnowWEAction));
		for (String s : files) {
			ResourceLoader.getInstance().addFirst(s, ResourceLoader.RESOURCE_STYLESHEET);
		}
	}

	public static void initResources(Extension[] extensions) {
		PriorityList<Double, String> cssFiles = new PriorityList<Double, String>(5.0);
		addCSS(cssFiles, extensions);
		for (String s : cssFiles) {
			ResourceLoader.getInstance().addFirst(s, ResourceLoader.RESOURCE_STYLESHEET);
		}
		PriorityList<Double, String> jsFiles = new PriorityList<Double, String>(5.0);
		addScripts(jsFiles, extensions);
		for (int i = 0; i < jsFiles.size(); i++) {
			String filename = jsFiles.get(i);
			ResourceLoader.getInstance().add(filename, ResourceLoader.RESOURCE_SCRIPT);
		}

	}

	private static void addScripts(PriorityList<Double, String> files, Extension[] extensions) {
		for (Extension e : extensions) {
			double priority = e.getPriority();
			List<String> scripts = e.getParameters("script");
			if (scripts != null) {
				for (String s : scripts) {
					if (!files.contains(s)) {
						files.add(priority, s);
					}
				}
			}
		}
	}

	private static void addCSS(PriorityList<Double, String> files, Extension[] extensions) {
		for (Extension e : extensions) {
			double priority = e.getPriority();
			List<String> scripts = e.getParameters("css");
			if (scripts != null) {
				for (String s : scripts) {
					files.add(priority, s);
				}
			}
		}
	}

}
