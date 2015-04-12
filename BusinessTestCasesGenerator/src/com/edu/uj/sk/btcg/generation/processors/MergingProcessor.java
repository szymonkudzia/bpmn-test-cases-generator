package com.edu.uj.sk.btcg.generation.processors;

import java.io.IOException;
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
		Preconditions.checkNotNull(generators);
		Preconditions.checkArgument(!generators.isEmpty());

		this.generators = generators;
		this.processorName = processorName;
	}
	
	
	@Override
	public void process(BpmnModel model, TestCasePersister persister) throws Exception {
		Preconditions.checkNotNull(model);
		Preconditions.checkNotNull(persister);
		
		
		generateTestCases(model, persister, generators.get(0), generators.subList(1, generators.size()));
	}

	
	private void generateTestCases(BpmnModel model, TestCasePersister persister, IGenerator generator, List<IGenerator> generators) throws Exception {
		
		Iterator<BpmnModel> iterator = generator.generate(model);
		
		if (!iterator.hasNext()) {
			
			persistOrDoNextIteration(model, persister, generators);
			
		} else {
		
			while (iterator.hasNext()) {
				BpmnModel nextModel = iterator.next();
				
				persistOrDoNextIteration(nextModel, persister, generators);
			}
		}
	}

	

	private void persistOrDoNextIteration(
			BpmnModel model,
			TestCasePersister persister, 
			List<IGenerator> generators)
			throws IOException, Exception {
		
		if (generators.isEmpty()) {
			persister.persist(processorName, model);
			
		}else {
			generateTestCases(model, persister, generators.get(0), generators.subList(1, generators.size()));
		}
	}	

}
