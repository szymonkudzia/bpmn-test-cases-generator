package com.edu.uj.sk.btcg.bpmn;

import java.util.Map;
import java.util.Optional;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;

import com.google.common.collect.Maps;

public abstract class BpmnGraphTraversalWithDefaultElementMarking<T extends BpmnGraphTraversal.IContext>
	extends BpmnGraphTraversal<T> {
	Map<String, Object> visited = Maps.newHashMap();
	
	protected abstract void doProcessing(T context, BpmnModel model);
	protected abstract Optional<T> getInitialContext(BpmnModel model);
	protected abstract Optional<T> getContext(FlowElement element, BpmnModel model);
	
	
	@Override
	protected void markAsVisited(T context, FlowElement element) {
		visited.put(element.getId(), null);
	}
	
	
	@Override
	protected boolean wasVisited(T context, FlowElement element) {
		return visited.containsKey(element.getId());
	}
}


