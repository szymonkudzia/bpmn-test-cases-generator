package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ServiceTask;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.collect.Lists;

public class CorruptedOutgoingMessageGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT = "Message:\n\n"
			+ "1. Was never sent.\n"
			+ "  a. Connection is down.\n\n"
			+ "  b. Recipent address is incorrect/\n\n"
			+ "  c. Recipent is currently unavailable.\n\n"
			+ "  d. Proxy was not configured properly.\n\n"
			+ "2. Has incorrect format.\n\n"
			+ "3. Has correct format but contains incorrect data.\n\n"
			+ "4. Holds data for different entity (but correct data).\n\n"
			+ "5. Timeout before sending message\n\n"
			+ "6. Data has poor quality.\n\n"
			;
	
	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}
	
	
	
	
	
	@Override
	public boolean allTestRequirementsCovered(
			BpmnModel model,
			List<GenerationInfo> generationInfos) {
		
		List<List<String>> coveredTestRequirements = 
				generationInfos
					.stream()
					.filter(i -> i instanceof CorruptedOutMessageInfo)
					.map(i -> (CorruptedOutMessageInfo) i)
					.map(i -> BpmnQueries.toIdList(i.senders))
					.collect(Collectors.toList());
		
		List<List<String>> allTestRequirements =
				new It(model).getAllTestRequirements();
		
		return coveredTestRequirements.containsAll(allTestRequirements);
	}





	class It extends AbstractGenerationIterator {
		List<List<ServiceTask>> messageSenders = Lists.newArrayList();
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			messageSenders = CCollections.powerSet(selectOutgoingMessagesEvents(originalModel));
			
		}
		
		public List<List<String>> getAllTestRequirements() {
			return BpmnQueries.toListOfIdList(messageSenders);
		}
		
		
		@Override
		public boolean hasNext() {
			return !messageSenders.isEmpty();
		}

		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			List<ServiceTask> nextMessageSenders = messageSenders.remove(0);
			
			BpmnModel newTestCase = BpmnUtil.clone(originalModel);
			for (ServiceTask e : nextMessageSenders) {
				BpmnQueries.createAnnotationForElement(newTestCase, ANNOTATION_TEXT, e);
			}
			
			return Pair.of(newTestCase, CorruptedOutMessageInfo.create(nextMessageSenders));
		}


		
		
		
		private List<ServiceTask> selectOutgoingMessagesEvents(
				BpmnModel model) {
			
			return BpmnQueries.selectAllOfType(model, ServiceTask.class)
					.stream()
					.filter(t -> {
						if (t.getType() == null) return false;
						
						return t.getType().equals("mail");
					})
					.collect(Collectors.toList());
		}

	}


}


class CorruptedOutMessageInfo extends GenerationInfo {
	public List<ServiceTask> senders;
	
	public CorruptedOutMessageInfo(List<ServiceTask> senders) {
		this.senders = senders;
	}
	
	public static CorruptedOutMessageInfo create(List<ServiceTask> senders) {
		return new CorruptedOutMessageInfo(senders);
	}
}
