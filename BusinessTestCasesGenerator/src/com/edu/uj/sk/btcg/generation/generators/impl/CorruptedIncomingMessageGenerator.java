package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.IntermediateCatchEvent;
import org.activiti.bpmn.model.MessageEventDefinition;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.collect.Lists;

public class CorruptedIncomingMessageGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT = "Message:\n\n"
			+ "1. Was never sent.\n\n"
			+ "  a. Connection is down.\n\n"
			+ "  b. Proxy was not configured properly.\n\n"
			+ "2. Has incorrect format.\n\n"
			+ "3. Has correct format but contains incorrect data.\n\n"
			+ "4. Holds data for different entity (but correct data).\n\n"
			+ "5. Timeout before receiving message\n\n"
			+ "6. Sender takes lot of time to sent it.\n\n"
			+ "7. Data has poor quality.\n\n"
			;
	
	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}
	
	
	
	@Override
	public boolean allTestRequirementsCovered(BpmnModel model,
			List<GenerationInfo> generationInfos) {
		
		List<String> allTestRequirements =
				new It(model).getAllTestRequirements();
		
		if (allTestRequirements.isEmpty()) return true;

		
		List<String> coveredTestRequirements = 
				generationInfos
					.stream()
					.filter(i -> i instanceof CorruptedInMessageInfo)
					.map(i -> (CorruptedInMessageInfo) i)
					.map(i -> i.receiver)
					.collect(Collectors.toList());
		
		
		return coveredTestRequirements.containsAll(allTestRequirements);
	}





	class It extends AbstractGenerationIterator {
		List<IntermediateCatchEvent> messageReceivers = Lists.newArrayList();
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			messageReceivers = selectIncominvMessagesEvents(originalModel);
			
		}
		
		public List<String> getAllTestRequirements() {
			return BpmnQueries.toIdList(messageReceivers);
		}
		
		
		@Override
		public boolean hasNext() {
			return !messageReceivers.isEmpty();
		}

		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			IntermediateCatchEvent nextCatchEvent = messageReceivers.remove(0);
				
			BpmnModel newTestCase = BpmnUtil.clone(originalModel);
			BpmnQueries.createAnnotationForElement(newTestCase, ANNOTATION_TEXT, nextCatchEvent);
			
			return Pair.of(newTestCase, CorruptedInMessageInfo.create(nextCatchEvent));
		}


		
		
		
		private List<IntermediateCatchEvent> selectIncominvMessagesEvents(
				BpmnModel model) {
			
			return BpmnQueries.selectAllOfType(model, IntermediateCatchEvent.class)
					.stream()
					.filter(x -> 
						!x.getEventDefinitions().isEmpty() 
						&& 
						x.getEventDefinitions().get(0) instanceof MessageEventDefinition
					)
					.collect(Collectors.toList());
		}

	}


}


class CorruptedInMessageInfo extends GenerationInfo {
	public String receiver;
	
	public CorruptedInMessageInfo(String receivers) {
		this.receiver = receivers;
	}
	
	public static CorruptedInMessageInfo create(IntermediateCatchEvent receivers) {
		return new CorruptedInMessageInfo(receivers.getId());
	}
}