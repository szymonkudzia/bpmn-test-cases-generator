package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;

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
	public boolean allTestRequirementsCovered(BpmnModel model,
			List<GenerationInfo> generationInfos) {
		// TODO Auto-generated method stub
		return false;
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
			
			return Pair.of(currentTestCase, null);
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
