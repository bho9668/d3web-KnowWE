package de.knowwe.core.compile.packaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.knowwe.core.compile.PackageCompiler;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.parsing.Section;

public abstract class PackageCompileType extends AbstractType {

	private static final String COMPILER_STORY_KEY = "compilerStoreKey";

	/**
	 * Needs to return a Collection of package names. These are the packages to
	 * be compiled compiled later...
	 * 
	 * @created 02.10.2010
	 * @param section should always be the Section calling this method.
	 * @return a Collection of package names.
	 */
	public abstract String[] getPackagesToCompile(Section<? extends PackageCompileType> section);

	@SuppressWarnings("unchecked")
	public Collection<PackageCompiler> getPackageCompilers(Section<? extends PackageCompileType> section) {
		Collection<PackageCompiler> compilers = (Collection<PackageCompiler>)
				section.getSectionStore().getObject(COMPILER_STORY_KEY);
		if (compilers == null) return Collections.emptyList();
		else return Collections.unmodifiableCollection(compilers);
	}

	public void registerPackageCompiler(PackageCompiler compiler, Section<? extends PackageCompileType> section) {
		@SuppressWarnings("unchecked")
		Collection<PackageCompiler> compilers = (Collection<PackageCompiler>)
				section.getSectionStore().getObject(COMPILER_STORY_KEY);
		if (compilers == null) {
			compilers = new ArrayList<PackageCompiler>(5);
			section.getSectionStore().storeObject(COMPILER_STORY_KEY, compilers);
		}
		compilers.add(compiler);
	}

}
