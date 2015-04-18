package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;

import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.lang3.tuple.Pair;

abstract class AbstractGenerationIterator implements Iterator<Pair<BpmnModel, GenerationInfo>> {
	protected BpmnModel originalModel;
	
	public AbstractGenerationIterator(BpmnModel originalModel) {
		this.originalModel = originalModel;
	}
}


