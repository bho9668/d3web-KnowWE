package de.knowwe.core.compile;

import java.util.Collection;

import de.knowwe.core.compile.terminology.TermCompiler;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.report.CompilerMessage;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;

/**
 * Compiles all default stuff that happens globally and not in packages.
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 13.11.2013
 */
public class DefaultGlobalCompiler implements TermCompiler {

	private TerminologyManager terminologyManager;
	private CompilerManager compilerManager;

	@Override
	public TerminologyManager getTerminologyManager() {
		return this.terminologyManager;
	}

	@Override
	public void init(CompilerManager compilerManager) {
		this.compilerManager = compilerManager;
		this.terminologyManager = new TerminologyManager();
	}

	@Override
	public void destroy() {
		// nothing to do
	}

	@Override
	public void compile(Collection<Section<?>> added, Collection<Section<?>> removed) {
		ScriptCompiler<DefaultGlobalCompiler> helper = new ScriptCompiler<DefaultGlobalCompiler>(
				this);
		for (Section<?> section : removed) {
			helper.addSubtree(section);
		}
		helper.destroy();
		helper = new ScriptCompiler<DefaultGlobalCompiler>(this);
		for (Section<?> section : added) {
			helper.addSubtree(section);
		}
		helper.compile();
	}

	@Override
	public CompilerManager getCompilerManager() {
		return this.compilerManager;
	}

	public static abstract class DefaultGlobalScript<T extends Type>
			implements CompileScript<DefaultGlobalCompiler, T>, DestroyScript<DefaultGlobalCompiler, T> {

		@Override
		public Class<DefaultGlobalCompiler> getCompilerClass() {
			return DefaultGlobalCompiler.class;
		}

		@Override
		public void destroy(DefaultGlobalCompiler compiler, Section<T> section) {
			// nothing to do for now...
		}
	}

	public static abstract class DefaultGlobalHandler<T extends Type>
			extends DefaultGlobalScript<T> {

		@Override
		public Class<DefaultGlobalCompiler> getCompilerClass() {
			return DefaultGlobalCompiler.class;
		}

		public abstract Collection<Message> create(DefaultGlobalCompiler compiler, Section<T> section) throws CompilerMessage;

		@Override
		public void compile(DefaultGlobalCompiler compiler, Section<T> section) throws CompilerMessage {
			Messages.storeMessages(section, getClass(), create(compiler, section));

		}

		@Override
		public void destroy(DefaultGlobalCompiler compiler, Section<T> section) {
			// nothing to do for now...
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	@Override
	public boolean isCompiling(Section<?> section) {
		return true;
	}

}
