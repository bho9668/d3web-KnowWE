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

package de.knowwe.rdf2go.sparql;

import de.knowwe.core.compile.packaging.PackageAnnotationNameType;
import de.knowwe.core.compile.packaging.PackageManager;
import de.knowwe.core.compile.packaging.PackageTerm;
import de.knowwe.core.kdom.rendering.NothingRenderer;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.kdom.renderer.AsynchronRenderer;
import de.knowwe.rdf2go.Rdf2GoCore;

public class SparqlMarkupType extends DefaultMarkupType {

	public static final String RAW_OUTPUT = "rawOutput";
	public static final String NAVIGATION = "navigation";
	public static final String ZEBRAMODE = "zebramode";
	public static final String TREE = "tree";
	public static final String SORTING = "sorting";
	public static final String BORDER = "border";
	public static final String NAME = "name";
	public static final String RENDER_QUERY = "showQuery";
	private static DefaultMarkup m = null;

	public static final String MARKUP_NAME = "Sparql";

	static {
		m = new DefaultMarkup(MARKUP_NAME);
		m.addContentType(new SparqlContentType());
		m.addAnnotation(RAW_OUTPUT, false, "true", "false");
		m.addAnnotation(NAVIGATION, false, "true", "false");
		m.addAnnotation(RENDER_QUERY, false, "true", "false");
		m.addAnnotationRenderer(NAVIGATION, NothingRenderer.getInstance());
		m.addAnnotation(ZEBRAMODE, false, "true", "false");
		m.addAnnotationRenderer(ZEBRAMODE, NothingRenderer.getInstance());
		m.addAnnotation(TREE, false, "true", "false");
		m.addAnnotationRenderer(TREE, NothingRenderer.getInstance());
		m.addAnnotation(SORTING, false, "true", "false");
		m.addAnnotationRenderer(SORTING, NothingRenderer.getInstance());
		m.addAnnotation(BORDER, false, "true", "false");
		m.addAnnotationRenderer(BORDER, NothingRenderer.getInstance());
		m.addAnnotation(AsynchronRenderer.ASYNCHRONOUS, false, "true", "false");
		m.addAnnotationRenderer(AsynchronRenderer.ASYNCHRONOUS, NothingRenderer.getInstance());
		m.addAnnotation(Rdf2GoCore.GLOBAL, false, "true", "false");
		m.addAnnotationRenderer(Rdf2GoCore.GLOBAL, NothingRenderer.getInstance());
		m.addAnnotation(NAME, false);
		m.addAnnotationRenderer(NAME, NothingRenderer.getInstance());
		m.addAnnotation(PackageManager.PACKAGE_ATTRIBUTE_NAME, false);

		m.addAnnotationNameType(PackageManager.PACKAGE_ATTRIBUTE_NAME,
				new PackageAnnotationNameType());
		m.addAnnotationContentType(PackageManager.PACKAGE_ATTRIBUTE_NAME,
				new PackageTerm());
	}

	public SparqlMarkupType(DefaultMarkup markup) {
		super(markup);
		this.setRenderer(new Rdf2GoCoreCheckRenderer());
	}

	public SparqlMarkupType() {
		this(m);
	}

}
