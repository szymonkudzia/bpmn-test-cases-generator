package com.edu.uj.sk.btcg.bpmn;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.SequenceFlow;

import com.google.common.collect.Maps;

public abstract class BpmnGraphTraversal<T extends BpmnGraphTraversal.IContext> {
	public void traverse(BpmnModel model) {
		Map<String, String> visited = Maps.newHashMap();
		Stack<T> contexts = new Stack<>();
		
		
		Optional<T> initialContext = getInitialContext(model);
		
		if (initialContext.isPresent()) 
			contexts.push(initialContext.get());
		
		
		while (!contexts.isEmpty()) {
			T context = contexts.pop();
			FlowElement element = context.getCurrentElement();
			
			if (wasVisited(visited, element)) continue;
			markAsVisited(visited, element);
			
			
			doProcessing(context, model);
			
			
			addNextElementsOntoStack(model, contexts, element);
		}
	}


	
	protected abstract void doProcessing(T context, BpmnModel model);
	protected abstract Optional<T> getInitialContext(BpmnModel model);
	protected abstract Optional<T> getContext(FlowElement element, BpmnModel model);
	
	public interface IContext {
		FlowElement getCurrentElement();
		
	}
	
	
	
	
	
	
	

	private void markAsVisited(Map<String, String> visited, FlowElement element) {
		visited.put(element.getId(), null);
	}



	private boolean wasVisited(Map<String, String> visited, FlowElement element) {
		return visited.containsKey(element.getId());
	}
	
	
	
	private void addNextElementsOntoStack(
			BpmnModel model, 
			Stack<T> contexts,
			FlowElement element) {
		
		
		if (element instanceof FlowNode) {
			FlowNode flowNode = (FlowNode) element;
			
			for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
				Optional<T> context = getContext(sequenceFlow, model);
				
				if (context.isPresent())
					contexts.push(context.get());
			}
			
		} else if (element instanceof SequenceFlow) {
			SequenceFlow sequenceFlow = (SequenceFlow) element;
			
			FlowElement targetElement = 
					model.getFlowElement(sequenceFlow.getTargetRef());
			
			Optional<T> context = getContext(targetElement, model);
			
			if (context.isPresent())
				contexts.push(context.get());
		}
	}
}


