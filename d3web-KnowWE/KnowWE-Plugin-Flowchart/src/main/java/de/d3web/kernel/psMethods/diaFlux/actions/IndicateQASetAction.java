package de.d3web.kernel.psMethods.diaFlux.actions;

import de.d3web.kernel.XPSCase;
import de.d3web.kernel.domainModel.QASet;

/**
 * @author Reinhard Hatko
 * Created: 14.09.2009
 *
 */
public class IndicateQASetAction implements IAction {

	
	private final QASet qaSet;
	
	
	public IndicateQASetAction(QASet qaSet) {
		this.qaSet = qaSet; 
	}
	
	@Override
	public void act(XPSCase theCase) {

		theCase.getQASetManager().getQASetQueue().add(qaSet);
		//TODO notify???
		
	}

	@Override
	public Object getObject() {
		return qaSet;
	}

	@Override
	public boolean isUndoable() {
		return true; //not really if questions were asked, but has it sideeffects?
	}

	@Override
	public void undo() {
		// do something?? z.b. Werte von nur 1 mal zu erfragenden Fragen l�schen?
		
		//QASet von Agenda l�schen, falls noch nicht gefragt?s
	}
	
	@Override
	public String toString() {
		return "Indicate (" + qaSet + ")";
	}
	

}
