/*
 * Copyright (C) 2010 Chair of Artificial Intelligence and Applied Informatics
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

package de.knowwe.core.compile.packaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.denkbares.collections.ConcatenateCollection;
import com.denkbares.events.EventManager;
import com.denkbares.strings.PredicateParser;
import com.denkbares.strings.PredicateParser.ParsedPredicate;
import com.denkbares.utils.Pair;
import de.knowwe.core.compile.Compiler;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.report.CompilerMessage;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

import static de.knowwe.core.kdom.parsing.Sections.$;

/**
 * Manages packages and its content in KnowWE.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 */
public class PackageManager {// implements EventListener {

	public static final String PACKAGE_ATTRIBUTE_NAME = "package";
	public static final String MASTER_ATTRIBUTE_NAME = "master";
	public static final String COMPILE_ATTRIBUTE_NAME = "uses";

	public static final String DEFAULT_PACKAGE = "default";

	public final Compiler compiler;

	/**
	 * For each article title, you get all default packages used in this article.
	 */
	private final Map<String, Set<String>> articleToDefaultPackages = new HashMap<>();
	private final Map<String, Set<ParsedPredicate>> articleToDefaultPackageRules = new HashMap<>();

	/**
	 * For each packageName, you get all Sections in the wiki belonging to this packageName.
	 */
	private final Map<String, Set<Section<? extends DefaultMarkupType>>> packageToSection = new HashMap<>();
	private final Map<Section<? extends DefaultMarkupType>, Set<String>> sectionToPackage = new HashMap<>();

	private final Map<ParsedPredicate, Set<Section<? extends DefaultMarkupType>>> predicateToSection = new HashMap<>();
	private final Map<Section<? extends DefaultMarkupType>, Set<ParsedPredicate>> sectionToPredicate = new HashMap<>();

	private final Set<Section<? extends DefaultMarkupPackageCompileType>> packageCompileSections = new HashSet<>();

	/**
	 * For each package, you get all compile sections of the compilers compiling the package.
	 */
	private final Map<String, Set<Section<? extends PackageCompileType>>> packageToCompilingSections = new HashMap<>();

	private final Map<String, Pair<Set<Section<?>>, Set<Section<?>>>> changedPackages = new HashMap<>();

	public <C extends Compiler> PackageManager(C compiler) {
		this.compiler = compiler;
		// EventManager.getInstance().registerListener(this);
	}

	public static void addPackageAnnotation(DefaultMarkup markup) {
		markup.addAnnotation(PackageManager.PACKAGE_ATTRIBUTE_NAME, false);
		markup.addAnnotationNameType(PackageManager.PACKAGE_ATTRIBUTE_NAME, new PackageAnnotationNameType());
		markup.addAnnotationContentType(PackageManager.PACKAGE_ATTRIBUTE_NAME, new PackageRule());
	}

	private boolean isDisallowedPackageName(String packageName) {
		return packageName == null || packageName.isEmpty();
	}

	public void addDefaultPackage(Article article, String defaultPackage) {
		articleToDefaultPackages.computeIfAbsent(article.getTitle(), k -> new HashSet<>(4)).add(defaultPackage);
	}

	public void addDefaultPackageRule(Article article, ParsedPredicate defaultPackageRule) {
		articleToDefaultPackageRules.computeIfAbsent(article.getTitle(), k -> new HashSet<>(4)).add(defaultPackageRule);
	}

	public void removeDefaultPackage(Article article, String defaultPackage) {
		Set<String> defaultPackages = articleToDefaultPackages.get(article.getTitle());
		if (defaultPackages != null) {
			defaultPackages.remove(defaultPackage);
			if (defaultPackages.isEmpty()) {
				articleToDefaultPackages.remove(article.getTitle());
			}
		}
	}

	public void removeDefaultPackageRule(Article article, ParsedPredicate defaultPackageRule) {
		Set<ParsedPredicate> defaultPackageRules = articleToDefaultPackageRules.get(article.getTitle());
		if (defaultPackageRules != null) {
			defaultPackageRules.remove(defaultPackageRule);
			if (defaultPackageRules.isEmpty()) {
				articleToDefaultPackageRules.remove(article.getTitle());
			}
		}
	}

	@NotNull
	public Set<String> getDefaultPackages(Article article) {
		Set<String> defaultPackages = articleToDefaultPackages.get(article.getTitle());
		if (defaultPackages == null) {
			defaultPackages = new HashSet<>(4);
			// we only use the DEFAULT package, if there is neither a default package rule nor a default package
			if (!articleToDefaultPackageRules.containsKey(article.getTitle())) {
				defaultPackages.add(DEFAULT_PACKAGE);
			}
		}
		return Collections.unmodifiableSet(defaultPackages);
	}

	@NotNull
	public Set<ParsedPredicate> getDefaultPackageRules(Article article) {
		return Collections.unmodifiableSet(articleToDefaultPackageRules.getOrDefault(article.getTitle(), Collections.emptySet()));
	}

	/**
	 * Adds the given Section to the package with the given name.
	 *
	 * @param section     the Section to add
	 * @param packageName the name of the package the Section is added to
	 * @created 28.12.2010
	 */
	public void addSectionToPackage(Section<? extends DefaultMarkupType> section, String packageName) throws CompilerMessage {
		if (isDisallowedPackageName(packageName)) {
			throw CompilerMessage.error("'" + packageName + "' is not allowed as a package name.");
		}
		packageToSection.computeIfAbsent(packageName, k -> new TreeSet<>()).add(section);
		sectionToPackage.computeIfAbsent(section, k -> new HashSet<>()).add(packageName);
		addSectionToChangedPackagesAsAdded(section, packageName);
	}

	private void addSectionToChangedPackagesAsAdded(Section<?> section, String packageName) {
		changedPackages.computeIfAbsent(packageName, s -> new Pair<>(new HashSet<>(), new HashSet<>()))
				.getA().add(section);
	}

	private void addSectionToChangedPackagesAsRemoved(Section<?> section, String packageName) {
		changedPackages.computeIfAbsent(packageName, s -> new Pair<>(new HashSet<>(), new HashSet<>()))
				.getB().add(section);
	}

	/**
	 * Adds/registers the given Section to/with the given package rule.
	 *
	 * @param section     the Section to add/register
	 * @param packageRule the package rule the Section is added/registerd to
	 */
	public void addSectionToPackageRule(Section<? extends DefaultMarkupType> section, ParsedPredicate packageRule) {
		predicateToSection.computeIfAbsent(packageRule, k -> new HashSet<>()).add(section);
		sectionToPredicate.computeIfAbsent(section, k -> new HashSet<>()).add(packageRule);
		for (String packageName : packageRule.getVariables()) {
			addSectionToChangedPackagesAsAdded(section, packageName);
		}
	}

	@NotNull
	public Set<String> getPackagesOfSection(Section<?> section) {
		Section<DefaultMarkupType> nearestDefaultMarkup = $(section).closest(DefaultMarkupType.class).getFirst();
		Set<String> packagesOfSection = sectionToPackage.getOrDefault(nearestDefaultMarkup, Collections.emptySet());
		return Collections.unmodifiableSet(packagesOfSection);
	}

	@NotNull
	public Set<ParsedPredicate> getPackageRulesOfSection(Section<?> section) {
		Section<DefaultMarkupType> nearestDefaultMarkup = $(section).closest(DefaultMarkupType.class).getFirst();
		Set<ParsedPredicate> packagesOfSection = sectionToPredicate.getOrDefault(nearestDefaultMarkup, Collections.emptySet());
		return Collections.unmodifiableSet(packagesOfSection);
	}

	/**
	 * Removes the given Section from the package with the given name.
	 *
	 * @param section     is the Section to remove
	 * @param packageName is the name of the package from which the section is removed
	 * @return whether the Section was removed
	 * @created 28.12.2010
	 */
	public boolean removeSectionFromPackage(Section<?> section, String packageName) {
		if (isDisallowedPackageName(packageName)) return false;
		Set<String> packageNames = sectionToPackage.get(section);
		if (packageNames != null) {
			packageNames.remove(packageName);
			if (packageNames.isEmpty()) {
				sectionToPackage.remove(section);
			}
		}
		Set<Section<? extends DefaultMarkupType>> packageSet = packageToSection.get(packageName);
		if (packageSet != null) {
			boolean removed = packageSet.remove(section);
			if (removed) {
				addSectionToChangedPackagesAsRemoved(section, packageName);
			}
			if (packageSet.isEmpty()) {
				packageToSection.remove(packageName);
			}
			return removed;
		}
		return false;
	}

	public boolean removeSectionFromPackageRule(Section<?> section, ParsedPredicate packageRule) {
		Set<ParsedPredicate> packageRules = sectionToPredicate.get(section);
		if (packageRules != null) {
			packageRules.remove(packageRule);
			if (packageRules.isEmpty()) {
				sectionToPredicate.remove(section);
			}
		}
		Set<Section<? extends DefaultMarkupType>> ruleSet = predicateToSection.get(packageRule);
		if (ruleSet != null) {
			boolean removed = ruleSet.remove(section);
			if (removed) {
				for (String packageName : packageRule.getVariables()) {
					addSectionToChangedPackagesAsRemoved(section, packageName);
				}
			}
			if (ruleSet.isEmpty()) {
				predicateToSection.remove(packageRule);
			}
			return removed;
		}
		return false;
	}

	/**
	 * Removes the given Section from all packages it was added to.
	 *
	 * @param s is the Section to remove
	 * @created 28.12.2010
	 */
	public void removeSectionFromAllPackagesAndRules(Section<?> s) {
		for (String packageName : new ArrayList<>(getPackagesOfSection(s))) {
			removeSectionFromPackage(s, packageName);
		}
		for (ParsedPredicate packageRule : new ArrayList<>(getPackageRulesOfSection(s))) {
			removeSectionFromPackageRule(s, packageRule);
		}
	}

	/**
	 * Returns an unmodifiable view on the sections of the given packages at the time of calling this method. The
	 * sections don't have a particular order.
	 * <p>
	 * NOTE: For each section-subtree that is contained in a package, only the top-most section of the package is
	 * returned; all successor sections are implicit in the same package.
	 *
	 * @param packageNames the package names to get the sections for
	 * @return the sections of the given packages
	 * @created 15.12.2013
	 */
	public Collection<Section<?>> getSectionsOfPackage(String... packageNames) {
		// minor optimization for single packages
		if (packageNames.length == 1) {
			return Collections.unmodifiableCollection(packageToSection.getOrDefault(packageNames[0], Collections.emptySet()));
		}

		List<Set<Section<? extends DefaultMarkupType>>> sets = new ArrayList<>();
		for (String packageName : packageNames) {
			Set<Section<? extends DefaultMarkupType>> sections = packageToSection.get(packageName);
			if (sections != null) {
				sets.add(sections);
			}
		}

		if (!predicateToSection.isEmpty()) {
			PackagePredicateValueProvider valueProvider = new PackagePredicateValueProvider(packageNames);
			for (Map.Entry<ParsedPredicate, Set<Section<? extends DefaultMarkupType>>> entry : predicateToSection.entrySet()) {
				if (entry.getKey().test(valueProvider)) {
					sets.add(entry.getValue());
				}
			}
		}

		//noinspection unchecked
		return new ConcatenateCollection<>(sets.toArray(new Set[0]));
	}

	public boolean hasChanged(String... packageNames) {
		for (String packageName : packageNames) {
			if (changedPackages.containsKey(packageName)) return true;
		}
		return false;
	}

	/**
	 * Returns all sections added to the given packages since the changes were last cleared with {@link
	 * #clearChangedPackages()}. The sections don't have a particular order.
	 *
	 * @param packageNames the package to return the added sections for
	 * @return the sections last added to the given packages
	 * @created 15.12.2013
	 */
	public Collection<Section<?>> getAddedSections(String... packageNames) {
		List<Set<Section<?>>> sets = new ArrayList<>();
		for (String packageName : packageNames) {
			Pair<Set<Section<?>>, Set<Section<?>>> pair = changedPackages.get(packageName);
			if (pair != null) {
				sets.add(pair.getA());
			}
		}
		//noinspection unchecked
		return new ConcatenateCollection<>(sets.toArray(new Set[0]));
	}

	/**
	 * Returns all sections removed from the given packages since the changes were last cleared with {@link
	 * #clearChangedPackages()}. The sections don't have a particular order.
	 *
	 * @param packageNames the package to return the removed sections for
	 * @return the sections last removed to the given packages
	 * @created 15.12.2013
	 */
	public Collection<Section<?>> getRemovedSections(String... packageNames) {
		List<Set<Section<?>>> sets = new ArrayList<>();
		for (String packageName : packageNames) {
			Pair<Set<Section<?>>, Set<Section<?>>> pair = changedPackages.get(packageName);
			if (pair != null) {
				sets.add(pair.getB());
			}
		}
		//noinspection unchecked
		return new ConcatenateCollection<>(sets.toArray(new Set[0]));
	}

	public void registerPackageCompileSection(Section<? extends DefaultMarkupPackageCompileType> section) {

		packageCompileSections.add(section);

		for (String object : section.get().getPackagesToCompile(section)) {
			packageToCompilingSections.computeIfAbsent(object, k -> new HashSet<>()).add(section);
		}

		EventManager.getInstance().fireEvent(new RegisteredPackageCompileSectionEvent(section));
	}

	public boolean unregisterPackageCompileSection(Section<? extends PackageCompileType> section) {

		boolean removed = packageCompileSections.remove(section);
		if (removed) {
			for (Iterator<Set<Section<? extends PackageCompileType>>> iterator = packageToCompilingSections.values()
					.iterator(); iterator.hasNext(); ) {
				Set<Section<? extends PackageCompileType>> compileSections = iterator.next();
				compileSections.remove(section);
				if (compileSections.isEmpty()) {
					iterator.remove();
				}
			}
		}

		EventManager.getInstance().fireEvent(new UnregisteredPackageCompileSectionEvent(section));
		return removed;
	}

	public Set<String> getAllPackageNames() {
		return Collections.unmodifiableSet(packageToSection.keySet());
	}

	public Collection<Section<? extends DefaultMarkupPackageCompileType>> getCompileSections() {
		return Collections.unmodifiableCollection(packageCompileSections);
	}

	public void clearChangedPackages() {
		this.changedPackages.clear();
	}

	/**
	 * Returns all the Sections of type {@link PackageCompileType}, that have a package the given Section is part of.
	 *
	 * @param section the Section we want the compile Sections for
	 * @return a Set of Sections compiling the given Section
	 * @created 28.12.2010
	 */
	public Set<Section<? extends PackageCompileType>> getCompileSections(Section<?> section) {
		Set<Section<? extends PackageCompileType>> compileSections = new HashSet<>();
		for (String packageName : section.getPackageNames()) {
			compileSections.addAll(getCompileSections(packageName));
		}
		return Collections.unmodifiableSet(compileSections);
	}

	/**
	 * Returns the Sections of type {@link PackageCompileType}, that represent the compiler compiling the given
	 * package.
	 *
	 * @param packageName is the name of the package
	 * @return a Set of titles of articles compiling the package with the given name.
	 * @created 28.08.2010
	 */
	public Set<Section<? extends PackageCompileType>> getCompileSections(String packageName) {
		Set<Section<? extends PackageCompileType>> compilingSections = new HashSet<>();

		Set<Section<? extends PackageCompileType>> resolvedSections = packageToCompilingSections.get(packageName);
		if (resolvedSections != null) {
			compilingSections.addAll(resolvedSections);
		}

		return Collections.unmodifiableSet(compilingSections);
	}

	/**
	 * @created 15.11.2013
	 * @deprecated
	 */
	@Deprecated
	public Set<String> getCompilingArticles(Section<?> section) {
		Set<Section<? extends PackageCompileType>> compileSections = getCompileSections(section);
		Set<String> titles = new HashSet<>();
		for (Section<? extends PackageCompileType> sections : compileSections) {
			titles.add(sections.getTitle());
		}
		return titles;
	}

	/**
	 * @created 15.11.2013
	 * @deprecated
	 */
	@Deprecated
	public Set<String> getCompilingArticles() {
		Collection<Section<? extends DefaultMarkupPackageCompileType>> compileSections = getCompileSections();
		Set<String> titles = new HashSet<>();
		for (Section<? extends PackageCompileType> sections : compileSections) {
			titles.add(sections.getTitle());
		}
		return titles;
	}

	/**
	 * @created 15.11.2013
	 * @deprecated
	 */
	@Deprecated
	public Set<String> getCompilingArticles(String packageName) {
		Collection<Section<? extends PackageCompileType>> compileSections = getCompileSections(packageName);
		Set<String> titles = new HashSet<>();
		for (Section<? extends PackageCompileType> sections : compileSections) {
			titles.add(sections.getTitle());
		}
		return titles;
	}

	private static class PackagePredicateValueProvider implements PredicateParser.ValueProvider {

		private final Set<String> packageNames;

		public PackagePredicateValueProvider(String... packageNames) {
			this.packageNames = new HashSet<>(Arrays.asList(packageNames));
		}

		@Override
		public @Nullable Collection<String> get(@NotNull String variable) {
			return packageNames.contains(variable) ? List.of("true") : List.of("false");
		}
	}
}
