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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.knowwe.core.compile.CompileScript;
import de.knowwe.core.compile.Compiler;
import de.knowwe.core.compile.CompilerManager;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.compile.ScriptManager;
import de.knowwe.core.kdom.basicType.PlainText;
import de.knowwe.core.kdom.parsing.Parser;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sectionizable;
import de.knowwe.core.kdom.parsing.Sectionizer;
import de.knowwe.core.kdom.rendering.DelegateRenderer;
import de.knowwe.core.kdom.rendering.Renderer;
import de.knowwe.core.kdom.sectionFinder.SectionFinder;
import de.knowwe.core.report.DefaultMessageRenderer;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.MessageRenderer;

public abstract class AbstractType implements Type, Sectionizable {

	/**
	 * the children types of the type. Used to serve the getAllowedChildrenTypes
	 * of the Type interface
	 * 
	 * @see Type#getChildrenTypes()
	 * 
	 */
	private final TypePriorityList childrenTypes = new TypePriorityList();

	private final List<Type> parents = new ArrayList<Type>(2);

	/**
	 * Contains all types this type can have as successors. It can be used to
	 * faster search for {@link Section}s of a certain type inside the KDOM.
	 * This set will be filled lazily while the KDOM is created. For search
	 * speed it would be better to have this set in every {@link Section} with
	 * the actual successor types of this section, but to reduce the memory
	 * overhead of another Set in each individual {@link Section}, we just do
	 * this per type. It will not be much slower to search and we can also
	 * reduce the overhead for creating this set (because it we don't need to to
	 * it every time a new Section is created).
	 */
	private final Set<Class<?>> successorTypes = new HashSet<Class<?>>();

	/**
	 * a flag to show, that this ObjectType is sensitive to the order its
	 * Sections appears in the article
	 */
	private boolean isOrderSensitive = false;

	/**
	 * determines whether there is a enumeration of siblings defined for this
	 * type (e.g., to add line numbers to line-based sections)
	 */
	private boolean isNumberedType = false;

	/**
	 * allows to set a custom renderer for a type (at initialization) if a
	 * custom renderer is set, it is used to present the type content in the
	 * wiki view
	 */
	private Renderer renderer = DelegateRenderer.getInstance();

	/**
	 * The sectionFinder of this type, used to serve the getSectionFinder-method
	 * of the Type interface
	 * 
	 * @see Type#getSectionFinder()
	 */
	private SectionFinder sectionFinder;

	public boolean isNumberedType() {
		return isNumberedType;
	}

	public void setNumberedType(boolean isNumberedType) {
		this.isNumberedType = isNumberedType;
	}

	/**
	 * Allows to set a specific sectionFinder for this type
	 * 
	 * @param sectionFinder
	 */
	@Override
	public void setSectionFinder(SectionFinder sectionFinder) {
		this.sectionFinder = sectionFinder;
	}

	/**
	 * constructor calling init() which is abstract
	 * 
	 */
	public AbstractType() {
		successorTypes.add(PlainText.class);
	}

	public AbstractType(SectionFinder sectionFinder) {
		this();
		this.setSectionFinder(sectionFinder);
	}

	@SuppressWarnings("unchecked")
	public <C extends Compiler, T extends Type, CS extends CompileScript<C, T>> void removeCompileScript(Class<C> compilerClass, Class<CS> scriptClass) {
		CompilerManager.getScriptManager(compilerClass).removeScript((T) this, scriptClass);
	}

	public <C extends Compiler, T extends Type> void addCompileScript(CompileScript<C, T> script) {
		addCompileScript(null, script);
	}

	/**
	 * Registers the given SubtreeHandlers with the given Priority.
	 */
	@SuppressWarnings("unchecked")
	public <C extends Compiler, T extends Type> void addCompileScript(Priority priority, CompileScript<C, T> script) {
		if (priority == null) priority = Priority.DEFAULT;
		CompilerManager.addScript(priority, (T) this, script);
	}

	@Override
	public boolean replaceChildType(Type type, Class<? extends Type> c) {
		Type replacedType = childrenTypes.replaceType(type, c);
		if (replacedType != null && replacedType instanceof AbstractType) {
			removeParentChildLink((AbstractType) replacedType);
		}
		if (type instanceof AbstractType) {
			addParentChildLink((AbstractType) type);
		}
		return replacedType != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.d3web.we.kdom.Type#getName()
	 */
	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.d3web.we.kdom.Sectionizable#getSectioFinder()
	 */
	@Override
	public final SectionFinder getSectionFinder() {
		return sectionFinder;
	}

	@Override
	public Parser getParser() {
		return new Sectionizer(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.d3web.we.kdom.Type#getAllowedChildrenTypes()
	 */
	@Override
	public final List<Type> getChildrenTypes() {
		return childrenTypes.getTypes();
	}

	@Override
	public final Collection<Type> getParentTypes() {
		return Collections.unmodifiableCollection(parents);
	}

	@Override
	public void clearChildrenTypes() {
		for (Type childrenType : childrenTypes.getTypes()) {
			if (childrenType instanceof AbstractType) {
				removeParentChildLink((AbstractType) childrenType);
			}
		}
		this.childrenTypes.clear();
	}

	@SuppressWarnings("unchecked")
	public <T extends Type> void clearCompileScripts() {
		Collection<ScriptManager<? extends Compiler>> scriptManagers = CompilerManager.getScriptManagers();
		for (ScriptManager<? extends Compiler> manager : scriptManagers) {
			manager.removeAllScript((T) this);
		}
	}

	@Override
	public void addChildType(double priority, Type type) {
		if (type instanceof AbstractType) {
			addParentChildLink((AbstractType) type);
		}
		childrenTypes.addType(priority, type);
	}

	private void addParentChildLink(AbstractType type) {
		type.parents.add(this);
		addSuccessorType(type.getClass());
		for (Class<?> successor : type.successorTypes) {
			addSuccessorType(successor);
		}
	}

	@Override
	public final void addChildType(Type type) {
		addChildType(TypePriorityList.DEFAULT_PRIORITY, type);
	}

	/**
	 * Adds the given typeClass to the potential successor types of this type.
	 * The added successor is automatically also added to all parent types.
	 * <p>
	 * <b>Normally, you don't have to add successor types manually, because
	 * children types are automatically also added as successor types. You
	 * should only need this, if you change the type tree after initialization,
	 * which you should only do, if you exactly know what you are doing.</b>
	 * 
	 * @created 10.12.2013
	 * @param typeClass the class of the successor you want to add
	 */
	public void addSuccessorType(Class<?> typeClass) {
		if (typeClass != null
				&& Type.class.isAssignableFrom(typeClass)
				&& typeClass != Type.class
				&& typeClass != Object.class) {
			if (successorTypes.add(typeClass)) {
				for (Type parent : parents) {
					if (parent instanceof AbstractType) {
						((AbstractType) parent).addSuccessorType(typeClass);
					}
				}
			}
			addSuccessorType(typeClass.getSuperclass());
			for (Class<?> interfaze : typeClass.getInterfaces()) {
				addSuccessorType(interfaze);
			}
		}
	}

	/**
	 * Removes the first occurrence (descending priority order) of a type where
	 * the given class is assignable from this type.
	 * 
	 * @created 09.12.2013
	 * @param typeClass the class of the type to be removed
	 */
	public void removeChildType(Class<? extends Type> typeClass) {
		Type removedType = this.childrenTypes.removeType(typeClass);
		if (removedType != null && removedType instanceof AbstractType) {
			removeParentChildLink((AbstractType) removedType);
		}
		// we could also clean the successorTypes, but for now, because of
		// possible loops and inheritance we don't know for sure which
		// successors belong to which child exactly
	}

	private void removeParentChildLink(AbstractType type) {
		type.parents.remove(this);
	}

	/**
	 * Adds the given type at the end of the (current) children priority chain.
	 * 
	 * @created 27.08.2013
	 * @param type the type to add
	 */
	public void addChildTypeLast(Type type) {
		childrenTypes.addLast(type);
	}

	@Override
	public final Renderer getRenderer() {
		return renderer;
	}

	@Override
	public MessageRenderer getMessageRenderer(Message.Type messageType) {
		switch (messageType) {
		case INFO:
			return DefaultMessageRenderer.NOTE_RENDERER;
		case WARNING:
			return DefaultMessageRenderer.WARNING_RENDERER;
		default:
			return DefaultMessageRenderer.ERROR_RENDERER;
		}
	}

	/**
	 * Allows to set a renderer for this type
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	@Override
	public void init(Type[] path) {
		// do nothing here for default
	}

	protected Set<Class<?>> getPotentialSuccessorTypes() {
		return Collections.unmodifiableSet(successorTypes);
	}

	@Override
	public boolean isOrderSensitive() {
		return isOrderSensitive;
	}

	public void setOrderSensitive(boolean orderSensitive) {
		this.isOrderSensitive = orderSensitive;
	}

	// /**
	// * If a type ignores package compile, all SubtreeHandlers registered to
	// this
	// * type will always compile, but only for the article the section is
	// * directly hooked in.
	// */
	// public void setIgnorePackageCompile(boolean ignorePackageCompile) {
	// this.setPackageCompile(!ignorePackageCompile);
	// }
	//
	// public void setPackageCompile(boolean packageCompile) {
	// this.packageCompile = packageCompile;
	// }
}
