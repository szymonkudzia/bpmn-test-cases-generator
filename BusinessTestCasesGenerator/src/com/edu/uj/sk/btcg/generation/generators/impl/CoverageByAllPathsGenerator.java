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
import com.google.common.collect.Lists;

public class CoverageByAllPathsGenerator implements IGenerator {

	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(final BpmnModel originalModel) {
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
		List<String> coveredTestRequirements = generationInfos.stream()
			.filter(i -> i instanceof AllPathsInfo)
			.map(i -> (AllPathsInfo) i)
			.map(i -> i.connections)
			.reduce(Lists.newArrayList(), (acc, c) -> {
				acc.addAll(c);
				return acc;
			});
		return coveredTestRequirements;
	}



	private List<String> allTestRequirements(BpmnModel model) {
		List<String> allTestRequirements = new It(model).getAllTestRequirements();
		return allTestRequirements;
	}



	class It extends AbstractGenerationIterator {
		private List<ExclusiveGateway> conditions;
		private int[] connectionIndexes;
		
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			conditions = selectConditionElements(originalModel);
			connectionIndexes = new int[conditions.size()];
		}

		
		public List<String> getAllTestRequirements() {
			return conditions.stream()
					.map(c -> BpmnQueries.toIdList(c.getOutgoingFlows()))
					.reduce(Lists.newArrayList(), (acc, c) -> {
						acc.addAll(c);
						return acc;
					});					
		}
		
		
		@Override
		public boolean hasNext() {
			return thereIsUnusedPermutation(conditions, connectionIndexes);
		}

		@Override
		public Pair<BpmnModel , GenerationInfo> next() {
			return createNewTestCase(originalModel, conditions, connectionIndexes);
		}
		
		
		
		

		private Pair<BpmnModel, GenerationInfo> createNewTestCase(BpmnModel model,
				List<ExclusiveGateway> conditions, int[] connectionIndexes) {
			BpmnModel currentTestCase = BpmnUtil.clone(model);
			
			List<SequenceFlow> removedConnections = 
					removeConditionsSelectedOutgoingFlows(
							conditions, connectionIndexes, currentTestCase);
			
			udpateToBeRemovedOutgingFlowIndexes(conditions, connectionIndexes);
			
			
			BpmnQueries.removeUnconnectedElements(currentTestCase);
			return Pair.of(currentTestCase, AllPathsInfo.create(removedConnections));
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






		private List<SequenceFlow> removeConditionsSelectedOutgoingFlows(
				List<ExclusiveGateway> conditions, int[] connectionIndexes,
				BpmnModel currentTestCase) {
			
			List<SequenceFlow> removed = Lists.newArrayList();
			
			int conditionIndex = 0;
			for (ExclusiveGateway c : conditions) {
				ExclusiveGateway condition = BpmnQueries.getExclusiveGateway(currentTestCase, c);
				
				SequenceFlow removedConnection = condition.getOutgoingFlows().remove(connectionIndexes[conditionIndex]);
				BpmnQueries.removeSequenceFlow(currentTestCase, removedConnection);
				
				conditionIndex++;
				removed.add(removedConnection);
			}
			
			return removed;
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

class AllPathsInfo extends GenerationInfo {
	public List<String> connections;
	
	private AllPathsInfo(List<String> connections) {
		this.connections = connections;
	}
	
	public static AllPathsInfo create(List<SequenceFlow> connections) {
		return new AllPathsInfo(BpmnQueries.toIdList(connections));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((connections == null) ? 0 : connections.hashCode());
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
		AllPathsInfo other = (AllPathsInfo) obj;
		if (connections == null) {
			if (other.connections != null)
				return false;
		} else if (!connections.equals(other.connections))
			return false;
		return true;
	}
	
	
}
