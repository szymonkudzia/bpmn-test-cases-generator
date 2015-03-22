package com.edu.uj.sk.btcg.generation.processors;

import java.util.List;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.impl.BrokenIncomingFlowsToParallelGatewayGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CorruptedIncomingMessageGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CorruptedOutgoingMessageGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CoverageByInputManipulation;
import com.edu.uj.sk.btcg.generation.generators.impl.CoverageByPaths;
import com.edu.uj.sk.btcg.generation.generators.impl.ExceptionInScriptServiceTaskGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.SimpleCoverageGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.UserTaskCasesGenerator;
import com.edu.uj.sk.btcg.logging.CLogger;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.collect.Lists;

public class ProcessorsExecuter {
	private static CLogger logger = CLogger.getLogger(ProcessorsExecuter.class);
	
	private static List<IProcessor> processors = Lists.newArrayList();
	
	static {
		processors.add(new Processor("simple_coverage", new SimpleCoverageGenerator()));
		processors.add(new Processor("corrupted_incoming_message", new CorruptedIncomingMessageGenerator()));
		processors.add(new Processor("corrupted_outgoing_message", new CorruptedOutgoingMessageGenerator()));
		processors.add(new Processor("coverage_by_input_manipulation", new CoverageByInputManipulation()));
		processors.add(new Processor("coverage_by_paths", new CoverageByPaths(), true));
		processors.add(new Processor("manual_tasks", new UserTaskCasesGenerator()));
		processors.add(new Processor("broken_parallel_tasks", new BrokenIncomingFlowsToParallelGatewayGenerator()));
		processors.add(new Processor("script_service_tasts_interrupted", new ExceptionInScriptServiceTaskGenerator()));
		
//		processors.add(new MergingProcessor("all_strategies_at_once", Lists.newArrayList(
//			new SimpleCoverageGenerator(),
//			new CorruptedIncomingMessageGenerator(),
//			new CorruptedOutgoingMessageGenerator(),
//			new CoverageByInputManipulation()
//		)));
	}
	
	public static void process(final BpmnModel model, final TestCasePersister persister) {
		for (IProcessor processor : processors) {
			final BpmnModel copy = BpmnUtil.clone(model);
			
			try {
				processor.process(copy, persister);
				
			} catch (Exception e) {
				logger.warn("Processor: [%s] finished with exception!", e, processor.getClass().getName());
			}
		}
	}
}
