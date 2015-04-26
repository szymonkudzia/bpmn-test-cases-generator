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
			
		List<String> allTestRequirements =
				new Generator(model).getAllTestRequirements();
		
		if (allTestRequirements.isEmpty()) return true;

		
		List<String> coveredTestRequirements =
				generationInfos.stream()
					.filter(i -> i instanceof LockInParallelGatewayInfo)
					.map(i -> (LockInParallelGatewayInfo) i)
					.map(i -> i.connection)
					.collect(Collectors.toList());
		
		
		return coveredTestRequirements.containsAll(allTestRequirements);
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
}