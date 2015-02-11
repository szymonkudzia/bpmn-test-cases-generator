package com.edu.uj.sk.btcg.generation.processors;

import java.util.Iterator;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.base.Preconditions;

public class MergingProcessor implements IProcessor {
	private List<IGenerator> generators;
	private String processorName;
	
	public MergingProcessor(String processorName, List<IGenerator> generators) {
		this.generators = generators;
		this.processorName = processorName;
		
		Preconditions.checkNotNull(generators);
		Preconditions.checkArgument(!generators.isEmpty());
	}
	
	
	@Override
	public void process(BpmnModel model, TestCasePersister persister) throws Exception {
		Preconditions.checkNotNull(model);
		Preconditions.checkNotNull(persister);
		
		generateTestCases(model, persister, generators.get(0), generators.subList(1, generators.size()));
	}

	
	private void generateTestCases(BpmnModel model, TestCasePersister persister, IGenerator generator, List<IGenerator> generators) throws Exception {
		
		processNextModel(
				model, 
				persister,
				generator, 
				generators);	
	}
	
	
	private void processNextModel(
			BpmnModel model, 
			TestCasePersister persister,
			IGenerator generator,
			List<IGenerator> generators) throws Exception {
	
		Iterator<BpmnModel> iterator = generator.generate(model);
		while (iterator.hasNext()) {
			BpmnModel nextModel = iterator.next();
			
			if (generators.isEmpty()) {
				persister.persist(processorName, nextModel);
				
			}else {
				processNextModel(nextModel, persister, generators.get(0), generators.subList(1, generators.size()));
			}
		}

	}

}
