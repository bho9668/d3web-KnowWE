package de.knowwe.ontology.turtle.compile;

import org.ontoware.rdf2go.model.node.Resource;

import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.rdf2go.Rdf2GoCore;

public interface ResourceProvider<T extends Type> extends NodeProvider<T> {

	Resource getResource(Section<T> section, Rdf2GoCore core);
}
