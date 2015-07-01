package com.edu.uj.sk.btcg.generation.processors;

import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.logging.CLogger;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.collect.Lists;

public class ProcessorsExecuter {
	private static CLogger logger = CLogger.getLogger(ProcessorsExecuter.class);
	
	
	public static void process(
			final List<IProcessor> processors, 
			final boolean asSingleStrategy, 
			final boolean greedyOptimization,
			final boolean greedy2Optimization, 
			final BpmnModel model, 
			final TestCasePersister persister) {
		
		List<IProcessor> p = Lists.newArrayList(processors);
		
		if (asSingleStrategy) {
			p = combineAsSingleStrategy(processors, greedyOptimization, greedy2Optimization);
		}
		
		
		for (IProcessor processor : p) {
			final BpmnModel copy = BpmnUtil.clone(model);
			
			try {
				processor.process(copy, persister);
				
			} catch (Exception e) {
				logger.warn("Processor: [%s] finished with exception!", e, processor.getClass().getName());
				throw new IllegalStateException(e);
			}
		}
	}

	

	private static List<IProcessor> combineAsSingleStrategy(
			List<IProcessor> processors, 
			boolean greedyOptimization,
			boolean greedy2Optimization) {
		
		List<IGenerator> generators = extractGeneratorsList(processors);
		
		MergingProcessor mergingProcessor = new MergingProcessor(
				"all_combined", 
				generators, 
				greedyOptimization,
				greedy2Optimization);
		
		return Lists.newArrayList(mergingProcessor);
	}


	
	private static List<IGenerator> extractGeneratorsList(final List<IProcessor> processors) {
		return processors
				.stream()
				.filter(a -> a instanceof Processor)
				.map(a -> ((Processor)a).getGenerator())
				.collect(Collectors.toList());
	}
}
