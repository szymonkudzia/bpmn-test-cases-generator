package com.edu.uj.sk.btcg.generation;

import java.io.IOException;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.generation.processors.ProcessorsExecuter;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;

public class TestCasesGenerator {

	public void generate(
			final BpmnModel bpmnModel,
			final TestCasePersister persister) throws IOException {
		
		ProcessorsExecuter.process(bpmnModel, persister);
	}

}
