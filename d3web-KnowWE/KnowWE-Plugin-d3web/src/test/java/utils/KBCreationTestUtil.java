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

package utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.d3web.empiricalTesting.Finding;
import de.d3web.empiricalTesting.RatedSolution;
import de.d3web.empiricalTesting.RatedTestCase;
import de.d3web.empiricalTesting.SequentialTestCase;
import de.d3web.empiricalTesting.StateRating;
import de.d3web.empiricalTesting.TestSuite;
import de.d3web.kernel.domainModel.Answer;
import de.d3web.kernel.domainModel.Diagnosis;
import de.d3web.kernel.domainModel.DiagnosisState;
import de.d3web.kernel.domainModel.KnowledgeBase;
import de.d3web.kernel.domainModel.KnowledgeBaseManagement;
import de.d3web.kernel.domainModel.NamedObject;
import de.d3web.kernel.domainModel.NumericalInterval;
import de.d3web.kernel.domainModel.QASet;
import de.d3web.kernel.domainModel.RuleComplex;
import de.d3web.kernel.domainModel.RuleFactory;
import de.d3web.kernel.domainModel.Score;
import de.d3web.kernel.domainModel.formula.Add;
import de.d3web.kernel.domainModel.formula.Div;
import de.d3web.kernel.domainModel.formula.FormulaExpression;
import de.d3web.kernel.domainModel.formula.FormulaNumber;
import de.d3web.kernel.domainModel.formula.Mult;
import de.d3web.kernel.domainModel.formula.QNumWrapper;
import de.d3web.kernel.domainModel.formula.Sub;
import de.d3web.kernel.domainModel.qasets.QContainer;
import de.d3web.kernel.domainModel.qasets.Question;
import de.d3web.kernel.domainModel.qasets.QuestionChoice;
import de.d3web.kernel.domainModel.qasets.QuestionNum;
import de.d3web.kernel.domainModel.ruleCondition.AbstractCondition;
import de.d3web.kernel.domainModel.ruleCondition.CondAnd;
import de.d3web.kernel.domainModel.ruleCondition.CondDState;
import de.d3web.kernel.domainModel.ruleCondition.CondEqual;
import de.d3web.kernel.domainModel.ruleCondition.CondKnown;
import de.d3web.kernel.domainModel.ruleCondition.CondNot;
import de.d3web.kernel.domainModel.ruleCondition.CondNumGreater;
import de.d3web.kernel.domainModel.ruleCondition.CondOr;
import de.d3web.kernel.psMethods.heuristic.PSMethodHeuristic;
import de.d3web.kernel.psMethods.xclPattern.XCLModel;
import de.d3web.kernel.psMethods.xclPattern.XCLRelationType;
import de.d3web.kernel.supportknowledge.DCElement;
import de.d3web.kernel.supportknowledge.DCMarkup;
import de.d3web.kernel.supportknowledge.MMInfoObject;
import de.d3web.kernel.supportknowledge.MMInfoStorage;
import de.d3web.kernel.supportknowledge.MMInfoSubject;
import de.d3web.kernel.supportknowledge.Property;
import de.d3web.we.core.KnowWEEnvironment;
import de.d3web.we.d3webModule.D3webModule;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Section;
import de.d3web.we.knowRep.KnowledgeRepresentationManager;
import de.d3web.we.terminology.D3webTerminologyHandler;
import de.d3web.we.testsuite.TestsuiteSection;
import de.d3web.we.utils.KnowWEUtils;
import dummies.KnowWETestWikiConnector;

/**
 * This Class loads the KnowledgeBase which will be
 * tested.
 * 
 * Furthermore in this class the KnowledgeBase
 * against which the loaded KnowledgeBase is compared
 * is created.
 * 
 * This class is a Singleton class because this
 * insures that the KnowledgeBase is loaded only
 * once.
 * 
 * Please be careful when editing anything in here because 
 * the order of the elements does matter in the tests!
 * (especially the IDs)
 * 
 * @author Sebastian Furth
 * @see KnowledgeBaseCreationTest
 *
 */
public class KBCreationTestUtil {
	
	private static KBCreationTestUtil instance = new KBCreationTestUtil();
	private KnowledgeBase loadedKB;
	private KnowledgeBase createdKB;
	private TestSuite loadedTS;
	private TestSuite createdTS;
	private KnowledgeBaseManagement createdKBM;
	
	/**
	 * Private Constructor insures noninstantiabilty.
	 */
	private KBCreationTestUtil() {
		
		loadKnowledge();
		createKnowledge();
		
	}
	
	
	/**
	 * Returns an instance of KBCreationTestKBStorage.
	 * @return KBCreationTestKBStorage
	 */
	public static KBCreationTestUtil getInstance() {
		return instance;
	}
	
	
	/**
	 * Returns the KnowledgeBase loaded from the KnowWEArticle.
	 * @return KnowledgeBase
	 */
	public KnowledgeBase getLoadedKB() {
		return loadedKB;
	}
	
	
	/**
	 * Returns the KnowledgeBase which was created manually.
	 * @return KnowledgeBase
	 */
	public KnowledgeBase getCreatedKB() {
		return createdKB;
	}
	
	/**
	 * Returns the TestSuite loaded from the KnowWEArticle.
	 * @return TestSuite
	 */
	public TestSuite getLoadedTS() {
		return loadedTS;
	}
	
	
	/**
	 * Returns the TestSuite which was created manually.
	 * @return TestSuite
	 */
	public TestSuite getCreatedTS() {
		return createdTS;
	}
	
	
	/**
	 * Creates a KnowWEArticle and loads the created Knowledge.
	 */
	private void loadKnowledge() {

		// Read File containing content
		String content = Utils.readTxtFile("src/test/resources/KBCreationTest.txt");
		
		// Initialize KnowWE
		KnowWEEnvironment.initKnowWE(new KnowWETestWikiConnector());
		D3webTerminologyHandler d3Handler = D3webModule.getInstance().getKnowledgeRepresentationHandler("default_web");
		
		// Create Article
		KnowWEArticle article = new KnowWEArticle(content, "KBCreationTest", 
					KnowWEEnvironment.getInstance().getRootTypes(), "default_web");
		KnowWEEnvironment.getInstance().getArticleManager("default_web").saveUpdatedArticle(article);
		
		// Load KnowledgeBase
		loadedKB =  d3Handler.getKBM(article, article.getSection()).getKnowledgeBase();

		// Load TestSuite
		Section s = article.getSection().findChildOfType(TestsuiteSection.class);
		loadedTS = (TestSuite) KnowWEUtils.getStoredObject("default_web", "KBCreationTest", s.getId(), "TestsuiteSection_Testsuite");
		
	}
	
	
	/**
	 * Creates the Knowledge against which the loaded KnowledgeBase
	 * will be tested.
	 */
	private void createKnowledge() {
		
		createdKB = new KnowledgeBase();
		createdKBM = KnowledgeBaseManagement.createInstance(createdKB);
		createDiagnoses();
		createQuestionnaires();
		createQuestions();
		createAttributeTable();
		createRules();
		createXCLModels();
		createTestSuite();
		
	}
		
	

	/**
	 * Creates the Diagnoses for the KnowledgeBase.
	 */
	private void createDiagnoses() {
		
		Diagnosis p0 = new Diagnosis("P000");
			p0.setText("P000");
			createdKB.add(p0);
		
		Diagnosis p1 = new Diagnosis("P1");
			p1.setText("Mechanical problem");
			createdKB.getRootDiagnosis().addChild(p1);
			createdKB.add(p1);
		
		Diagnosis p2 = new Diagnosis("P2");
			p2.setText("Damaged idle speed system");
			p1.addChild(p2);
			createdKB.add(p2);
		
		Diagnosis p3 = new Diagnosis("P3");
			p3.setText("Leaking air intake system");
			p3.getProperties().setProperty(Property.EXPLANATION, 
					"The air intake system is leaking.");
			p1.addChild(p3);
			createdKB.add(p3);
		
		Diagnosis p4 = new Diagnosis("P4");
			p4.setText("Other problem");
			createdKB.getRootDiagnosis().addChild(p4);
			createdKB.add(p4);
						
	}
	

	/**
	 * Creates the Questionnaires for the KnowledgeBase.
	 */
	private void createQuestionnaires() {
		
		QContainer qc0 = new QContainer("Q000");
			qc0.setText("Q000");
			createdKB.add(qc0);		
		
		QContainer qc1 = new QContainer("QC1");
			qc1.setText("Observations");
			createdKB.getRootQASet().addChild(qc1);
			createdKB.add(qc1);
		
		QContainer qc2 = new QContainer("QC2");
			qc2.setText("Idle speed system");
			qc1.addChild(qc2);
			createdKB.add(qc2);
		
		QContainer qc3 = new QContainer("QC3");
			qc3.setText("Air filter");
			qc3.getProperties().setProperty(Property.EXPLANATION, 
					"Here you can enter your observations concerning the air filter.");
			qc1.addChild(qc3);
			createdKB.add(qc3);
		
		QContainer qc4 = new QContainer("QC4");
			qc4.setText("Ignition timing");
			qc1.addChild(qc4);
			createdKB.add(qc4);
		
		QContainer qc5 = new QContainer("QC5");
			qc5.setText("Technical Examinations");
			createdKB.getRootQASet().addChild(qc5);
			createdKB.add(qc5);
		
		// Set Init-Questions
		List<QContainer> initQuestions = new ArrayList<QContainer>();
		initQuestions.add(qc1);
		createdKB.setInitQuestions(initQuestions);
		
	}
	
	
	/**
	 * Creates the Question for the KnowledgeBase.
	 */
	private void createQuestions() {
		
		// Get QContainer
		// Observations
		QContainer qc1 = createdKBM.findQContainer("Observations");
		
		// Add Question:
		// - Exhaust fumes ~ "What is the color of the exhaust fumes?" [oc]
		// -- black
		// -- blue
		// -- invisible
		Question q0 = createdKBM.createQuestionOC("Exhaust fumes", qc1, 
				new String[] {"black", "blue", "invisible"});
		
		// Add MMInfo to Question "Exhaust fumes": "What is the color of the exhaust fumes?"
		addMMInfo(q0, "LT", MMInfoSubject.PROMPT.getName(), "What is the color of the exhaust fumes?");
		
		// Add question:
		// --- Fuel [oc]
		// ---- diesel
		// ---- unleaded gasoline
		createdKBM.createQuestionOC("Fuel", q0, new String[] {"diesel", "unleaded gasoline"});
						
		// Add question:
		// - "Average mileage /100km" [num] {liter} (0 30) #Q1337
		Question q1 = new QuestionNum("Q1337");
			q1.setText("Average mileage /100km");
			q1.getProperties().setProperty(Property.UNIT, "liter");
			q1.getProperties().setProperty(Property.QUESTION_NUM_RANGE, 
					new NumericalInterval(0, 30));
			createdKB.add(q1);
			qc1.addChild(q1);		
		
		// Add question:
		// -- "Num. Mileage evaluation" [num] <abstract>
		Question q2 = createdKBM.createQuestionNum("Num. Mileage evaluation", q1);
			q2.getProperties().setProperty(Property.ABSTRACTION_QUESTION, Boolean.TRUE);
			
		// Add question:
		// --- Mileage evaluation [oc] <abstract>
		// ---- normal
		// ---- increased
		Question q3 = createdKBM.createQuestionOC("Mileage evaluation", q2, 
				new String[] {"normal", "increased"});
			q3.getProperties().setProperty(Property.ABSTRACTION_QUESTION, Boolean.TRUE);

		// Add question:
		// - "Real mileage  /100km" [num]
		createdKBM.createQuestionNum("Real mileage  /100km", qc1);
		
		// Add question:
		// - Driving [mc]
		// -- insufficient power on partial load
		// -- insufficient power on full load
		// -- unsteady idle speed
		// -- everything is fine
		Question q4 = createdKBM.createQuestionMC("Driving", qc1, 
				new String[] {"insufficient power on partial load", 
							  "insufficient power on full load", 
							  "unsteady idle speed",
							  "everything is fine"});
		
		// Add question:
		// ---- Other [text]
		createdKBM.createQuestionText("Other", q4);
		
		// Get second QContainer and add Question
		// Technical Examinations
		QContainer qc2 = createdKBM.findQContainer("Technical Examinations");
		// - "Idle speed system o.k.?" [yn]
		createdKBM.createQuestionYN("Idle speed system o.k.?", qc2);	
		
	}

	/**
	 * Creates MMInfo similar to the info from an AttributeTable
	 */
	private void createAttributeTable() {
		
		// Get the Diagnosis which will get the MMInfo
		Diagnosis d = createdKBM.findDiagnosis("Mechanical problem");
		
		// Add MMInfo which is similar to the MMInfo created from the AttributeTable
		// | Mechanical problem | info | description | some problem description
		addMMInfo(d, "description", MMInfoSubject.INFO.getName(), "some problem description");
		
	}
	

	/**
	 * Creates the Rules for the KnowledgeBase
	 */
	private void createRules() {
		createDTIndicationRules();
		createDTSetValueRules();
		createDTHeuristicRules();
		createDTRefinementRules();
		createSetValueRules();
		createHeuristicRules();
		createIndicationRules();
	}
	
	
	/**
	 * Creates the Indication-Rules
	 */
	private void createDTIndicationRules() {
		
		Answer answer;
		CondEqual condition;
		String ruleID;
		QuestionChoice condQuestion = (QuestionChoice) createdKBM.findQuestion("Exhaust fumes");
		QuestionChoice actionQuestion = (QuestionChoice) createdKBM.findQuestion("Fuel");
		
		// Create Rule R1:
		// - Exhaust fumes [oc]
		// -- black
		// --- Fuel [oc]
		answer = createdKBM.findAnswer(condQuestion, "black");
		condition = new CondEqual(condQuestion, answer);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createIndicationRule(ruleID, actionQuestion, condition);
		
		// Create Rule R2:
		// - Exhaust fumes [oc]
		// -- blue
		// --- &REF Fuel
		answer = createdKBM.findAnswer(condQuestion, "blue");
		condition = new CondEqual(condQuestion, answer);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createIndicationRule(ruleID, actionQuestion, condition);
		
		// Create Rule R3:
		// - Exhaust fumes [oc]
		// -- invisible
		// --- &REF Fuel
		answer = createdKBM.findAnswer(condQuestion, "invisible");
		condition = new CondEqual(condQuestion, answer);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createIndicationRule(ruleID, actionQuestion, condition);
		
	}
	
	
	/**
	 * Creates the SetValue Rules from the DecisionTree
	 */
	private void createDTSetValueRules() {
		
		// Create Rule R4:
		// - Driving [mc]
        // -- insufficient power on partial load
        // --- "Num. Mileage evaluation" SET (110)
		QuestionChoice q1 = (QuestionChoice) createdKBM.findQuestion("Driving");
		Answer a1 = createdKBM.findAnswer(q1, "insufficient power on partial load");
		CondEqual c1 = new CondEqual(q1, a1);
		String ruleID = createdKBM.findNewIDFor(new RuleComplex());
		QuestionNum q2 = (QuestionNum) createdKBM.findQuestion("Num. Mileage evaluation");
		FormulaNumber fn1 = new FormulaNumber(110.0);
		FormulaExpression f1 = new FormulaExpression(q2, fn1);
		RuleFactory.createSetValueRule(ruleID, q2, f1, c1);
		
		// Create Rule R5:
		// - Driving [mc]
        // -- insufficient power on full load
        // --- "Num. Mileage evaluation" (20)
		Answer a2 = createdKBM.findAnswer(q1, "insufficient power on full load");
		CondEqual c2 = new CondEqual(q1, a2);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		QuestionNum q3 = (QuestionNum) createdKBM.findQuestion("Num. Mileage evaluation");
		FormulaNumber fn2 = new FormulaNumber(20.0);
		FormulaExpression f2 = new FormulaExpression(q3, fn2);
		RuleFactory.createAddValueRule(ruleID, q3, new Object[] { f2 }, c2);
	}


	/**
	 * Creates the Heuristic Rules from the DecisionTree
	 */
	private void createDTHeuristicRules() {
		
		// Create Rule R6:
		// - Driving [mc]
		// -- everything is fine
		// --- Other problem (P7)
		QuestionChoice condQuestion = (QuestionChoice) createdKBM.findQuestion("Driving");
		Answer answer = createdKBM.findAnswer(condQuestion, "everything is fine");
		CondEqual condition = new CondEqual(condQuestion, answer);
		String ruleID = createdKBM.findNewIDFor(new RuleComplex());
		Diagnosis diag = createdKBM.findDiagnosis("Other problem");
		RuleFactory.createHeuristicPSRule(ruleID, diag, Score.P7, condition);
		
	}
	
	/**
	 * Creates the Refinement Rules from the DecisionTree
	 */
	private void createDTRefinementRules() {
		
		// Create Rule R7:
		// --- Other problem (P7)
		// ---- Other [text]
		Question question = createdKBM.findQuestion("Other");
		List<Question> action = new ArrayList<Question>();
		action.add(question);
		String ruleID = createdKBM.findNewIDFor(new RuleComplex());
		Diagnosis diag = createdKBM.findDiagnosis("Other problem");
		CondDState condition = new CondDState(diag, 
				DiagnosisState.ESTABLISHED, PSMethodHeuristic.class);
		RuleFactory.createRefinementRule(ruleID, action, diag, condition);
		
	}

	
	/**
	 * Creates the Set-Value Rules corresponding to
	 * the rules of the Rules-section
	 */
	private void createSetValueRules() {
			
		// Create first Condition:
		// KNOWN["Real mileage  /100km"]
		QuestionNum q11 = (QuestionNum) createdKBM.findQuestion("Real mileage  /100km");
		CondKnown c11 = new CondKnown(q11);
		
		// Create second Condition: 
		// "Average mileage /100km" > 0
		QuestionNum q12 = (QuestionNum) createdKBM.findQuestion("Average mileage /100km");
		CondNumGreater c12 = new CondNumGreater(q12, 0.0);
		
		// Create AND Condition:
		// "Average mileage /100km" > 0 AND KNOWN["Real mileage  /100km"]
		List<AbstractCondition> conditions = new LinkedList<AbstractCondition>();
		conditions.add(c11);
		conditions.add(c12);
		CondAnd c1 = new CondAnd(conditions);
		
		// Create Rule R8:
		// IF "Average mileage /100km" > 0 AND KNOWN["Real mileage  /100km"]
		// THEN "Num. Mileage evaluation" = (("Real mileage  /100km" / "Average mileage /100km") * 100.0)
		String ruleID = createdKBM.findNewIDFor(new RuleComplex());
		QuestionNum q3 = (QuestionNum) createdKBM.findQuestion("Num. Mileage evaluation");
		Div d = new Div(new QNumWrapper(q11), new QNumWrapper(q12));
		FormulaNumber fn = new FormulaNumber(100.0);
		Mult m = new Mult(d, fn);
		FormulaExpression f = new FormulaExpression(q3, m);
		RuleFactory.createSetValueRule(ruleID, q3, f, c1);
		
		// Create Rule R9:
		// IF "Num. Mileage evaluation" > 130
		// THEN Mileage evaluation = increased
		CondNumGreater c2 = new CondNumGreater(q3, 130.0);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		Question q4 = createdKBM.findQuestion("Mileage evaluation");
		Answer a = createdKBM.findAnswer(q4, "increased");
		RuleFactory.createSetValueRule(ruleID, q4, new Object[] { a }, c2);
		
		// Create rule R10:
		// IF Driving = unsteady idle speed
		// THEN "Real mileage  /100km" += ( 2 )
		QuestionChoice questionIf4 = (QuestionChoice) createdKBM.findQuestion("Driving");
		Answer answerIf4 = createdKBM.findAnswer(questionIf4, "unsteady idle speed");
		CondEqual conditionIf4 = new CondEqual(questionIf4, answerIf4);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		QuestionNum qnum = (QuestionNum) createdKBM.findQuestion("Real mileage  /100km");
		FormulaNumber fn2 = new FormulaNumber(2.0);
		Add add = new Add(new QNumWrapper(qnum), fn2);
		FormulaExpression f2 = new FormulaExpression(qnum, add);
		RuleFactory.createSetValueRule(ruleID, qnum, f2, conditionIf4);
		
		// Create Rule R11:
		// IF Driving = insufficient power on full load
		// THEN "Real mileage  /100km" = ("Average mileage /100km" + 2)
		QuestionChoice questionIf5 = (QuestionChoice) createdKBM.findQuestion("Driving");
		Answer answerIf5 = createdKBM.findAnswer(questionIf5, "insufficient power on full load");
		CondEqual conditionIf5 = new CondEqual(questionIf5, answerIf5);
		QuestionNum questionFormula = (QuestionNum) createdKBM.findQuestion("Average mileage /100km");
		FormulaNumber fn3 = new FormulaNumber(2.0);
		Add addition = new Add(new QNumWrapper(questionFormula), fn3);
		QuestionNum questionThen = (QuestionNum) createdKBM.findQuestion("Real mileage  /100km");
		FormulaExpression f3 = new FormulaExpression(questionThen, addition);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createSetValueRule(ruleID, questionThen, f3, conditionIf5);
		
		// Create Rule R12:
		// IF Driving = insufficient power on partial load
		// THEN "Real mileage  /100km" = ("Average mileage /100km" - 1)
		QuestionChoice questionIf6 = (QuestionChoice) createdKBM.findQuestion("Driving");
		Answer answerIf6 = createdKBM.findAnswer(questionIf6, "insufficient power on partial load");
		CondEqual conditionIf6 = new CondEqual(questionIf6, answerIf6);
		QuestionNum questionFormula2 = (QuestionNum) createdKBM.findQuestion("Average mileage /100km");
		FormulaNumber fn4 = new FormulaNumber(1.0);
		Sub subtraction = new Sub(new QNumWrapper(questionFormula2), fn4);
		QuestionNum questionThen2 = (QuestionNum) createdKBM.findQuestion("Real mileage  /100km");
		FormulaExpression f4 = new FormulaExpression(questionThen2, subtraction);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createSetValueRule(ruleID, questionThen2, f4, conditionIf6);
		
	}
	
	/**
	 * Creates the Heuristic Rules corresponding to
	 * the rules of the Rules-section
	 */
	private void createHeuristicRules() {
		
		// Create rule R13:
		// IF Exhaust fumes = black EXCEPT Fuel = diesel
		// THEN Air filter (P7)
		QuestionChoice questionIf = (QuestionChoice) createdKBM.findQuestion("Exhaust fumes");
		Answer answerIf = createdKBM.findAnswer(questionIf, "black");
		CondEqual conditionIf = new CondEqual(questionIf, answerIf);
		
		QuestionChoice questionExc = (QuestionChoice) createdKBM.findQuestion("Fuel");
		Answer answerExc = createdKBM.findAnswer(questionExc, "diesel");
		CondEqual conditionExc = new CondEqual(questionExc, answerExc);
		
		String ruleID = createdKBM.findNewIDFor(new RuleComplex());
		Diagnosis diag = createdKBM.findDiagnosis("Mechanical problem");
		RuleFactory.createHeuristicPSRule(ruleID, diag, Score.P7, conditionIf, conditionExc);
		
		
		// Create rule R14:
		// IF NOT Fuel = unleaded gasoline OR NOT Exhaust fumes = black 
		// THEN Mechanical problem = N7
		QuestionChoice questionIf2 = (QuestionChoice) createdKBM.findQuestion("Fuel");
		Answer answerIf2 = createdKBM.findAnswer(questionIf2, "unleaded gasoline");
		CondEqual conditionIf2 = new CondEqual(questionIf2, answerIf2);
		CondNot condNot1 = new CondNot(conditionIf2);
		
		QuestionChoice questionIf3 = (QuestionChoice) createdKBM.findQuestion("Exhaust fumes");
		Answer answerIf3 = createdKBM.findAnswer(questionIf3, "black");
		CondEqual conditionIf3 = new CondEqual(questionIf3, answerIf3);
		CondNot condNot2 = new CondNot(conditionIf3);
		
		List<AbstractCondition> conditions = new LinkedList<AbstractCondition>();
		conditions.add(condNot2);
		conditions.add(condNot1);
		CondOr condOr = new CondOr(conditions);
		
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createHeuristicPSRule(ruleID, diag, Score.N7, condOr);		
		
	}

	/**
	 * Creates the Indication-Rules
	 */
	private void createIndicationRules() {
		
		String ruleID;
		
		// Create Rule R15:
		// IF Driving = unsteady idle speed
		// THEN Technical Examinations
		QuestionChoice condQuestion = (QuestionChoice) createdKBM.findQuestion("Driving");
		QASet actionQuestion = createdKBM.findQContainer("Technical Examinations");
		Answer answer = createdKBM.findAnswer(condQuestion, "unsteady idle speed");
		CondEqual condition = new CondEqual(condQuestion, answer);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createIndicationRule(ruleID, actionQuestion, condition);
		
		// Create Rule R16:
		// IF Mileage evaluation = increased
		// THEN "Idle speed system o.k.?"; Driving
		QuestionChoice condQuestion2 = (QuestionChoice) createdKBM.findQuestion("Mileage evaluation");
		QuestionChoice actionQuestion2 = (QuestionChoice) createdKBM.findQuestion("Idle speed system o.k.?");
		QuestionChoice actionQuestion3 = (QuestionChoice) createdKBM.findQuestion("Driving");
		Answer answer2 = createdKBM.findAnswer(condQuestion2, "increased");
		CondEqual condition2 = new CondEqual(condQuestion2, answer2);
		List<QASet> nextQuestions = new ArrayList<QASet>();
		nextQuestions.add(actionQuestion2);
		nextQuestions.add(actionQuestion3);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createIndicationRule(ruleID, nextQuestions, condition2);
		
		// Create Rule R17:
		// IF KNOWN[Other]
		// THEN Technical Examinations
		Question condQuestion3 = createdKBM.findQuestion("Other");
		QASet actionQuestion4 = createdKBM.findQContainer("Technical Examinations");
		CondKnown condition3 = new CondKnown(condQuestion3);
		ruleID = createdKBM.findNewIDFor(new RuleComplex());
		RuleFactory.createIndicationRule(ruleID, actionQuestion4, condition3);
		
	}
	
	/**
	 * Creats a XCLModel similar to the one which is created
	 * in the KnowWEArticle
	 */
	private void createXCLModels() {
			
		Diagnosis d = createdKBM.findDiagnosis("Damaged idle speed system");

	    // "Idle speed system o.k.?" = Yes [--]
		Question q1 = createdKBM.findQuestion("Idle speed system o.k.?");
		Answer  a1 = createdKBM.findAnswer(q1, "Yes");
		CondEqual c1 = new CondEqual(q1, a1);
		XCLModel.insertXCLRelation(createdKB, c1, d, XCLRelationType.contradicted);
		
	    // Driving = unsteady idle speed [!]
		Question q2 = createdKBM.findQuestion("Driving");
		Answer  a2 = createdKBM.findAnswer(q2, "unsteady idle speed");
		CondEqual c2 = new CondEqual(q2, a2);
	    XCLModel.insertXCLRelation(createdKB, c2, d, XCLRelationType.requires);
		
	    // "Idle speed system o.k.?" = no [++]
		Question q3 = createdKBM.findQuestion("Idle speed system o.k.?");
		Answer  a3 = createdKBM.findAnswer(q3, "No");
		CondEqual c3 = new CondEqual(q3, a3);
		XCLModel.insertXCLRelation(createdKB, c3, d, XCLRelationType.sufficiently);
		
		// Mileage evaluation = increased [3]
		Question q4 = createdKBM.findQuestion("Mileage evaluation");
		Answer  a4 = createdKBM.findAnswer(q4, "increased");
		CondEqual c4 = new CondEqual(q4, a4);
		XCLModel.insertXCLRelation(createdKB, c4, d, XCLRelationType.explains, 3.0);
		
		// Exhaust fumes = black
		Question q5 = createdKBM.findQuestion("Exhaust fumes");
		Answer  a5 = createdKBM.findAnswer(q5, "black");
		CondEqual c5 = new CondEqual(q5, a5);
		XCLModel.insertXCLRelation(createdKB, c5, d, XCLRelationType.explains);
		
	}
	
	/**
	 * Creats a TestSuite similar to the one which is created
	 * in the KnowWEArticle
	 */
	private void createTestSuite() {
		
		// Create Finding
		Question q = createdKBM.findQuestion("Driving");
		Answer a = createdKBM.findAnswer(q, "everything is fine");
		Finding f = new Finding(q, a);
		
		// Create RatedSolution
		Diagnosis d = createdKBM.findDiagnosis("Other problem");
		StateRating sr = new StateRating(DiagnosisState.ESTABLISHED);
		RatedSolution rs = new RatedSolution(d, sr);
		
		// Add Finding and RatedSolution to RatedTestCase
		RatedTestCase rtc = new RatedTestCase();
		rtc.add(f);
		rtc.addExpected(rs);
		rtc.setName("STC1_RTC1");
		
		// Add RatedTestCase to SequentialTestCase
		SequentialTestCase stc = new SequentialTestCase();
		stc.add(rtc);
		stc.setName("STC1");
		
		// Add SequentialTestCase to the repository
		List<SequentialTestCase> repository = new ArrayList<SequentialTestCase>();
		repository.add(stc);
		
		// Create testSuite
		TestSuite t = new TestSuite();
		t.setKb(createdKB);
		t.setRepository(repository);
		createdTS = t;
		
	}
	
	/**
	 * Adds a MMInfo to the NamedObject
	 * @param o NamedObject
	 * @param title String
	 * @param subject String
	 * @param content String
	 */
	private void addMMInfo(NamedObject o, String title, String subject,
			String content) {

		MMInfoStorage mmis;
		DCMarkup dcm = new DCMarkup();
		dcm.setContent(DCElement.TITLE, title);
		dcm.setContent(DCElement.SUBJECT, subject);
		dcm.setContent(DCElement.SOURCE, o.getId());
		MMInfoObject mmi = new MMInfoObject(dcm, content);
		mmis = new MMInfoStorage();
		o.getProperties().setProperty(Property.MMINFO, mmis);
		mmis.addMMInfo(mmi);
		
	}

}
