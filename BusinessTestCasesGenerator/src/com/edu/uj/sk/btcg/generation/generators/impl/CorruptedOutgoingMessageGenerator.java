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
	private static final String ANNOTATION_TEXT = "Message was not sent to the receiver (because of lost connection or other reason) !!!";
	
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
