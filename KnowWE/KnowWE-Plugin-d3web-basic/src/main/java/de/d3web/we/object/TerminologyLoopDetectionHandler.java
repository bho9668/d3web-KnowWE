package de.d3web.we.object;

import java.util.Collection;

import de.d3web.core.knowledge.TerminologyObject;
import de.d3web.core.manage.KnowledgeBaseUtils;
import de.d3web.we.reviseHandler.D3webSubtreeHandler;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;

public class TerminologyLoopDetectionHandler<TermObject extends TerminologyObject> extends D3webSubtreeHandler<D3webTermDefinition<TermObject>> {

	public static final String LOOP_DETECTED = "loopDetected";
	public static final String REMOVE_PARENTS = "removeParents";

	@Override
	public Collection<Message> create(Article article, Section<D3webTermDefinition<TermObject>> section) {
		TermObject termObject = section.get().getTermObject(article, section);
		if (termObject != null && KnowledgeBaseUtils.isInLoop(termObject)) {
			section.getSectionStore().storeObject(article, LOOP_DETECTED, REMOVE_PARENTS);
			return Messages.asList(Messages.error("Loop detected: The "
					+ termObject.getClass().getSimpleName() + " '" + termObject.getName()
					+ "' is an ancestor of itself."));
		}
		return Messages.noMessage();
	}

}
