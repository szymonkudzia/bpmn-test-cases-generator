package com.edu.uj.sk.btcg.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.IntermediateCatchEvent;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.Task;

import com.edu.uj.sk.btcg.bpmn.BpmnGraphTraversalWithDefaultElementMarking;
import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.logic.extractors.VariableValueExtractor;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;

public class VariableValueExtractorFromBpmnModel {
	private VariableValueExtractor variableValueExtractor =
			VariableValueExtractor.create();
	
	
	private VariableValueExtractorFromBpmnModel() {}
	
	public static VariableValueExtractorFromBpmnModel create() {
		return new VariableValueExtractorFromBpmnModel();
	}
	
	
	public Map<String, Multimap<String, Object>> extract(BpmnModel model) {
		Traverser traverser = new Traverser();
		
		traverser.traverse(model);
		
		return traverser.getIdVariableValueMap();
	}
	
	
	
	private class Traverser extends BpmnGraphTraversalWithDefaultElementMarking<Context> {
		private static final String NO_START_EVENT_MSG = "There is no start event defined!";
		
		private Map<String, Multimap<String, Object>> flowElementIdVariableValueMap = new HashMap<>();
		private FlowElement currentVariableModifier;
		private FlowElement currentElement;
		
		@Override
		protected void doProcessing(Context context, BpmnModel model) {
			currentElement          = context.getCurrentElement();
			currentVariableModifier = context.getVariableModifier();
			
			if (isVariableModifier(currentElement)) 
				currentVariableModifier = currentElement;
			
			Multimap<String, Object> 
			variableValueMap = variableValueExtractor.extract(currentElement);
			
			
			String 
			id = currentVariableModifier != null ? currentVariableModifier.getId() : "";
			
			
			CCollections.updateMap(flowElementIdVariableValueMap, id, variableValueMap);
		}





		@Override
		protected Optional<Context> getInitialContext(BpmnModel model) {
			List<StartEvent> 
			startEvents = BpmnQueries.selectAllOfType(model, StartEvent.class);
			
			Preconditions.checkArgument(!startEvents.isEmpty(), NO_START_EVENT_MSG);
			
			StartEvent startEvent = startEvents.get(0);
			
			currentVariableModifier = null;
			if (isVariableModifier(startEvent))
				currentVariableModifier = startEvent;
			
			return Optional.of(new Context(startEvent, currentVariableModifier));
		}


		@Override
		protected Optional<Context> getContext
			(FlowElement element, BpmnModel model) {
			
			return Optional.of(new Context(element, currentVariableModifier));
		}
		
		
		public Map<String, Multimap<String, Object>> getIdVariableValueMap() {
			return flowElementIdVariableValueMap;
		}
		
		
		private boolean isVariableModifier(FlowElement element) {
			if (element instanceof StartEvent)             return true;
			if (element instanceof IntermediateCatchEvent) return true;
			if (element instanceof Task)                   return true;
			
			return false;
		}
		
	}
	
	
	
	private class Context implements BpmnGraphTraversalWithDefaultElementMarking.IContext {
		public FlowElement currentElement;
		public FlowElement variableModifier;
		
		
		public Context(FlowElement currentElement, FlowElement variableModifier) {
			this.currentElement = currentElement;
			this.variableModifier = variableModifier;
		}



		@Override
		public FlowElement getCurrentElement() {
			return currentElement;
		}

		public FlowElement getVariableModifier() {
			return variableModifier;
		}	
	}
}
	
