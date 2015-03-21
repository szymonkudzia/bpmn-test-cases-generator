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
		Stack<T> contexts = new Stack<>();
		
		
		Optional<T> initialContext = getInitialContext(model);
		
		if (initialContext.isPresent()) 
			contexts.push(initialContext.get());
		
		
		while (!contexts.isEmpty()) {
			T context = contexts.pop();
			FlowElement element = context.getCurrentElement();
			
			if (wasVisited(context, element)) continue;
			markAsVisited(context, element);
			
			
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
	
	

	protected abstract void markAsVisited(T context, FlowElement element);
	protected abstract boolean wasVisited(T context, FlowElement element);
	
	
	
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


