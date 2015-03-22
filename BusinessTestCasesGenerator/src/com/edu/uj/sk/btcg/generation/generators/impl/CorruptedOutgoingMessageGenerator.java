package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ServiceTask;

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
	public Iterator<BpmnModel> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}
	
	class It extends AbstractGenerationIterator {
		List<List<ServiceTask>> messages = Lists.newArrayList();
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			messages = CCollections.powerSet(selectOutgoingMessagesEvents(originalModel));
			
		}
		
		@Override
		public boolean hasNext() {
			return !messages.isEmpty();
		}

		@Override
		public BpmnModel next() {
			List<ServiceTask> nextCatchEvents = messages.remove(0);
			
			BpmnModel newTestCase = BpmnUtil.clone(originalModel);
			for (ServiceTask e : nextCatchEvents) {
				createAnnotationForElement(newTestCase, ANNOTATION_TEXT, e);
			}
			
			return newTestCase;
		}


		
		
		
		private List<ServiceTask> selectOutgoingMessagesEvents(
				BpmnModel model) {
			
			return BpmnQueries.selectAllOfType(model, ServiceTask.class)
					.stream()
					.filter(t -> t.getType().equals("mail"))
					.collect(Collectors.toList());
		}

	}


}
