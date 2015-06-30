package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ServiceTask;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
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
				generationInfos
					.stream()
					.filter(i -> i instanceof CorruptedOutMessageInfo)
					.map(i -> (CorruptedOutMessageInfo) i)
					.map(i -> i.senders)
					.collect(Collectors.toList());
		return coveredTestRequirements;
	}





	private List<String> allTestRequirements(BpmnModel model) {
		List<String> allTestRequirements =
				new It(model).getAllTestRequirements();
		return allTestRequirements;
	}





	class It extends AbstractGenerationIterator {
		List<ServiceTask> messageSenders = Lists.newArrayList();
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			messageSenders = selectOutgoingMessagesEvents(originalModel);
			
		}
		
		public List<String> getAllTestRequirements() {
			return BpmnQueries.toIdList(messageSenders);
		}
		
		
		@Override
		public boolean hasNext() {
			return !messageSenders.isEmpty();
		}

		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			ServiceTask nextMessageSender = messageSenders.remove(0);
			
			BpmnModel newTestCase = BpmnUtil.clone(originalModel);
			BpmnQueries.createAnnotationForElement(newTestCase, ANNOTATION_TEXT, nextMessageSender);
			
			return Pair.of(newTestCase, CorruptedOutMessageInfo.create(nextMessageSender));
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
	public String senders;
	
	public CorruptedOutMessageInfo(String senders) {
		this.senders = senders;
	}
	
	public static CorruptedOutMessageInfo create(ServiceTask senders) {
		return new CorruptedOutMessageInfo(senders.getId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((senders == null) ? 0 : senders.hashCode());
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
		CorruptedOutMessageInfo other = (CorruptedOutMessageInfo) obj;
		if (senders == null) {
			if (other.senders != null)
				return false;
		} else if (!senders.equals(other.senders))
			return false;
		return true;
	}
	
	
}
