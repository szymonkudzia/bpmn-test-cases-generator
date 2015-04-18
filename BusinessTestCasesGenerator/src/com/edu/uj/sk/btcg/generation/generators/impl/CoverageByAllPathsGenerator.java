package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExclusiveGateway;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.SequenceFlow;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;

public class CoverageByAllPathsGenerator implements IGenerator {

	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(final BpmnModel originalModel) {
		return new It(originalModel);
	}

	
	
	@Override
	public boolean allTestRequirementsCovered(BpmnModel model,
			List<GenerationInfo> generationInfos) {
		// TODO Auto-generated method stub
		return false;
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
		public Pair<BpmnModel , GenerationInfo> next() {
			BpmnModel model = createNewTestCase(originalModel, conditions, connectionIndexes);
			
			return Pair.of(model, null);
		}
		
		
		
		

		private BpmnModel createNewTestCase(BpmnModel model,
				List<ExclusiveGateway> conditions, int[] connectionIndexes) {
			BpmnModel currentTestCase = BpmnUtil.clone(model);
			
			removeConditionsSelectedOutgoingFlows(
					conditions, connectionIndexes, currentTestCase);
			
			udpateToBeRemovedOutgingFlowIndexes(conditions, connectionIndexes);
			
			
			BpmnQueries.removeUnconnectedElements(currentTestCase);
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
				ExclusiveGateway condition = BpmnQueries.getExclusiveGateway(currentTestCase, c);
				
				SequenceFlow removedConnection = condition.getOutgoingFlows().remove(connectionIndexes[conditionIndex]);
				BpmnQueries.removeSequenceFlow(currentTestCase, removedConnection);
				
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
			
			if (conditions.isEmpty()) return false;
			if (indices.length == 0) return false;
			
			return indices[0] < conditions.get(0).getOutgoingFlows().size();
		}

		
		

		private List<ExclusiveGateway> selectConditionElements(BpmnModel model) {
			Collection<FlowElement> flowElements = BpmnQueries.selectAllFlowElements(model);
			
			List<ExclusiveGateway> conditions = flowElements
					.stream()
					.filter(e -> e instanceof ExclusiveGateway)
					.map(ExclusiveGateway.class::cast)
					.collect(Collectors.toList());
			
			return conditions;
		}
		
		

		

	}
}

