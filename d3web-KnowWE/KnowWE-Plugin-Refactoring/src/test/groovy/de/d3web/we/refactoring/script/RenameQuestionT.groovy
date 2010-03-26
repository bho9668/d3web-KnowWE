package de.d3web.we.refactoring.script;

import de.d3web.we.kdom.KnowWEObjectType;
import de.d3web.we.kdom.objects.QuestionDef;

class RenameQuestionT extends Rename {
	@Override
	public Class<? extends KnowWEObjectType> findRenamingType() {
		return QuestionDef.class;
	}
	
	@Override
	public <T extends KnowWEObjectType> String findObjectID(Class<T> clazz) {
		return "RenameObject/RootType/QuestionTree/QuestionTree@content/QuestionDashTree/SubTree/" +
		"SubTree/DashTreeElement/QuestionDashTreeElementContent/QuestionLine/QuestionDef";
	}
	
	@Override
	public String findNewName() {
		return "Frage Umbenannt";
	}
}
