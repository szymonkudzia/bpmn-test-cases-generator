package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.SendTask;

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
		List<List<SendTask>> messages = Lists.newArrayList();
		
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
			List<SendTask> nextCatchEvents = messages.remove(0);
			
			BpmnModel newTestCase = BpmnUtil.clone(originalModel);
			for (SendTask e : nextCatchEvents) {
				createAnnotationForElement(newTestCase, ANNOTATION_TEXT, e);
			}
			
			return newTestCase;
		}


		
		
		
		private List<SendTask> selectOutgoingMessagesEvents(
				BpmnModel model) {
			
			return selectAllMainProcessFlowElementsOfType(model, SendTask.class)
					.stream()
					.collect(Collectors.toList());
		}

	}


}
