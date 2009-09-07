package de.d3web.we.action;

import java.util.List;

import de.d3web.kernel.domainModel.Answer;
import de.d3web.kernel.domainModel.QASet;
import de.d3web.kernel.domainModel.qasets.Question;
import de.d3web.we.core.DPSEnvironment;
import de.d3web.we.core.broker.Broker;
import de.d3web.we.core.knowledgeService.D3webKnowledgeService;
import de.d3web.we.core.knowledgeService.D3webKnowledgeServiceSession;
import de.d3web.we.core.knowledgeService.KnowledgeService;
import de.d3web.we.core.knowledgeService.KnowledgeServiceSession;
import de.d3web.we.d3webModule.DPSEnvironmentManager;
import de.d3web.we.javaEnv.KnowWEAttributes;
import de.d3web.we.javaEnv.KnowWEParameterMap;

public class QuestionStateReportAction implements KnowWEAction{
	/**
	 * Used by GuidelineModul edit in GuidelineRenderer: Method: d3webVariablesScript
	 * Don´t change output syntax
	 */
	@SuppressWarnings("deprecation")
	@Override
	public String perform(KnowWEParameterMap parameterMap) {
		String namespace = java.net.URLDecoder.decode(parameterMap.get(KnowWEAttributes.SEMANO_NAMESPACE));
		String questionID = parameterMap.get(KnowWEAttributes.SEMANO_OBJECT_ID);
		String questionName = parameterMap.get(KnowWEAttributes.TERM);
		if(questionName == null) {
			questionName = parameterMap.get(KnowWEAttributes.SEMANO_TERM_NAME);
		}
		//String valueid = parameterMap.get(KnowWEAttributes.SEMANO_VALUE_ID);
		//String valuenum = parameterMap.get(KnowWEAttributes.SEMANO_VALUE_NUM);
		//String valueids = parameterMap.get(KnowWEAttributes.SEMANO_VALUE_IDS);
		String user = parameterMap.get(KnowWEAttributes.USER);
		String web = parameterMap.get(KnowWEAttributes.WEB);
		
		String result = "Error on finding question state -wrong id/name ?";
		
		DPSEnvironment env = DPSEnvironmentManager.getInstance().getEnvironments(web);
		Broker broker = env.getBroker(user);
		
		KnowledgeServiceSession kss = broker.getSession().getServiceSession(namespace);
		KnowledgeService service = env.getService(namespace);
		Question q = null;
		if(service instanceof D3webKnowledgeService) {
			
			if(questionID != null) {
				
			QASet set = ((D3webKnowledgeService)service).getBase().searchQASet(questionID);
			if(set instanceof Question) {
				
				q = ((Question)set);
			}
			}
			if(questionName != null) {
				
				List<Question> questionList = ((D3webKnowledgeService)service).getBase().getQuestions();
				for (Question question : questionList) {
					if(question.getText().equals(questionName)) {
						
						q = question;
					}
				}
			}
		}
		if(kss instanceof de.d3web.we.core.knowledgeService.D3webKnowledgeServiceSession){
			
		}
		if(kss instanceof de.d3web.we.core.knowledgeService.D3webKnowledgeServiceSession && q != null) {
			
			D3webKnowledgeServiceSession d3kss = ((D3webKnowledgeServiceSession)kss);
			de.d3web.kernel.XPSCase case1 = d3kss.getXpsCase();
			List<? extends Question> answeredQuestions = case1.getAnsweredQuestions();
			if(answeredQuestions.contains(q)) {
				List answers = q.getValue(case1);
				result = "#"+q.getText()+":";
				for (Object object : answers) {
					
					if(object instanceof Answer) {
						result += ((Answer)object).toString() +";";
					}else {
						result += "no answer object";
					}
					
				}
				 
			} else {
				result = "undefined";
			}
		} 
		
		
		
		return result;
	}
	
	
}
