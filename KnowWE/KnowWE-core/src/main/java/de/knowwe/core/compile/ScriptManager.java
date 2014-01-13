package de.knowwe.core.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.d3web.collections.PriorityList;
import de.knowwe.core.event.Event;
import de.knowwe.core.event.EventListener;
import de.knowwe.core.event.EventManager;
import de.knowwe.core.kdom.Type;
import de.knowwe.event.InitEvent;

/**
 * Class to manage all the compile scripts of a specific compiler type.
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 30.10.2013
 */
public class ScriptManager<C extends Compiler> implements EventListener {

	private static class ScriptList<C extends Compiler, T extends Type>
			extends PriorityList<Priority, CompileScript<C, T>> {

		public ScriptList() {
			super(Priority.DEFAULT);
		}
	}

	@SuppressWarnings("rawtypes")
	private static final ScriptList EMPTY_SCRIPT_LIST = new ScriptList();

	private final Class<C> compilerClass;

	@SuppressWarnings("rawtypes")
	private final Map<Type, ScriptList> scripts = Collections.synchronizedMap(new HashMap<Type, ScriptList>());

	private final Set<Type> subtreeTypesWithScripts = Collections.synchronizedSet(new HashSet<Type>());

	public ScriptManager(Class<C> compilerClass) {
		this.compilerClass = compilerClass;
		EventManager.getInstance().registerListener(this);
	}

	/**
	 * Adds a script for a specific type with a specific priority.
	 * 
	 * @created 30.10.2013
	 * @param priority the priority of the script
	 * @param type the type the script is intended for
	 * @param script the script to be added
	 */
	@SuppressWarnings({
			"rawtypes", "unchecked" })
	public <T extends Type> void addScript(Priority priority, T type, CompileScript<C, T> script) {
		// find map and create lazy if not exists
		ScriptList list = scripts.get(type);
		if (list == null) {
			list = new ScriptList();
			scripts.put(type, list);
		}

		// add script to list
		synchronized (list) {
			list.add(priority, script);
		}
	}

	private <T extends Type> void addSubtreeTypesWithScripts(Type type) {
		if (!subtreeTypesWithScripts.add(type)) return;
		for (Type parent : type.getParentTypes()) {
			addSubtreeTypesWithScripts(parent);
		}
	}

	@SuppressWarnings({
			"rawtypes", "unchecked" })
	public <T extends Type> Map<Priority, List<CompileScript<C, T>>> getScripts(T type) {
		ScriptList list = scripts.get(type);
		if (list == null) list = EMPTY_SCRIPT_LIST;
		synchronized (list) {
			return list.getPriorityMap();
		}
	}

	public Collection<Type> getTypes() {
		return Collections.unmodifiableSet(scripts.keySet());
	}

	public boolean hasScriptsForSubtree(Type type) {
		return subtreeTypesWithScripts.contains(type);
	}

	public <T extends Type> void removeScript(T type, Class<? extends CompileScript<C, T>> clazz) {
		@SuppressWarnings("unchecked")
		ScriptList<C, T> scriptsOfType = scripts.get(type);
		List<CompileScript<C, T>> remove = new ArrayList<CompileScript<C, T>>();
		synchronized (scriptsOfType) {
			for (CompileScript<C, T> compileScript : scriptsOfType) {
				if (compileScript.getClass().equals(clazz)) {
					remove.add(compileScript);
				}
			}
			scriptsOfType.removeAll(remove);
		}
	}

	public <T extends Type> void removeAllScript(T type) {
		scripts.remove(type);
	}

	public Class<C> getCompilerClass() {
		return compilerClass;
	}

	@Override
	public Collection<Class<? extends Event>> getEvents() {
		List<Class<? extends Event>> events = new ArrayList<Class<? extends Event>>();
		events.add(InitEvent.class);
		return events;
	}

	@Override
	public void notify(Event event) {
		if (event instanceof InitEvent) {
			// we have to do this after type are initialized, because while
			// scripts are added to the manager, not all parent-child-links may
			// be established
			for (Type type : getTypes()) {
				addSubtreeTypesWithScripts(type);
			}
		}

	}

}
