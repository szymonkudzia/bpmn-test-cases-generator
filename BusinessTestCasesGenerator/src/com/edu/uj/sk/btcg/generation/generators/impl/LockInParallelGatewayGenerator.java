package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ParallelGateway;
import org.activiti.bpmn.model.SequenceFlow;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.collect.Lists;

public class LockInParallelGatewayGenerator implements IGenerator {

	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(BpmnModel originalModel) {
		return new Generator(originalModel);
	}
	
	
	@Override
	public boolean allTestRequirementsCovered(
			BpmnModel model,
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
				generationInfos.stream()
					.filter(i -> i instanceof LockInParallelGatewayInfo)
					.map(i -> (LockInParallelGatewayInfo) i)
					.map(i -> i.connection)
					.collect(Collectors.toList());
		return coveredTestRequirements;
	}


	private List<String> allTestRequirements(BpmnModel model) {
		List<String> allTestRequirements =
				new Generator(model).getAllTestRequirements();
		return allTestRequirements;
	}







	private class Generator extends AbstractGenerationIterator {
		private List<SequenceFlow> connections = Lists.newArrayList();
		
		
		public Generator(BpmnModel originalModel) {
			super(originalModel);
			
			List<ParallelGateway> gateways = BpmnQueries.selectAllOfType(originalModel, ParallelGateway.class);
			
			for (ParallelGateway gateway : gateways) {
				if (gateway.getIncomingFlows().size() > 1) {
					connections.addAll(gateway.getIncomingFlows());
				}
			}
		}
		
		
		public List<String> getAllTestRequirements() {
			return BpmnQueries.toIdList(connections);
		}
		

		@Override
		public boolean hasNext() {
			return !connections.isEmpty();
		}

		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);
			SequenceFlow currentConnection = connections.remove(0);
			
			currentConnection = (SequenceFlow) currentTestCase.getFlowElement(currentConnection.getId());
			
			FlowElement source = currentTestCase.getFlowElement(currentConnection.getSourceRef());
			FlowElement target = currentTestCase.getFlowElement(currentConnection.getTargetRef());
			
			BpmnQueries.createAnnotationForElement(currentTestCase, "If this task fails", source);
			BpmnQueries.createAnnotationForElement(currentTestCase, "Then this gate which waits for all incoming flows will wait forever!!!", target);
			
			createAnnotationForConnection(currentConnection);
			
			return Pair.of(currentTestCase, LockInParallelGatewayInfo.create(currentConnection));
		}

		private void createAnnotationForConnection(
				SequenceFlow currentConnection) {
			
			String newName = "Or this connection fails ";
			String previousName = currentConnection.getName();
			
			if (!StringUtils.isBlank(previousName))
				newName += "(" + previousName + ")";
			
			currentConnection.setName(newName);
		}
		
	}
}


class LockInParallelGatewayInfo extends GenerationInfo {
	public String connection;

	protected LockInParallelGatewayInfo(String connection) {
		this.connection = connection;
	}

	public static LockInParallelGatewayInfo create(SequenceFlow connection) {
		return new LockInParallelGatewayInfo(connection.getId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((connection == null) ? 0 : connection.hashCode());
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
		LockInParallelGatewayInfo other = (LockInParallelGatewayInfo) obj;
		if (connection == null) {
			if (other.connection != null)
				return false;
		} else if (!connection.equals(other.connection))
			return false;
		return true;
	}
	
	
}