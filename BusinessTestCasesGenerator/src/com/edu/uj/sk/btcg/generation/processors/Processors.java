package com.edu.uj.sk.btcg.generation.processors;

import com.edu.uj.sk.btcg.generation.generators.impl.ArtifactGenerationFailedGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.BrokenIncomingFlowsToParallelGatewayGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CorruptedIncomingMessageGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CorruptedOutgoingMessageGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CoverageByInputManipulation;
import com.edu.uj.sk.btcg.generation.generators.impl.CoverageByPaths;
import com.edu.uj.sk.btcg.generation.generators.impl.ExceptionInScriptServiceTaskGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.HasRightsGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.RetrivingInformationFailedGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.SimpleCoverageGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.UserTaskCasesGenerator;

public class Processors {
	public static IProcessor newNaiveCoverage() {
		return new Processor("simple_coverage", new SimpleCoverageGenerator());
	}
	
	
	public static IProcessor newCorruptedIncomingMessage() {
		return new Processor("corrupted_incoming_message", new CorruptedIncomingMessageGenerator());
	}
	
	
	public static IProcessor newCorruptedOutgoingMessages() {
		return new Processor("corrupted_outgoing_message", new CorruptedOutgoingMessageGenerator());
	}
	
	
	public static IProcessor newCoverageByInputManipulation() {
		return new Processor("coverage_by_input_manipulation", new CoverageByInputManipulation());
	}
	
	
	public static IProcessor newCoverageByUniquePaths() {
		return new Processor("coverage_by_paths", new CoverageByPaths(), true);
	}
	
	
	public static IProcessor newManualTask() {
		return new Processor("manual_tasks", new UserTaskCasesGenerator());
	}
	
	
	public static IProcessor newParallelGatewayLock() {
		return new Processor("broken_parallel_tasks", new BrokenIncomingFlowsToParallelGatewayGenerator());
	}
	
	
	public static IProcessor newExceptionInScriptServiceTask() {
		return new Processor("script_service_tasts_interrupted", new ExceptionInScriptServiceTaskGenerator());
	}
	
	
	public static IProcessor newArtifactGenerationFail() {
		return new Processor("producer_fail", new ArtifactGenerationFailedGenerator());
	}
	
	
	public static IProcessor newFailedInformationRetrival() {
		return new Processor("data_fetching", new RetrivingInformationFailedGenerator());
	}
	
	
	public static IProcessor newHasRights() {
		return new Processor("rights", new HasRightsGenerator());
	}
}
