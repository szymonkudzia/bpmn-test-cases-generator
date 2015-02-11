package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.IntermediateCatchEvent;
import org.activiti.bpmn.model.MessageEventDefinition;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.collect.Lists;

public class CorruptedIncomingMessageGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT = "Message is corrupted !!!";
	
	@Override
	public Iterator<BpmnModel> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}
	
	class It extends AbstractGenerationIterator {
		List<List<IntermediateCatchEvent>> messages = Lists.newArrayList();
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			messages = CCollections.powerSet(selectIncominvMessagesEvents(originalModel));
			
		}
		
		@Override
		public boolean hasNext() {
			return !messages.isEmpty();
		}

		@Override
		public BpmnModel next() {
			List<IntermediateCatchEvent> nextCatchEvents = messages.remove(0);
				
			BpmnModel newTestCase = BpmnUtil.clone(originalModel);
			for (IntermediateCatchEvent e : nextCatchEvents) {
				createAnnotationForElement(newTestCase, ANNOTATION_TEXT, e);
			}
			
			return newTestCase;
		}


		
		
		
		private List<IntermediateCatchEvent> selectIncominvMessagesEvents(
				BpmnModel model) {
			
			return selectAllMainProcessFlowElementsOfType(model, IntermediateCatchEvent.class)
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
