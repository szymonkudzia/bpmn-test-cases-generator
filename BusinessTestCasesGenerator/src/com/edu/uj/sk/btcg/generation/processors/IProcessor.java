package com.edu.uj.sk.btcg.generation.processors;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.persistance.TestCasePersister;

public interface IProcessor {
	ProcessingStats process(final BpmnModel model, final TestCasePersister persister) throws Exception;
}
