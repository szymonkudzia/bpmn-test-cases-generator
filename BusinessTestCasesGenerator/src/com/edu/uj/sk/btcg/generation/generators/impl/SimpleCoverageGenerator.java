package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExclusiveGateway;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.SequenceFlow;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;

public class SimpleCoverageGenerator implements IGenerator {

	@Override
	public Iterator<BpmnModel> generate(final BpmnModel originalModel) {
		return new It(originalModel);
	}

	
	class It extends AbstractGenerationIterator {
		private List<ExclusiveGateway> conditions;
		private int[] connectionIndexes;
		
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			conditions = selectConditionElements(originalModel);
			connectionIndexes = new int[conditions.size()];
		}

		@Override
		public boolean hasNext() {
			return thereIsUnusedPermutation(conditions, connectionIndexes);
		}

		@Override
		public BpmnModel next() {
			return createNewTestCase(originalModel, conditions, connectionIndexes);
		}
		
		
		
		

		private BpmnModel createNewTestCase(BpmnModel model,
				List<ExclusiveGateway> conditions, int[] connectionIndexes) {
			BpmnModel currentTestCase = BpmnUtil.clone(model);
			
			removeConditionsSelectedOutgoingFlows(
					conditions, connectionIndexes, currentTestCase);
			
			udpateToBeRemovedOutgingFlowIndexes(conditions, connectionIndexes);
			
			
			removeUnconnectedElements(currentTestCase);
			return currentTestCase;
		}






		private void udpateToBeRemovedOutgingFlowIndexes(
				List<ExclusiveGateway> conditions, int[] connectionIndexes) {
			for (int i = connectionIndexes.length - 1; i >= 0; --i) {
				connectionIndexes[i]++;
				
				if (!connectionIndexIsLowerThanConnectionCount(conditions, connectionIndexes, i)) {
					if (i > 0) {
						connectionIndexes[i] = 0;
						continue;
					}
				}
				break;
			}
		}






		private void removeConditionsSelectedOutgoingFlows(
				List<ExclusiveGateway> conditions, int[] connectionIndexes,
				BpmnModel currentTestCase) {
			int conditionIndex = 0;
			for (ExclusiveGateway c : conditions) {
				ExclusiveGateway condition = getExclusiveGateway(currentTestCase, c);
				
				SequenceFlow removedConnection = condition.getOutgoingFlows().remove(connectionIndexes[conditionIndex]);
				removeSequenceFlow(currentTestCase, removedConnection);
				
				conditionIndex++;
			}
		}






		private boolean connectionIndexIsLowerThanConnectionCount(
				List<ExclusiveGateway> conditions, int[] connectionIndexes, int i) {
			return connectionIndexes[i] < conditions.get(i).getOutgoingFlows().size();
		}


		private boolean thereIsUnusedPermutation(
				List<ExclusiveGateway> conditions,
				int[] indices) {
			
			return indices[0] < conditions.get(0).getOutgoingFlows().size();
		}

		
		

		private List<ExclusiveGateway> selectConditionElements(BpmnModel model) {
			Collection<FlowElement> flowElements = selectAllFlowElements(model);
			
			List<ExclusiveGateway> conditions = flowElements
					.stream()
					.filter(e -> e instanceof ExclusiveGateway)
					.map(ExclusiveGateway.class::cast)
					.collect(Collectors.toList());
			
			return conditions;
		}
		
		
		/**
		 * Select all flow elements from given model
		 * (Elements in subprocesses are ignored)
		 * 
		 * @param model
		 * @return collection of all flow elements of main process in @model
		 */
		private Collection<FlowElement> selectAllFlowElements(BpmnModel model) {
			Collection<FlowElement> mainElements = model.getMainProcess().getFlowElements();
//			Collection<FlowElement> subElements = selectAllSubFlowElements(mainElements);

			Set<FlowElement> result = new HashSet<>();
			
			result.addAll(mainElements);
//			result.addAll(subElements);
			
			return result;
		}
		

	}
}
