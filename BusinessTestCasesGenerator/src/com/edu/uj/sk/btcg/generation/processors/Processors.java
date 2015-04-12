package com.edu.uj.sk.btcg.generation.processors;

import com.edu.uj.sk.btcg.generation.generators.impl.IncorectArtifactsGenerationGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.LockInParallelGatewayGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CorruptedIncomingMessageGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CorruptedOutgoingMessageGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CoverageByInputManipulation;
import com.edu.uj.sk.btcg.generation.generators.impl.KEdgeCoverage;
import com.edu.uj.sk.btcg.generation.generators.impl.ExceptionInAutomatedTasksGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.HasRightsGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.RetrivingInformationFailedGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.CoverageByAllPathsGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.ManualTaskGenerator;

public class Processors {
	public static IProcessor newCoverageByAllPaths() {
		return new Processor("coverage_by_all_paths", new CoverageByAllPathsGenerator());
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
	
	
	public static IProcessor newKEdgeCoverage(int kNearestDecisionNodes) {
		return new Processor("k_edge_coverage", new KEdgeCoverage(kNearestDecisionNodes), true);
	}
	
	
	public static IProcessor newManualTask() {
		return new Processor("manual_tasks", new ManualTaskGenerator());
	}
	
	
	public static IProcessor newLockInParallelGateway() {
		return new Processor("lock_in_parallel_gateway", new LockInParallelGatewayGenerator());
	}
	
	
	public static IProcessor newExceptionInAutomatedTasks() {
		return new Processor("exception_in_automated_tasks", new ExceptionInAutomatedTasksGenerator());
	}
	
	
	public static IProcessor newIncorrectArtifactsGeneration() {
		return new Processor("incorrect_artifacts_generation", new IncorectArtifactsGenerationGenerator());
	}
	
	
	public static IProcessor newInformationRetrivalTask() {
		return new Processor("information_retrival_task", new RetrivingInformationFailedGenerator());
	}
	
	
	public static IProcessor newHasRights() {
		return new Processor("has_rights", new HasRightsGenerator());
	}
}
