/*
 * Copyright (C) 2013 University Wuerzburg, Computer Science VI
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
package de.knowwe.ontology.compile;

import java.util.List;

import com.denkbares.semanticcore.Reasoning;
import de.d3web.strings.Strings;
import de.d3web.utils.Log;
import de.knowwe.core.compile.PackageCompiler;
import de.knowwe.core.compile.PackageRegistrationCompiler;
import de.knowwe.core.compile.PackageRegistrationCompiler.PackageRegistrationScript;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.compile.packaging.DefaultMarkupPackageCompileType;
import de.knowwe.core.compile.packaging.DefaultMarkupPackageCompileTypeRenderer;
import de.knowwe.core.compile.packaging.PackageCompileType;
import de.knowwe.core.compile.packaging.PackageManager;
import de.knowwe.core.compile.packaging.PackageTerm;
import de.knowwe.core.compile.terminology.TermCompiler.MultiDefinitionMode;
import de.knowwe.core.event.EventManager;
import de.knowwe.core.kdom.basicType.AttachmentType;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.DelegateRenderer;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.report.CompilerMessage;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.defaultMarkup.AnnotationNameType;
import de.knowwe.kdom.defaultMarkup.AnnotationType;
import de.knowwe.kdom.defaultMarkup.CompileMarkupPackageRegistrationScript;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupPackageReferenceRegistrationScript;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupPackageRegistrationScript;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.ontology.kdom.InitTerminologyHandler;
import de.knowwe.util.Icon;

/**
 * Compiles and provides ontology from the Ontology-MarkupSet.
 *
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.12.2013
 */
public class OntologyType extends DefaultMarkupType {

	public static final String PLUGIN_ID = "KnowWE-Plugin-Ontology-MarkupSet";

	public static final String ANNOTATION_COMPILE = "uses";
	public static final String ANNOTATION_RULE_SET = "ruleset";
	public static final String ANNOTATION_MULTI_DEF_MODE = "multiDefinitionMode";
	public static final String ANNOTATION_COMMIT = "commit";
	public static final String ANNOTATION_IMPORT = "import";
	public static final String ANNOTATION_EXPORT = "export";
	public static final String ANNOTATION_SILENT_IMPORT = "silentImport";

	private static final DefaultMarkup MARKUP;

	static {
		MARKUP = new DefaultMarkup("Ontology");
		MARKUP.addAnnotation(ANNOTATION_COMPILE, false);
		MARKUP.addAnnotationIcon(ANNOTATION_COMPILE, Icon.PACKAGE.addTitle("Uses"));

		MARKUP.addAnnotation(ANNOTATION_IMPORT, false);
		MARKUP.addAnnotationIcon(ANNOTATION_IMPORT, Icon.FILE_XML.addTitle("Import"));

		MARKUP.addAnnotation(ANNOTATION_EXPORT, false);
		MARKUP.addAnnotationIcon(ANNOTATION_EXPORT, Icon.GLOBE.addTitle("Export"));

		MARKUP.addAnnotation(ANNOTATION_SILENT_IMPORT, false);
		MARKUP.addAnnotationIcon(ANNOTATION_SILENT_IMPORT, Icon.FILE.addTitle("Import silently (faster, but without term support)"));

		MARKUP.addAnnotation(ANNOTATION_RULE_SET, false, Reasoning.values());
		MARKUP.addAnnotationIcon(ANNOTATION_RULE_SET, Icon.COG.addTitle("Rule Set"));

		MARKUP.addAnnotation(ANNOTATION_MULTI_DEF_MODE, false, MultiDefinitionMode.values());
		MARKUP.addAnnotationIcon(ANNOTATION_MULTI_DEF_MODE, Icon.ORDERED_LIST.addTitle("Multi-definition-mode"));

		MARKUP.addAnnotationContentType(ANNOTATION_IMPORT, new AttachmentType());
		MARKUP.addAnnotation(ANNOTATION_COMMIT, false, CommitType.values());
		DefaultMarkupPackageCompileType compileType = new DefaultMarkupPackageCompileType();
		compileType.addCompileScript(Priority.INIT, new InitTerminologyHandler());
		compileType.addCompileScript(new OntologyCompilerRegistrationScript());
		MARKUP.addContentType(compileType);

		MARKUP.addAnnotationContentType(PackageManager.COMPILE_ATTRIBUTE_NAME, new PackageTerm());
	}

	public OntologyType() {
		super(MARKUP);

		this.removeCompileScript(PackageRegistrationCompiler.class,
				DefaultMarkupPackageReferenceRegistrationScript.class);
		this.setRenderer(new DefaultMarkupPackageCompileTypeRenderer() {
			@Override
			protected void renderContents(Section<?> section, UserContext user, RenderResult string) {
				List<Section<AnnotationType>> annotations = Sections.successors(section, AnnotationType.class);
				for (Section<AnnotationType> annotation : annotations) {
					Section<AnnotationNameType> annotationName = Sections.successor(annotation, AnnotationNameType.class);
					if (annotationName.getText().startsWith("@" + ANNOTATION_COMPILE)) continue;
					DelegateRenderer.getInstance().render(annotation, user, string);
					string.appendHtml("<br>");
				}
				super.renderContents(section, user, string);
			}
		});

		removeCompileScript(PackageRegistrationCompiler.class, DefaultMarkupPackageRegistrationScript.class);
		addCompileScript(new CompileMarkupPackageRegistrationScript());

		EventManager.getInstance().registerListener(OntologyExporter.getInstance());
	}

	private static class OntologyCompilerRegistrationScript extends PackageRegistrationScript<PackageCompileType> {

		@Override
		public void compile(PackageRegistrationCompiler compiler, Section<PackageCompileType> section) throws CompilerMessage {
			Section<DefaultMarkupType> ontologyType = Sections.ancestor(section, DefaultMarkupType.class);
			String ruleSetValue = DefaultMarkupType.getAnnotation(ontologyType, ANNOTATION_RULE_SET);
			Reasoning ruleSet = getRuleSet(ruleSetValue);
			String multiDefModeValue = DefaultMarkupType.getAnnotation(ontologyType, ANNOTATION_MULTI_DEF_MODE);
			MultiDefinitionMode multiDefMode = getMultiDefinitionMode(multiDefModeValue);
			OntologyCompiler ontologyCompiler = new OntologyCompiler(
					compiler.getPackageManager(), section, OntologyType.class, ruleSet, multiDefMode);
			compiler.getCompilerManager().addCompiler(5, ontologyCompiler);

			//OntologyConstructCompiler constructCompiler = new OntologyConstructCompiler(ontologyCompiler);
			//compiler.getCompilerManager().addCompiler(6, constructCompiler);

			if (ruleSetValue != null && ruleSet == null) {
				throw CompilerMessage.warning("The rule set \"" + ruleSetValue + "\" does not exist.");
			}
		}

		private MultiDefinitionMode getMultiDefinitionMode(String multiDefModeValue) {
			return parseEnum(MultiDefinitionMode.class, multiDefModeValue, "multi-definition-mode");
		}

		private Reasoning getRuleSet(String ruleSetValue) {
			return parseEnum(Reasoning.class, ruleSetValue, "rule set");
		}

		private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, String enumName) {
			if (value != null) {
				try {
					return Enum.valueOf(enumClass, value);
				}
				catch (IllegalArgumentException e) {
					Log.warning("'" + value + "' is not a " + enumName + ", please choose one of the following: "
							+ Strings.concat(", ", enumClass.getEnumConstants()));
				}
			}
			return null;
		}

		@Override
		public void destroy(PackageRegistrationCompiler compiler, Section<PackageCompileType> section) {
			// we just remove the no longer used compiler... we do not need to destroy the s
			for (PackageCompiler packageCompiler : section.get().getPackageCompilers(section)) {
				if (packageCompiler instanceof OntologyCompiler) {
					compiler.getCompilerManager().removeCompiler(packageCompiler);
				}
			}
		}

	}

}