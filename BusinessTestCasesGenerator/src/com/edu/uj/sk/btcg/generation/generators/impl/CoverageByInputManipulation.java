package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.UserTask;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.logic.VariableValueExtractor;
import com.edu.uj.sk.btcg.scripting.GroovyEvaluator;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class CoverageByInputManipulation implements IGenerator {
	private VariableValueExtractor variableValueExtractor = VariableValueExtractor.create();

	@Override
	public Iterator<BpmnModel> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}

	
	private class It extends AbstractGenerationIterator {
		// Map<UserTask Id, Map<Variable Name, Index>
		private Map<String, Map<String, Integer>> userTaskToVariableIndexMap = new HashMap<>();
		private List<List<Object>> allVariablesValuesPermutations;
		
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			Map<String, Multimap<String, Object>> userTaskVariableValueMap = new HashMap<>();
			
			Stack<Pair<FlowElement, UserTask>> contexts = new Stack<>(); 
			contexts.add(Pair.of(selectStartEvent(originalModel), null));
			
			while (!contexts.isEmpty()) {
				Pair<FlowElement, UserTask> context = contexts.pop();
				FlowElement element = context.getKey();
				UserTask currentUserTask = context.getValue();
				
				
				
				if (element instanceof UserTask) {
					UserTask userTask = (UserTask)element;
					
					Multimap<String, Object> variableValueMap = HashMultimap.create();
					
					if (currentUserTask != null) {
						variableValueMap = HashMultimap.create(
							userTaskVariableValueMap.get(currentUserTask.getId())
						);
					}
						
					userTaskVariableValueMap
						.put(userTask.getId(), variableValueMap);
					
					currentUserTask = userTask;
					
				} else if (element instanceof SequenceFlow) {
					SequenceFlow sequenceFlow = (SequenceFlow) element;
					
					if (currentUserTask != null && !StringUtils.isBlank(sequenceFlow.getConditionExpression())) {
						String userTaskId = currentUserTask.getId();
						
						
						Multimap<String, Object> variableValueMap = 
							variableValueExtractor
								.extractVariableValueMap(sequenceFlow.getConditionExpression());
						
							userTaskVariableValueMap
								.get(userTaskId)
								.putAll(variableValueMap);
						
					}
					
					FlowElement target = originalModel.getFlowElement(sequenceFlow.getTargetRef());
					contexts.push(Pair.of(target, currentUserTask));
					
					continue;
				}
				
				if (element instanceof FlowNode) {
					FlowNode flowNode = (FlowNode) element;
					
					for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
						contexts.push(Pair.of(sequenceFlow, currentUserTask));
					}
				}
			}
			
			
			
			
			List<Collection<Object>> values = Lists.newArrayList();
			
			for (String userTask : userTaskVariableValueMap.keySet()) {
				Multimap<String, Object> variableValuesMap = userTaskVariableValueMap.get(userTask);
				Map<String, Integer> variableIndexMap = new HashMap<>();
				
				for (String variable : variableValuesMap.keySet()) {
					Integer index = values.size();
					values.add(new ArrayList<Object>(variableValuesMap.get(variable)));
					
					variableIndexMap.put(variable, index);
				}
				
				
				userTaskToVariableIndexMap.put(userTask, variableIndexMap);
			}
			
			allVariablesValuesPermutations = CCollections.allCombinations(values);
		}


		@Override
		public boolean hasNext() {
			return !allVariablesValuesPermutations.isEmpty();
		}

		@Override
		public BpmnModel next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);
			Map<FlowElement, Boolean> visitedElements = new HashMap<>();
			
			List<Object> currentVariablesValues = allVariablesValuesPermutations.remove(0);
			
			Stack<Context> contexts = new Stack<>();
			contexts.push(new Context(selectStartEvent(currentTestCase)));
			
			while (!contexts.isEmpty()) {
				Context context = contexts.pop();
				
				if (visitedElements.containsKey(context.element)) continue;
				visitedElements.put(context.element, Boolean.TRUE);
				
				
				if (context.element instanceof UserTask) {
					UserTask userTask = (UserTask)context.element;
					
					Map<String, Object> variableValueMap = getInputFor(userTask, currentVariablesValues);

					// override current values
					context.variableValueMap.putAll(variableValueMap);
					
					if (!userTask.getFormProperties().isEmpty()) {
						String text = createAnnotationTextForUserTask(userTask, variableValueMap);
						createAnnotationForElement(currentTestCase, text, userTask);
					}
					
				} else if (context.element instanceof SequenceFlow) {
					SequenceFlow sequenceFlow = (SequenceFlow)context.element;
					
					String condition = sequenceFlow.getConditionExpression();
					
					Boolean conditionValue = true;
					if (!StringUtils.isBlank(condition)) {
						try {
							conditionValue = (Boolean)GroovyEvaluator
									.evaluate(condition, context.variableValueMap);
							
						} catch (Throwable e) {
							conditionValue = false;
							FlowElement element = 
									currentTestCase.getFlowElement(sequenceFlow.getSourceRef());
							
							createAnnotationForElement(currentTestCase, "Exception: " + e.getMessage(), element);
						}
					}
					
					if (!conditionValue) {
						removeSequenceFlow(currentTestCase, sequenceFlow);
					} else {
						FlowElement element = currentTestCase.getFlowElement(sequenceFlow.getTargetRef());
						contexts.push(new Context(element, context.variableValueMap));
					}
					
					continue;
				} 
				
				if (context.element instanceof FlowNode) {
					FlowNode flowNode = (FlowNode) context.element;
					
					for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
						contexts.push(new Context(sequenceFlow, context.variableValueMap));
					}
				}
			}
			
			removeUnconnectedElements(currentTestCase);
			return currentTestCase;
		}

		
		
		
		private Map<String, Object> getInputFor(
				UserTask userTask,
				List<Object> currentVariablesValues) {
			
			Map<String, Object> variableValueMap = new HashMap<>();
			
			Map<String, Integer> variableIndexMap = 
					userTaskToVariableIndexMap.get(userTask.getId());			
					
			for (String variable : variableIndexMap.keySet()) {
				Integer index = variableIndexMap.get(variable);
				Object value = currentVariablesValues.get(index);
				
				variableValueMap.put(variable, value);
			}
			
			return variableValueMap;
		}

		
		
		private StartEvent selectStartEvent(BpmnModel currentTestCase) {
			List<StartEvent> startEvents = selectAllMainProcessFlowElementsOfType(currentTestCase, StartEvent.class);
			
			Preconditions.checkArgument(!startEvents.isEmpty());
			
			return startEvents.get(0);
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
		private String createAnnotationTextForUserTask(
				UserTask userTask,
				Map<String, Object> variableValueMap) {
			
			StringBuilder annotationText = new StringBuilder("User submits variables:    ");
			List<FormProperty> formProperties = userTask.getFormProperties();
			
			for (String variable : variableValueMap.keySet()) {
				if (formContainsVariable(formProperties, variable)) {
					annotationText
						.append(variable)
						.append(" = <")
						.append(variableValueMap.get(variable))
						.append(">,   ");
				}
			}
			
			return annotationText.toString();
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
		
		
		

		
		
		
		
		
		class Context {
			public Context(FlowElement element) {
				this.element = element;
			}
			
			
			
			public Context(FlowElement element,
					Map<String, Object> variablesValues) {
				this.element = element;
				this.variableValueMap = new HashMap<>(variablesValues);
			}



			public FlowElement element;
			public Map<String, Object> variableValueMap = new HashMap<>();
		}
		
	}


	
	
	
	
	public VariableValueExtractor getVariableValueExtractor() {
		return variableValueExtractor;
	}


	public void setVariableValueExtractor(
			VariableValueExtractor variableValueExtractor) {
		this.variableValueExtractor = variableValueExtractor;
	}
	
	
	
	
}
