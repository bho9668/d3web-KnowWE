package de.knowwe.ontology.turtle.lazyRef;

import java.util.Collection;
import java.util.Iterator;

import de.d3web.strings.Identifier;
import de.knowwe.core.compile.Compilers;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.ontology.compile.OntologyCompiler;
import de.knowwe.tools.DefaultTool;
import de.knowwe.tools.Tool;
import de.knowwe.tools.ToolProvider;

public class LazyURIReferenceCompleteToolProvider implements ToolProvider {

	@Override
	public Tool[] getTools(Section<?> section, UserContext userContext) {
		OntologyCompiler compiler = Compilers.getCompiler(section, OntologyCompiler.class);
		Collection<Identifier> potentiallyMatchingIdentifiers = LazyURIReference.getPotentiallyMatchingIdentifiers(
				compiler, section);
		Tool[] tools = new Tool[potentiallyMatchingIdentifiers.size()];
		Iterator<Identifier> iterator = potentiallyMatchingIdentifiers.iterator();
		for (int i = 0; i < potentiallyMatchingIdentifiers.size(); i++) {
			Identifier identifier = iterator.next();
			tools[i] = createModificationTool(section, identifier);
		}
		return tools;
	}

	private Tool createModificationTool(Section<?> section, Identifier identifier) {
		Section<DefaultMarkupType> defaultMarkup = Sections.findAncestorOfType(section,
				DefaultMarkupType.class);
		String defaultMarkupID = defaultMarkup.getID();

		String insertString = identifier.toExternalForm().replaceFirst("#", ":");
		String jsAction = "KNOWWE.plugin.ontology.expandLazyReference('" + section.getID()
				+ "','" + insertString + "','" + defaultMarkupID + "')";
		return new DefaultTool(
				"KnowWEExtension/images/edit.png",
				"change to '" + insertString + "'",
				"expand current lazy identifier to '"
						+ insertString + "'",
				jsAction);
	}

	@Override
	public boolean hasTools(Section<?> section, UserContext userContext) {
		Collection<Identifier> potentiallyMatchingIdentifiers = LazyURIReference.getPotentiallyMatchingIdentifiers(
				Compilers.getCompiler(section, OntologyCompiler.class), section);
		return potentiallyMatchingIdentifiers != null && potentiallyMatchingIdentifiers.size() > 0;
	}

}
