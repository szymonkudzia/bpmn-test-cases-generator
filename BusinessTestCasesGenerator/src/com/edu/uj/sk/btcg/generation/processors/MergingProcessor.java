package com.edu.uj.sk.btcg.generation.processors;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

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
	public ProcessingStats process(BpmnModel model, TestCasePersister persister) throws Exception {
		Preconditions.checkNotNull(model);
		Preconditions.checkNotNull(persister);
		
		CountingPersister countingPersister = new CountingPersister(persister);
		
		generateTestCases(model, countingPersister, generators.get(0), generators.subList(1, generators.size()));
		
		return countingPersister.getStats(processorName);
	}

	
	private void generateTestCases(BpmnModel model, TestCasePersister persister, IGenerator generator, List<IGenerator> generators) throws Exception {
		
		Iterator<BpmnModel> iterator = generator.generate(model);
		while (iterator.hasNext()) {
			BpmnModel nextModel = iterator.next();
			
			if (generators.isEmpty()) {
				persister.persist(processorName, nextModel);
				
			}else {
				generateTestCases(nextModel, persister, generators.get(0), generators.subList(1, generators.size()));
			}
		}

	}	

	
	class CountingPersister extends TestCasePersister {
		private TestCasePersister original;
		private int total;
		private Set<String> uniqueModels = Sets.newHashSet();
		
		
		public CountingPersister(TestCasePersister original) throws IOException {
			super(null);
			this.original = original;
		}

		@Override
		public void persist(String namespace, BpmnModel model,
				boolean preserveDuplications) throws IOException {
			
			++total;
			uniqueModels.add(BpmnUtil.toString(model));
			
			original.persist(namespace, model, preserveDuplications);
		}

		
		public ProcessingStats getStats(String name) {
			return new ProcessingStats(name, total, uniqueModels.size());
		}
	}
}
