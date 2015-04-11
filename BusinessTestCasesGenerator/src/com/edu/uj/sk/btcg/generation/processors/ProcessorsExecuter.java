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
	
	
	public static List<ProcessingStats> process(final List<IProcessor> processors, final boolean asSingleStrategy, final BpmnModel model, final TestCasePersister persister) {
		List<ProcessingStats> stats = Lists.newArrayList();
		List<IProcessor> p = Lists.newArrayList(processors);
		
		if (asSingleStrategy) {
			p = combineAsSingleStrategy(processors);
		}
		
		
		for (IProcessor processor : p) {
			final BpmnModel copy = BpmnUtil.clone(model);
			
			try {
				ProcessingStats stat = processor.process(copy, persister);
				stats.add(stat);
				
			} catch (Exception e) {
				logger.warn("Processor: [%s] finished with exception!", e, processor.getClass().getName());
				throw new IllegalStateException(e);
			}
		}
		
		return stats;
	}

	

	private static List<IProcessor> combineAsSingleStrategy(final List<IProcessor> processors) {
		List<IGenerator> generators = extractGeneratorsList(processors);
		
		return Lists.newArrayList(new MergingProcessor("all_combined", generators));
	}


	
	private static List<IGenerator> extractGeneratorsList(final List<IProcessor> processors) {
		return processors
				.stream()
				.filter(a -> a instanceof Processor)
				.map(a -> ((Processor)a).getGenerator())
				.collect(Collectors.toList());
	}
}
