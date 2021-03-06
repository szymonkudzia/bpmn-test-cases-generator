package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Event;
import org.activiti.bpmn.model.EventDefinition;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FormProperty;
import org.activiti.bpmn.model.IntermediateCatchEvent;
import org.activiti.bpmn.model.Message;
import org.activiti.bpmn.model.MessageEventDefinition;
import org.activiti.bpmn.model.ScriptTask;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.Task;
import org.activiti.bpmn.model.UserTask;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnGraphTraversalWithDefaultElementMarking;
import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.logic.VariableValueExtractorFromBpmnModel;
import com.edu.uj.sk.btcg.scripting.GroovyEvaluator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class CoverageByInputManipulation implements IGenerator {
	private VariableValueExtractorFromBpmnModel 
	variableValueExtractor = VariableValueExtractorFromBpmnModel.create();

	
	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}

	
	
	
	
	@Override
	public boolean allTestRequirementsCovered(BpmnModel model,
			List<GenerationInfo> generationInfos) {
		
		List<String> allTestRequirements = allTestRequirements(model);
		
		if (allTestRequirements.isEmpty()) return true;
		
		List<String> coveredTestRequirements = coveredTestRequirements(generationInfos);
		
		
		
		return coveredTestRequirements.containsAll(allTestRequirements);
	}

	@Override
	public int countCoveredTestRequirementsNumber(BpmnModel model,
			List<GenerationInfo> currentInfoSet) {
		List<String> allTestRequirements = allTestRequirements(model);
		int allCount = allTestRequirements.size();
		
		allTestRequirements.removeAll(coveredTestRequirements(currentInfoSet));
		
		return allCount - allTestRequirements.size();
	}




	private List<String> coveredTestRequirements(
			List<GenerationInfo> generationInfos) {
		List<String> coveredTestRequirements =
				Lists.newArrayList(
					generationInfos.stream()
						.filter(i -> i instanceof InputManipulationInfo)
						.map(i -> (InputManipulationInfo) i)
						.map(i -> Sets.newHashSet(i.variableValue))
						.reduce(Sets.newHashSet(), (acc, vv) -> {
							acc.addAll(vv);
							return acc;
						})
				);
		return coveredTestRequirements;
	}





	private List<String> allTestRequirements(BpmnModel model) {
		List<String> allTestRequirements =
				new It(model).getAllTestRequirements();
		return allTestRequirements;
	}





	private class It extends AbstractGenerationIterator {
		// Map<FlowElement Id, Map<Variable Name, Index>
		private Map<String, Map<String, Integer>> idVariableIndexMap = Maps.newHashMap();
		private List<List<Object>> allValueCombinations;
		
		private Map<Integer, String> indexVariableMap = Maps.newHashMap();
		
		// concatenated variable name and value
		private Set<String> testRequirement = Sets.newHashSet();
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			Map<String, Multimap<String, Object>>
			userTaskVariableValueMap = variableValueExtractor.extract(originalModel);
			
			calculateIndexAndValuesCombination(userTaskVariableValueMap);
			
			for(Multimap<String, Object> variableValue : userTaskVariableValueMap.values()) {
				for (String variable : variableValue.keySet()) {
					for (Object value : variableValue.get(variable)) {
						testRequirement.add(variable + "_" + value);
					}
				}
			}
		}
		
		
		public List<String> getAllTestRequirements() {
			List<String> list = Lists.newArrayList(testRequirement);
			Collections.sort(list);
			return list;
		}



		@Override
		public boolean hasNext() {
			return !allValueCombinations.isEmpty();
		}

		
		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);
			List<Object> currentValuesCombination = allValueCombinations.remove(0);
			
			Traverser traverser = new Traverser(currentValuesCombination);
			
			traverser.traverse(currentTestCase);
			
			BpmnQueries.removeUnconnectedElements(currentTestCase);
			
			Set<String> variableValue = Sets.newHashSet();
			for (int i = 0; i < currentValuesCombination.size(); ++i) {
				Object value = currentValuesCombination.get(i);
				String variable = indexVariableMap.get(i);
				
				variableValue.add(variable + "_" + value);
			}
			
			return Pair.of(currentTestCase, InputManipulationInfo.create(variableValue));
		}
		
		
		
		
		private class Traverser extends BpmnGraphTraversalWithDefaultElementMarking<Traverser.Context> {
			private String elementIdToSkip = "";
			private Context currentContext;
			private List<Object> currentValuesCombination;
			
			public Traverser(List<Object> valuesCombinationMap) {
				currentValuesCombination = valuesCombinationMap;
			}
			
			@Override
			protected void doProcessing(Context context, BpmnModel model) {
				elementIdToSkip = "";
				currentContext = context;
				
				
				if (isVariableModifier(context.element)) {
					overrideCurrentVariableValues(context, context.element);
					
					annotateWithCurrentInputChanges
						(model, context.element, context.variableValueMap);
					
				} else if (context.element instanceof SequenceFlow) {
					SequenceFlow sequenceFlow = (SequenceFlow)context.element;
					
					String condition = sequenceFlow.getConditionExpression();
					
					if (StringUtils.isBlank(condition)) return;
					
					
					Boolean conditionValue = true;
					try {
						conditionValue = (Boolean)
							GroovyEvaluator.evaluate(condition, context.variableValueMap);
						
					} catch (Throwable e) {
						conditionValue = false;
						annotateWithException(model, sequenceFlow, e);
					}
					
					if (conditionValue == null || !conditionValue) {
						BpmnQueries.removeSequenceFlow(model, sequenceFlow);
						elementIdToSkip = sequenceFlow.getTargetRef();
					}
				} 
				
			}



			@Override
			protected Optional<Context> getInitialContext(BpmnModel model) {
				StartEvent startEvent = selectStartEvent(model);
				
				return Optional.of(new Context(startEvent));
			}
			
			

			@Override
			protected Optional<Context> getContext(FlowElement element,
					BpmnModel model) {
				
				Context context = new Context(element, currentContext.variableValueMap);
				
				if (isConnectionToSkip(element)) 
					return Optional.empty();
				
				return Optional.of(context);
			}
			
			
			private boolean isConnectionToSkip(FlowElement element) {
				return elementIdToSkip.equals(element.getId());
			}

			
			private boolean isVariableModifier(FlowElement element) {
				if (element instanceof Task) return true;
				if (element instanceof StartEvent) return true;
				if (element instanceof IntermediateCatchEvent) return true;
				
				return false;
			}

			
			private void annotateWithCurrentInputChanges(
				BpmnModel model, 
				FlowElement element, 
				Map<String, Object> variableValueMap) {
				
				List<String> variables = getModifiedVariables(model, element);
				
				if (variables.isEmpty()) return;
				
				String text = createAnnotationText(variables, variableValueMap);
				
				if (!StringUtils.isBlank(text))
					BpmnQueries.createAnnotationForElement(model, text, element);
			}

			
			private void annotateWithException(
					BpmnModel model,
					SequenceFlow sequenceFlow, 
					Throwable e) {
				
				FlowElement element = model.getFlowElement(sequenceFlow.getSourceRef());
				
				BpmnQueries.createAnnotationForElement(model, "Exception: " + e.getMessage(), element);
			}
			
			
			private void 
			overrideCurrentVariableValues(Context context, FlowElement element) {
				Map<String, Object> variableValueMap = 
					getCurrentVariableValuesContext(element, currentValuesCombination);

				context.variableValueMap.putAll(variableValueMap);
			}
			
			
			
			

			private class Context implements BpmnGraphTraversalWithDefaultElementMarking.IContext {
				public FlowElement element;
				public Map<String, Object> variableValueMap = new HashMap<>();
				
				
				public Context(FlowElement element) {
					this.element = element;
				}
				
				
				public Context(FlowElement element,
						Map<String, Object> variablesValues) {
					this.element = element;
					this.variableValueMap = new HashMap<>(variablesValues);
				}

				
				@Override
				public FlowElement getCurrentElement() {
					return element;
				}
			}
			
		}
		
		

		
		
		
		private Map<String, Object> getCurrentVariableValuesContext(
				FlowElement element, List<Object> currentVariablesValues) {
			
			Map<String, Object> variableValueMap = new HashMap<>();
			
			Map<String, Integer> variableIndexMap = 
					idVariableIndexMap.get(element.getId());			
					
			for (String variable : variableIndexMap.keySet()) {
				Integer index = variableIndexMap.get(variable);
				Object value = currentVariablesValues.get(index);
				
				variableValueMap.put(variable, value);
			}
			
			return variableValueMap;
		}

		
		
		private StartEvent selectStartEvent(BpmnModel currentTestCase) {
			List<StartEvent> startEvents = BpmnQueries.selectAllOfType(currentTestCase, StartEvent.class);
			
			Preconditions.checkArgument(!startEvents.isEmpty());
			
			return startEvents.get(0);
		}
		
		
		
		
		
		
		/**
		 * create list of variable values and then calculate all combinations 
		 * (@allVariablesValuesPermutations)
		 * create also index (@userTaskToVariableIndexMap) 
		 * which will tell you at what index in given combination
		 * value y is stored for variable x
		 * 
		 * 
		 * @param idVariableValueMap
		 */
		private void calculateIndexAndValuesCombination(
				Map<String, Multimap<String, Object>> idVariableValueMap) {
			
			List<Collection<Object>> values = Lists.newArrayList();
			
			for (String id : idVariableValueMap.keySet()) {
				Multimap<String, Object> variableValuesMap = idVariableValueMap.get(id);
				Map<String, Integer> variableIndexMap = new HashMap<>();
				
				for (String variable : variableValuesMap.keySet()) {
					Integer index = values.size();
					
					values.add(variableValuesMap.get(variable));
					variableIndexMap.put(variable, index);
					indexVariableMap.put(index, variable);
				}
				
				idVariableIndexMap.put(id, variableIndexMap);
			}
			
			allValueCombinations = CCollections.allCombinations(values);
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
		private String createAnnotationText(
				List<String> variables,
				Map<String, Object> variableValueMap) {

			StringBuilder annotationText = new StringBuilder();
			
			for (String variable : variables) {
				if (!variableValueMap.containsKey(variable)) continue;
				
				if (annotationText.length() <= 0)
					annotationText.append("User submits variables:    ");

				
				Object value = variableValueMap.get(variable);
				
				annotationText
					.append(variable)
					.append(" = <")
					.append(value)
					.append(">,   ");
			}
			
			return annotationText.toString();
		}



		private List<String> getModifiedVariables(BpmnModel model, FlowElement element) {
			List<String> variables = Lists.newArrayList();
			
			if (element instanceof Event) {
				Event startEvent = (Event) element;
				
				for (EventDefinition ed : startEvent.getEventDefinitions()) {
					if (ed instanceof MessageEventDefinition) {
						MessageEventDefinition message = (MessageEventDefinition) ed;
						
						Message msg = model.getMessage(message.getMessageRef());
						variables.add(msg.getName());
					}
				};
			} else if (element instanceof UserTask) {
				UserTask userTask = (UserTask) element;
				
				for (FormProperty property : userTask.getFormProperties()) {
					variables.add(property.getName());
				}
			} else if (element instanceof ScriptTask) {
				ScriptTask task = (ScriptTask) element;
				
				String script = task.getScript();
				if (StringUtils.isBlank(script)) return variables;
				
				Pattern variablePattern = Pattern.compile("[A-Z]\\w*\\s+([a-z]\\w*)(;|\\s)");
				Matcher variableMatcher = variablePattern.matcher(script);
				
				while (variableMatcher.find()) {
					String variable = variableMatcher.group(1);
					variables.add(variable);
				}
			}
			
			return variables;
		}		
		
	}
	
}


class InputManipulationInfo extends GenerationInfo {
	public List<String> variableValue = Lists.newArrayList(); 
	
	public InputManipulationInfo(List<String> variableValue) {
		this.variableValue = variableValue;
	}
	
	public static InputManipulationInfo create(Set<String> variableValue) {
		List<String> list = Lists.newArrayList(variableValue);
		Collections.sort(list);
		
		return new InputManipulationInfo(list);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((variableValue == null) ? 0 : variableValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InputManipulationInfo other = (InputManipulationInfo) obj;
		if (variableValue == null) {
			if (other.variableValue != null)
				return false;
		} else if (!variableValue.equals(other.variableValue))
			return false;
		return true;
	}
	
	
}
