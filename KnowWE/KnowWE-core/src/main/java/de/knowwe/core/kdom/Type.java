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

package de.knowwe.core.kdom;

import java.util.List;
import java.util.TreeMap;

import de.knowwe.core.compile.Priority;
import de.knowwe.core.kdom.parsing.Parser;
import de.knowwe.core.kdom.rendering.Renderer;
import de.knowwe.core.kdom.subtreeHandler.SubtreeHandler;
import de.knowwe.core.report.MessageRenderer;

/**
 * @author Jochen
 * 
 *         This interface is the foundation of the KnowWE2 Knowledge-DOM
 *         type-system. To every node in this dom tree exactly one Type is
 *         associated.
 * 
 *         A type defines itself by its SectionFinder, which allocates text
 *         parts to this type.
 * @see getSectioner
 * 
 *      Further it defines what subtypes it allows.
 * @see getAllowedChildrenTypes
 * 
 *      For user presentation it provides a renderer.
 * @see getRenderer
 * 
 */
public interface Type {

	/**
	 * Returns whether this type is a leaf type, i.e., true if it has no
	 * children types.
	 * 
	 * @created 27.08.2013
	 * @return
	 */
	boolean isLeafType();

	/**
	 * Returns whether this.getClass() equals the given class.
	 * 
	 * @created 27.08.2013
	 * @param clazz
	 * @return
	 */
	boolean isType(Class<? extends Type> clazz);

	/**
	 * Returns whether the passed class isAssignableFrom the class of this
	 * instance.
	 * 
	 * @created 27.08.2013
	 * @param clazz
	 * @return
	 */
	boolean isAssignableFromType(Class<? extends Type> clazz);

	/**
	 * Initializes the type instance with a specific path to the root. Each type
	 * is initialized only once. If the type is available in multiple path the
	 * specified path is the breaths-first-path which can be used to access the
	 * type. Especially for recursive types this is also the most shallow path
	 * available. This method is called directly after the object has been
	 * initialized through the available decorating plugins, e.g. child types,
	 * subtree handlers, etc.
	 * 
	 * @created 28.10.2013
	 * @param path the path to this type
	 */
	void init(Type[] path);

	/*
	 * Methods related to parsing
	 */

	/**
	 * Returns the parser that can be used to parse the textual markup of this
	 * type into a section of this type. The parser is responsible to build the
	 * full kdom tree for the specified text.
	 * 
	 * @created 12.03.2011
	 * @return the parser to be used to parse textual markup of this type
	 */
	Parser getParser();

	/**
	 * Adds the type as a child of the current type with the given priority
	 * value. If there are already child types for the given priority it is
	 * appended at the end of the list (lower priority).
	 * 
	 * @created 27.08.2013
	 * @param priority the priority with which the type is added
	 * @param type the type to add
	 */
	void addChildType(double priority, Type type);

	/**
	 * Adds the type as the (currently) last child type with the default
	 * priority (which is 5).
	 * 
	 * @created 27.08.2013
	 * @param type the type to add
	 */
	void addChildType(Type type);

	/**
	 * Replaces the first type with the passed class where the type is instance
	 * of the passed class, if such is existing.
	 * 
	 * @created 27.08.2013
	 * @param newType type to be inserted
	 * @param classToBeReplaced class to determine what type should be replaced
	 * @return true if a replacement has been made
	 */
	boolean replaceChildType(Type type, Class<? extends Type> c);

	/**
	 * Clears the list of children for this type.
	 * 
	 * @created 27.08.2013
	 */
	void clearChildrenTypes();

	/**
	 * @return name of this type
	 */
	String getName();

	/**
	 * A (priority-ordered) list of the types, which are allowed as children of
	 * nodes of this type
	 * 
	 * @return
	 */
	List<Type> getChildrenTypes();

	/*
	 * Management of Renderers
	 */

	/**
	 * When KnowWE renders the article this renderer is used to render this
	 * node. In most cases rendering should be delegated to children types.
	 * 
	 * @created 27.08.2013
	 * @return
	 */
	Renderer getRenderer();

	void setRenderer(Renderer renderer);

	MessageRenderer getErrorRenderer();

	MessageRenderer getNoticeRenderer();

	MessageRenderer getWarningRenderer();

	/*
	 * Methods related to compilation
	 */
	boolean isOrderSensitive();

	boolean isIgnoringPackageCompile();

	void setIgnorePackageCompile(boolean ignorePackageCompile);

	void setOrderSensitive(boolean orderSensitive);

	/*
	 * Management of SubtreeHandlers
	 */
	TreeMap<Priority, List<SubtreeHandler<? extends Type>>> getSubtreeHandlers();

	List<SubtreeHandler<? extends Type>> getSubtreeHandlers(Priority p);

	void addSubtreeHandler(Priority p, SubtreeHandler<? extends Type> handler);

}
