package com.edu.uj.sk.btcg.logic.extractors;

import org.activiti.bpmn.model.FlowElement;

import com.google.common.collect.Multimap;

abstract class AbstractVariableValueExtractor<T> implements IVariableValueExtractor {

	@SuppressWarnings("unchecked")
	@Override
	public Multimap<String, Object> extract(FlowElement element) {
		return doExtraction((T) element);
	}

	protected abstract Multimap<String, Object> doExtraction(T element);
}
