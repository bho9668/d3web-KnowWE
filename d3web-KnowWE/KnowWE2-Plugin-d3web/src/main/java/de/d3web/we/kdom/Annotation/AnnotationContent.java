package de.d3web.we.kdom.Annotation;

import de.d3web.we.kdom.DefaultAbstractKnowWEObjectType;
import de.d3web.we.kdom.sectionFinder.AllTextFinder;
import de.d3web.we.kdom.semanticAnnotation.AnnotatedString;
import de.d3web.we.kdom.semanticAnnotation.AnnotationMapSign;

public class AnnotationContent extends DefaultAbstractKnowWEObjectType {

	@Override
	public void init() {
		this.childrenTypes.add(new AnnotationMapSign());
		this.childrenTypes.add(new AnnotatedString());
		this.childrenTypes.add(new AnnotationObject());
		
		this.sectionFinder = new AllTextFinder(this);		
		
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}


}
