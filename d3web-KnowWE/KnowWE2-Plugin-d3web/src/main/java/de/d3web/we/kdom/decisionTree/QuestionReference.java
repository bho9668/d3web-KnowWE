package de.d3web.we.kdom.decisionTree;

import de.d3web.we.kdom.LineContent;
import de.d3web.we.kdom.renderer.FontColorRenderer;

public class QuestionReference extends  LineContent {
	
	@Override
	protected void init() {
		this.setCustomRenderer(new FontColorRenderer(FontColorRenderer.COLOR8));
	}
	
}
