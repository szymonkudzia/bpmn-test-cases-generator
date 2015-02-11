package com.edu.uj.sk.btcg.generation.processors;

import java.util.Iterator;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.logging.CLogger;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;

public class Processor implements IProcessor{
	private static CLogger logger = CLogger.getLogger(Processor.class);
	
	private IGenerator generator;
	private String processorName;
	
	public Processor(String processorName, IGenerator generator) {
		this.generator = generator;
		this.processorName = processorName;
	}
	
	@Override
	public void process(BpmnModel model, TestCasePersister persister) throws Exception {
		logger.info("Processor [%s] started...", processorName);
		
		Iterator<BpmnModel> iterator = generator.generate(model);
		while (iterator.hasNext()) {
			BpmnModel newTestCase = iterator.next();
			
			persister.persist(processorName, newTestCase);
		}
		
		logger.info("Processor [%s] finished", processorName);
	}

}
