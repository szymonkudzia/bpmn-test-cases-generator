package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExtensionElement;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.Gateway;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.apache.commons.lang.StringUtils;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.logic.BooleanExpressionNode;
import com.edu.uj.sk.btcg.logic.BooleanExpressionTree;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;

public class CoverageByConditionEvaluation implements IGenerator {

	@Override
	public Iterator<BpmnModel> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}

	class It extends AbstractGenerationIterator {
		private List<SequenceFlow> connections;
		private SequenceFlow connection;
		private List<String> variables = Lists.newArrayList();
		private List<List<Object>> valuesPermutations = Lists.newArrayList();
		
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			connections = selectAllConnectionsWithConditions(originalModel);
		}

		@Override
		public boolean hasNext() {
			if (!valuesPermutations.isEmpty()) return true;
			
			return !connections.isEmpty();
		}

		@Override
		public BpmnModel next() {
			if (valuesPermutations.isEmpty()) {
				connection = connections.remove(0);
				
				String condition = cleanCondition(connection.getConditionExpression());
				BooleanExpressionTree expressionTree = BooleanExpressionTree.create(condition);
				HashMultimap<String, Object> variablesValues = determineVariablesBoundaryValues(expressionTree);
				
				List<Collection<Object>> possibleValues = Lists.newArrayList(variablesValues.asMap().values());
				
				variables = Lists.newArrayList(variablesValues.keySet());
				valuesPermutations = CCollections.allCombinations(possibleValues);
			}
			
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);
			SequenceFlow currentConnection = getFlowElement(currentTestCase, connection);
			
			List<Object> currentValuesCombination = Lists.newArrayList(valuesPermutations.remove(0));
			
			FlowNode node = (FlowNode)currentTestCase.getFlowElement(currentConnection.getSourceRef());
			Stack<FlowNode> nodesToVisite = new Stack<FlowNode>();
			nodesToVisite.push(node);
			
			while (!nodesToVisite.isEmpty() && (node = nodesToVisite.pop()) != null) {
				boolean appendAnnotation = false;
				StringBuilder annotationText = new StringBuilder("User submits variables:    ");
				
				if (node instanceof UserTask) {
					appendAnnotation = 
						createAnnotationTextForUserTask(
							currentValuesCombination, 
							node, 
							annotationText);
				}

				if (appendAnnotation)
					createAnnotationForElement(currentTestCase,	annotationText.toString(), node);
				
				addNodesToVisit(currentTestCase, node, nodesToVisite);
			}
			
			
			return currentTestCase;
		}

		





		



		private List<SequenceFlow> selectAllConnectionsWithConditions(BpmnModel model) {
			return selectAllMainProcessFlowElementsOfType(model, Gateway.class)
				.stream()
				.map(gateway -> gateway.getOutgoingFlows())
				.reduce(
					Lists.newArrayList(), 
					(accumulator, connection) -> {
						accumulator.addAll(connection); 
						return accumulator;
					})
				.stream()
				.filter(connection -> !StringUtils.isBlank(connection.getConditionExpression()) )
				.collect(Collectors.toList());
		}
		
		
		
		private String cleanCondition(String conditionExpression) {
			return conditionExpression
				.replaceAll("^return (.*)", "$1")
				.replaceFirst("(.*);$", "$1");
		}
		
		


		private HashMultimap<String, Object> determineVariablesBoundaryValues(
			BooleanExpressionTree tree) {
			HashMultimap<String, Object> result = HashMultimap.create();
			
			determinaBoundaryValues(result, tree.getRoot());			
			
			return result;
		}
		
		private void determinaBoundaryValues(HashMultimap<String, Object> possibleValues, Optional<BooleanExpressionNode> node) {
			while (node.isPresent()) {
				if (isVarialbe(node.get().getLeft())) {
					getBoundaryValues(
							possibleValues, 
							node.get().getLeft().get().getValue(), 
							node.get().getValue(), 
							node.get().getRight().get());
					
					node = node.get().getRight();
					
				} else if (isVarialbe(node.get().getRight())) {
					getBoundaryValues(
							possibleValues, 
							node.get().getRight().get().getValue(), 
							node.get().getValue(), 
							node.get().getLeft().get());
					
					node = node.get().getLeft();
				} else {
					determinaBoundaryValues(possibleValues, node.get().getLeft());
					determinaBoundaryValues(possibleValues, node.get().getRight());
					
					node = Optional.empty();
				}
			}
		}
		
		
		
		private boolean isVarialbe(Optional<BooleanExpressionNode> left) {
			if (!left.isPresent()) return false;
			
			String value = left.get().getValue();
			if (isNumber(value)) return false;
			if (isOperator(value)) return false; 
			if ("null".equals(value)) return false;
			
			return true;
		}

		private boolean isOperator(String value) {
			return Lists.newArrayList(">", "<", ">=", "<=", "&&", "||", "==").contains(value);
		}

		private boolean isNumber(String value) {
			return value.matches("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$");
		}
		
		
		/**
		 * Get possible values of variables used in connection condition
		 * 
		 * @param possibleValues
		 * @param variable
		 * @param operator
		 * @param sibling
		 */
		private void getBoundaryValues(
				HashMultimap<String, Object> possibleValues,
				String variable, 
				String operator,
				BooleanExpressionNode sibling) {

			if (Lists.newArrayList("&&", "||").contains(operator)) {
				possibleValues.put(variable, Boolean.TRUE);
				possibleValues.put(variable, Boolean.FALSE);
			} else {
				String value = sibling.getValue();
				
				if (isOperator(sibling.getValue())) {
					// TODO evaluate sibling
				}
				
				if (isNumber(value)) {
 					Double dval = Double.parseDouble(value);
					
					possibleValues.put(variable, dval + 1);
					possibleValues.put(variable, dval);
					possibleValues.put(variable, dval - 1);
				} else {
					possibleValues.put(variable, value);
					possibleValues.put(variable, Base64.getEncoder().encodeToString(value.getBytes()));
				}
				
			}
		}
		
		
		
		
		
		/**
		 * Create annotation text for UserTask containing 
		 * all input values for variables defined in input 
		 * form of UserTask
		 * 
		 * @param currentValuesCombination
		 * @param node
		 * @param annotationText
		 * @return
		 */
		private boolean createAnnotationTextForUserTask(
				List<Object> currentValuesCombination, 
				FlowNode node,
				StringBuilder annotationText) {
			
			
			boolean appendAnnotation = false;
			List<FormProperty> formProperties = getFormProperties(node);
			
			for (int i = 0; i < variables.size(); ++i) {
				if (formContainsVariable(formProperties, variables.get(i))) {
					annotationText
						.append(variables.get(i))
						.append(" = <")
						.append(currentValuesCombination.get(i))
						.append(">,   ");
					
					appendAnnotation = true;
				}
			}
			return appendAnnotation;
		}
		
		
		
		
		
		/**
		 * Get ExtenstionElements from UserTask which are 
		 * interpreted as input form 
		 * 
		 * @param node
		 * @return
		 */
		private List<FormProperty> getFormProperties(FlowNode node) {
			UserTask userTask = (UserTask)node;
			List<FormProperty> formProperties = userTask.getFormProperties();
			
			
			return formProperties;
		}
		
		
		/**
		 * Get all incoming SequenceFlow's sources as new nodes to visit
		 * 
		 * @param currentTestCase
		 * @param node
		 * @param nodesToVisite
		 */
		private void addNodesToVisit(BpmnModel currentTestCase, FlowNode node,
				Stack<FlowNode> nodesToVisite) {
			for (SequenceFlow flow : node.getIncomingFlows()) {
				FlowElement element = currentTestCase.getFlowElement(flow.getSourceRef());
				
				if (element instanceof FlowNode) {
					nodesToVisite.push((FlowNode)element);
				}
			}
		}
		
		
		
		/**
		 * Check if given list of ExtensionElement's (input form @formProperties)
		 * contains input for variable with name @variable
		 * 
		 * @param formProperties
		 * @param variable
		 * @return
		 */
		private boolean formContainsVariable(
				List<FormProperty> formProperties, String variable) {
			
			for (FormProperty property : formProperties) {
				if (property.getName().equals(variable)) return true;
			}
			
			return false;
		}
		
		
		
		
		
		
		
		


	
	}
}
