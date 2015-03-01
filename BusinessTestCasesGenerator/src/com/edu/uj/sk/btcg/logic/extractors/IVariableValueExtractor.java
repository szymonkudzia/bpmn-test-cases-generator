package com.edu.uj.sk.btcg.logic.extractors;

import org.activiti.bpmn.model.FlowElement;

import com.google.common.collect.Multimap;

public interface IVariableValueExtractor {
	public Multimap<String, Object> extract(FlowElement element);
}
