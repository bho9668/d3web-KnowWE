/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 *                    Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package de.d3web.we.flow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import de.d3web.KnOfficeParser.D3webConditionBuilder;
import de.d3web.KnOfficeParser.DefaultLexer;
import de.d3web.KnOfficeParser.RestrictedIDObjectManager;
import de.d3web.KnOfficeParser.complexcondition.ComplexConditionSOLO;
import de.d3web.kernel.domainModel.Answer;
import de.d3web.kernel.domainModel.Diagnosis;
import de.d3web.kernel.domainModel.KnowledgeBase;
import de.d3web.kernel.domainModel.KnowledgeBaseManagement;
import de.d3web.kernel.domainModel.NamedObject;
import de.d3web.kernel.domainModel.QASet;
import de.d3web.kernel.domainModel.RuleAction;
import de.d3web.kernel.domainModel.RuleComplex;
import de.d3web.kernel.domainModel.RuleFactory;
import de.d3web.kernel.domainModel.Score;
import de.d3web.kernel.domainModel.answers.AnswerChoice;
import de.d3web.kernel.domainModel.qasets.QContainer;
import de.d3web.kernel.domainModel.qasets.Question;
import de.d3web.kernel.domainModel.qasets.QuestionMC;
import de.d3web.kernel.domainModel.ruleCondition.AbstractCondition;
import de.d3web.kernel.domainModel.ruleCondition.CondAnd;
import de.d3web.kernel.psMethods.diaFlux.ConditionTrue;
import de.d3web.kernel.psMethods.diaFlux.FluxSolver;
import de.d3web.kernel.psMethods.diaFlux.actions.IndicateFlowAction;
import de.d3web.kernel.psMethods.diaFlux.actions.NoopAction;
import de.d3web.kernel.psMethods.diaFlux.flow.Flow;
import de.d3web.kernel.psMethods.diaFlux.flow.FlowFactory;
import de.d3web.kernel.psMethods.diaFlux.flow.FlowSet;
import de.d3web.kernel.psMethods.diaFlux.flow.IEdge;
import de.d3web.kernel.psMethods.diaFlux.flow.INode;
import de.d3web.kernel.psMethods.heuristic.ActionHeuristicPS;
import de.d3web.kernel.psMethods.nextQASet.ActionIndication;
import de.d3web.kernel.psMethods.nextQASet.ActionInstantIndication;
import de.d3web.kernel.psMethods.questionSetter.ActionSetValue;
import de.d3web.report.Message;
import de.d3web.we.core.KnowWEEnvironment;
import de.d3web.we.flow.type.ActionType;
import de.d3web.we.flow.type.CommentType;
import de.d3web.we.flow.type.EdgeType;
import de.d3web.we.flow.type.ExitType;
import de.d3web.we.flow.type.GuardType;
import de.d3web.we.flow.type.NodeType;
import de.d3web.we.flow.type.PositionType;
import de.d3web.we.flow.type.SourceType;
import de.d3web.we.flow.type.StartType;
import de.d3web.we.flow.type.TargetType;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.basic.PlainText;
import de.d3web.we.kdom.filter.SectionFilter;
import de.d3web.we.kdom.xml.AbstractXMLObjectType;
import de.d3web.we.logging.Logging;
import de.d3web.we.terminology.D3webReviseSubTreeHandler;

/**
 * 
 *
 * @author Reinhard Hatko
 * Created on: 12.10.2009
 */
public class FlowchartSubTreeHandler extends D3webReviseSubTreeHandler {
	

	@Override
	public void reviseSubtree(KnowWEArticle article, Section s) {
		
		KnowledgeBaseManagement kbm = getKBM(article, s);
		
		if (kbm == null)
			return;

		Section content = ((AbstractXMLObjectType) s.getObjectType()).getContentChild(s);

		if (content == null) 
			return;
		
		List<Message> errors = new ArrayList<Message>();
		
		List<Section> nodeSections = new ArrayList<Section>();
		List<Section> edgeSections = new ArrayList<Section>();

		Map<String, String> attributeMap = AbstractXMLObjectType.getAttributeMapFor(s);
		String name = attributeMap.get("name");
		
		if (name == null || name.equals(""))
			name = "unnamed";
		
		content.findSuccessorsOfType(NodeType.class, nodeSections);
		List<INode> nodes = createNodes(article, name, nodeSections, errors);
		
		if (nodeSections.size() != nodes.size())
			System.out.println("Could not parse all nodes");
		
//		System.out.println(nodes);
//		System.out.println(nodeSections);
		
		
		content.findSuccessorsOfType(EdgeType.class, edgeSections);
		List<IEdge> edges = createEdges(article, edgeSections, nodes, errors);
		
		
		String id = attributeMap.get("id");
		
		Flow flow = FlowFactory.getInstance().createFlow(id, name, nodes, edges);
		
		
		KnowledgeBase knowledgeBase = getKBM(article, s).getKnowledgeBase();

		List slices = (List) knowledgeBase.getKnowledge(FluxSolver.class, FluxSolver.DIAFLUX);
		
		
		FlowSet flows;
		if (slices == null || slices.size() == 0 ) {
			flows = new FlowSet(s.getTitle());
			knowledgeBase.addKnowledge(FluxSolver.class, flows, FluxSolver.DIAFLUX);
			
		} else {
			flows = (FlowSet) slices.get(0);
		}
		
		flows.put(flow);
		
		
		
		if (!errors.isEmpty())
			System.out.println(errors.size() +" errors in Flow '" + name +"': " + errors);
		
		
	}



	private List<IEdge> createEdges(KnowWEArticle article, List<Section> edgeSections, List<INode> nodes, List<Message> errors) {
		List<IEdge> result = new ArrayList<IEdge>();
		
		
		for (Section section : edgeSections) {
			
			String id = AbstractXMLObjectType.getAttributeMapFor(section).get("id");
			Section content = (Section) section.getChildren().get(1); //get edgecontent-section
			
			String sourceID = getXMLContentText(content.findChildOfType(SourceType.class));
			
			INode source = getNodeByID(sourceID, nodes);
			
			if (source == null) {
				errors.add(new Message("No source node found with id '" + sourceID + "' in edge '" + id + "'."));
				continue;
			}
			
			
			String targetID = getXMLContentText(content.findChildOfType(TargetType.class));
			
			INode target = getNodeByID(targetID, nodes);
			
			if (target == null) {
				errors.add(new Message("No target node found with id '" + targetID + "' in edge '" + id + "'."));
				continue;
			}
			
			
			
			
			AbstractCondition condition;
			
			
			Section guardSection = content.findChildOfType(GuardType.class);
			
			if (guardSection != null) {
				condition = buildCondition(article, guardSection, errors);
				
				if (condition == null) {
					condition = ConditionTrue.INSTANCE;
					
					errors.add(new Message("Could not parse condition: " + guardSection.getOriginalText()));
					
				}
				
			}
			else
				condition = ConditionTrue.INSTANCE;
			
			result.add(FlowFactory.getInstance().createEdge(id, source, target, condition));
			
		}
		
		
		
		return result;
	}

	private AbstractCondition buildCondition(KnowWEArticle article, Section s, List<Message> errors) {
		
		String originalText = getXMLContentText(s);
		
		InputStream stream = new ByteArrayInputStream(originalText.getBytes());
		ANTLRInputStream input  = null;
		try {
			input = new ANTLRInputStream(stream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		DefaultLexer lexer = new DefaultLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		ComplexConditionSOLO parser = new ComplexConditionSOLO(tokens);
		
		RestrictedIDObjectManager idom = new RestrictedIDObjectManager(getKBM(article, s));
		
		//For creating conditions in edges coming out from call nodes
		//TODO explicitly create oc-question for all FCs
//		idom.setLazyAnswers(true);
//		idom.setLazyQuestions(true);
		
		D3webConditionBuilder builder = new D3webConditionBuilder("Parsed from article", errors, idom);
		
		
		parser.setBuilder(builder);
		try {
			parser.complexcondition();
		} catch (RecognitionException e) {
			e.printStackTrace();
		}
		AbstractCondition condition = builder.pop();
		
		return condition;
	}

	private String getXMLContentText(Section s) {
		String originalText = ((Section) s.getChildren().get(1)).getOriginalText();
		return originalText;
	}

	private INode getNodeByID(String nodeID, List<INode> nodes) {

		for (INode node : nodes) {
			if (node.getID().equals(nodeID))
				return node;
		}
		
		return null;
	}

	private List<INode> createNodes(KnowWEArticle article, String flowName, List<Section> nodeSections, List<Message> errors) {
		
		List<INode> result = new ArrayList<INode>();
		
		
		for (Section section : nodeSections) {
			
			Section nodeContent = (Section) section.getChildren().get(1); //Section of NodeContentType
			
			// get the important info 
			List<Section> children = nodeContent.getChildren(new SectionFilter() { 
				
				public boolean accept(Section section) {
					return section.getObjectType() != PositionType.getInstance() && section.getObjectType() != PlainText.getInstance();
				}
			});
			
//			Assert.assertEquals(section + " has the wrong number of children.",1, children.size());
			
			if (children.size() != 1)
				continue;
			
			Section nodeinfo = children.get(0);
			
			String id = AbstractXMLObjectType.getAttributeMapFor(section).get("id");
			
			if (nodeinfo.getObjectType() == StartType.getInstance()) 
				result.add(createStartNode(id, nodeinfo));
			else if (nodeinfo.getObjectType() == ExitType.getInstance())
				result.add(createEndNode(article, id, flowName, nodeinfo));
			else if(nodeinfo.getObjectType() == ActionType.getInstance())
				result.add(createActionNode(article, id, nodeinfo, errors));
			else if(nodeinfo.getObjectType() == CommentType.getInstance())
				result.add(createCommentNode(article, id, nodeinfo, errors));
			else
				errors.add(new Message("Unknown node type: " + nodeinfo.getObjectType()));
//				throw new UnsupportedOperationException();
			
			
		}
		
		
		return result;
	}

	private INode createCommentNode(KnowWEArticle article, String id,
			Section nodeinfo, List<Message> errors) {

		RuleAction action = NoopAction.INSTANCE;
		
		return FlowFactory.getInstance().createNode(id, action);
	}



	private INode createActionNode(KnowWEArticle article, String id, Section section, List<Message> errors) {
		
		RuleAction action = NoopAction.INSTANCE;
		String string = getXMLContentText(section);
		if (string.startsWith("ERFRAGE")) {
			action = createQAindicationAction(article, section, string, errors);
		} else if (string.contains("=")) {
			action = createHeuristicScoreAction(article, section, string, errors);
		} else if (string.startsWith("CALL[")) {
			action = createCallFlowAction(article, section, string, errors);
		}
		else
			action = NoopAction.INSTANCE;
		
		return FlowFactory.getInstance().createNode(id, action);
		
	}

	private RuleAction createCallFlowAction(KnowWEArticle article, Section section, String string, List<Message> errors) {
		int nodenameStart = string.indexOf('(');
		int nodenameEnd = string.indexOf(')');

		String flowName = string.substring(5, nodenameStart);
		String nodeName = string.substring(nodenameStart + 1, nodenameEnd);
		
		
		KnowledgeBaseManagement kbm = getKBM(article, section);
		
		QContainer container = kbm.findQContainer(flowName + "_Questionnaire");
		
		if (container == null) {
			errors.add(new Message("Terminology not found for flow'" + flowName + "'"));
			return NoopAction.INSTANCE;
		}
		
		QuestionMC question = null;
		
		for (NamedObject child : container.getChildren()) {
			
			if (child.getText().equals(flowName + "_" + FlowchartTerminologySubTreeHandler.STARTNODES_QUESTION_NAME))
				question = (QuestionMC) child;
		}
		
		if (question == null) {
			errors.add(new Message("No startnode question found for flow '" + flowName + "'."));
			return NoopAction.INSTANCE;
		}
		
		ActionSetValue action = FlowFactory.getInstance().createSetValueAction();

		action.setQuestion(question);
		
		AnswerChoice answer = null;
		for (AnswerChoice child : question.getAllAlternatives()) {
			if (child.getText().equals(nodeName))
				answer = (AnswerChoice) child;
			
		}
		
		if (answer == null) {
			errors.add(new Message("No startnode  '" + flowName + "' not found in terminology of flow '" + flowName +"'."));
			return NoopAction.INSTANCE;
		}
		
		action.setValues(new AnswerChoice[] {answer} );
		
		return action;
	}



	private RuleAction createHeuristicScoreAction(KnowWEArticle article, Section section,
			String string, List<Message> errors) {
		
		String[] split = string.split("=");
		String solution = split[0].trim();
		String score = split[1].trim();
		
		//Fix after Refactoring
		RuleComplex rule = new RuleComplex();
		rule.setId("FlowchartRule" + System.currentTimeMillis());
		
		ActionHeuristicPS action = new ActionHeuristicPS(rule);
		rule.setAction(action);
		rule.setCondition(new CondAnd(new ArrayList()));
		//
		
		
		if (solution.startsWith("\""))// remove "
			solution = solution.substring(1, solution.length()-1);
		
		
		KnowledgeBaseManagement kbm = getKBM(article, section);
		Diagnosis diagnosis = kbm.findDiagnosis(solution); 
		
		if (diagnosis == null) {
			errors.add(new Message("Diagnosis not found: " + solution));
		}
		
		action.setDiagnosis(diagnosis);
		
		if (score.contains("P7"))
			action.setScore(Score.P7);
		else 
			action.setScore(Score.N7);
		
		
		return action;
	}

	private RuleAction createQAindicationAction(KnowWEArticle article, Section section, String string, List<Message> errors) {
		String name = string.substring(8, string.length() - 1);
		QASet findQuestion = getKBM(article, section).findQuestion(name);
		
		if (findQuestion == null)
			findQuestion = getKBM(article, section).findQContainer(name);
			
		if (findQuestion == null) {
			errors.add(new Message("Question not found: " + name));
			return NoopAction.INSTANCE;
		}
		
		List qasets = new ArrayList();
		
		qasets.add(findQuestion);
		
		RuleComplex rule = RuleFactory.createRule("FlowQAIndicationRule_" + System.currentTimeMillis());
		rule.setProblemsolverContext(FluxSolver.class);
		rule.setCondition(new CondAnd(new ArrayList()));

		RuleAction action = new ActionInstantIndication(rule);
		((ActionIndication) action).setQASets(qasets);
//		((RuleAction)action).setCorrespondingRule(rule);
		
		return action;
	}

	private INode createEndNode(KnowWEArticle article, String id, String flowName, Section section) {
		String name = ((Section) section.getChildren().get(1)).getOriginalText();
		
		ActionSetValue action = FlowFactory.getInstance().createSetValueAction();
		
		QuestionMC question = (QuestionMC) getKBM(article, section).findQuestion(flowName + "_" + FlowchartTerminologySubTreeHandler.EXITNODES_QUESTION_NAME);
		
		action.setQuestion(question);
		
		AnswerChoice answer = null;
		for (AnswerChoice child : question.getAllAlternatives()) {
			if (child.getText().equals(name))
				answer = (AnswerChoice) child;
			
		}
		
		if (answer == null) {
//			errors.add(new Message("No startnode  '" + flowName + "' not found in terminology of flow '" + flowName +"'."));
			return null;
		}
		
		action.setValues(new AnswerChoice[] {answer} );
		
		return FlowFactory.getInstance().createEndNode(id, name, action);
	}

	private INode createStartNode(String id, Section section) {
		String name = ((Section) section.getChildren().get(1)).getOriginalText();
		
		return FlowFactory.getInstance().createStartNode(id, name);
		
	}

}
