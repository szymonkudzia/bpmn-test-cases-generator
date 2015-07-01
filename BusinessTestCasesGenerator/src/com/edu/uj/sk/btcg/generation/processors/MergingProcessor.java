package com.edu.uj.sk.btcg.generation.processors;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.GenerationInfo;
import com.edu.uj.sk.btcg.logging.CLogger;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

public class MergingProcessor implements IProcessor {
	private CLogger logger = CLogger.getLogger(MergingProcessor.class);
	
	private List<IGenerator> generators;
	private String processorName;
	private boolean greedyOptimization;
	private boolean greedy2Optimization;
	
	public MergingProcessor(
			String processorName, 
			List<IGenerator> generators, 
			boolean greedyOptimization,
			boolean greedy2Optimization) {
		
		Preconditions.checkNotNull(generators);
		Preconditions.checkArgument(!generators.isEmpty());

		this.generators = generators;
		this.processorName = processorName;
		this.greedyOptimization = greedyOptimization;
		this.greedy2Optimization = greedy2Optimization;
	}
	
	
	@Override
	public void process(BpmnModel model, TestCasePersister persister) throws Exception {
		Preconditions.checkNotNull(model);
		Preconditions.checkNotNull(persister);
		
		logger.info("Generating models...");
		
		Multimap<BpmnModel, GenerationInfo> notFilteredResults = HashMultimap.create();
		Pair<BpmnModel, List<GenerationInfo>> modelGenerationInfo = Pair.of(model, Lists.newArrayList());
		
		generateTestCases(modelGenerationInfo, notFilteredResults, generators.get(0), generators.subList(1, generators.size()));
		
		logger.info("Generation finished!");
		
		List<BpmnModel> models = removeDuplicates(notFilteredResults);
		List<List<GenerationInfo>> infos = getValuesInSameOrderAsModels(models, notFilteredResults);
		
		
		if (greedyOptimization) {
			Stopwatch stoper = Stopwatch.createStarted();
			logger.info("Greedy optimizaton started...");
			
			greedyOptimization(models, infos, model, persister);
			
			logger.info("Greedy optimization finished! In %s", stoper.stop());
			
		} else if (greedy2Optimization) {
			Stopwatch stoper = Stopwatch.createStarted();
			logger.info("Greedy^2 optimization started...");
					
			greedy2Optimization(models, infos, model, persister);
			
			logger.info("Greedy^2 optimization finished. In %s", stoper.stop());
			
		} else {
			Stopwatch stoper = Stopwatch.createStarted();
			logger.info("Storing generated models...");
			store(persister, models);
			
			logger.info("Generated models stored. In %s", stoper.stop());
		}
	}


	private void greedyOptimization(
			final List<BpmnModel> models,
			final List<List<GenerationInfo>> infos,
			final BpmnModel original,
			TestCasePersister persister) throws IOException {
		
		List<Greedy> greedyList = toGreedyList(models, infos, original);
		Collections.sort(greedyList);
		
		List<GenerationInfo> infosTotal = Lists.newArrayList();
		for (Greedy greedy : greedyList) {
			
			if (greedy.doesNotContainAnythingNew(infosTotal))
				continue;
			
			infosTotal.addAll(greedy.infos);
			
			persister.persist(processorName, greedy.model);
			if (allTestRequirementCovered(original, infosTotal))
				break;
		}
	}


	private List<Greedy> toGreedyList(final List<BpmnModel> models,
			final List<List<GenerationInfo>> infos, final BpmnModel original) {
		
		List<Greedy> mi = IntStream.range(0, models.size())
				.mapToObj(i -> newGreedy(original, models.get(i), infos.get(i)))
				.collect(Collectors.toList());
		
		return mi;
	}

	
	
	private class Greedy implements Comparable<Greedy> {
		BpmnModel originalModel;
		BpmnModel model;
		List<GenerationInfo> infos;
		Integer covered = 0;
				
		public Greedy recalculateCovered() {
			covered = countCoveredTestRequirementsNumber(originalModel, Lists.newArrayList(infos));
			return this;
		}

		@Override
		public int compareTo(Greedy o) {
			return o.covered.compareTo(covered);
		}
		
		public boolean doesNotContainAnythingNew(List<GenerationInfo> infos) {
			return infos.containsAll(this.infos);
		}
		
		public Greedy removeAll(List<GenerationInfo> infos) {
			this.infos.removeAll(infos);
			return this;
		}
	}
	
	private Greedy newGreedy(BpmnModel originalModel, BpmnModel model, List<GenerationInfo> infos) {
		Greedy greedy = new Greedy();
		greedy.originalModel = originalModel;
		greedy.model = model;
		greedy.infos = Lists.newArrayList(infos);
		
		greedy.recalculateCovered();
		
		return greedy;
	}
	
	
	
	private void greedy2Optimization(
			final List<BpmnModel> models,
			final List<List<GenerationInfo>> infos,
			final BpmnModel original,
			TestCasePersister persister) throws IOException {
		
		List<Greedy> greedyList = toGreedyList(models, infos, original);
		Collections.sort(greedyList);
		
		List<GenerationInfo> infosTotal = Lists.newArrayList();
		while (!greedyList.isEmpty()) {
			Greedy greedy = greedyList.remove(0);
			
			if (greedy.doesNotContainAnythingNew(infosTotal))
				continue;
			
			infosTotal.addAll(greedy.infos);
			
			persister.persist(processorName, greedy.model);
			if (allTestRequirementCovered(original, infosTotal))
				break;
			
			greedyList.forEach(g -> g.removeAll(infosTotal).recalculateCovered());
			greedyList = greedyList.stream().filter(g -> g.covered > 0).collect(Collectors.toList());
			Collections.sort(greedyList);
		}
	}
	
	

	
	
	private List<BpmnModel> removeDuplicates(
			Multimap<BpmnModel, GenerationInfo> notFilteredResults) {
		
		final Map<String, Void> map = Maps.newHashMap();
		
		return notFilteredResults.keySet()
			.stream()
			.filter(m -> {
				String key = BpmnUtil.toString(m);
				
				if (map.containsKey(key))
					return false;
				
				map.put(key, null);
				return true;
			})
			.collect(Collectors.toList());
	}


	
	
	
	
	
	
	







	
	
	

	








	private void generateTestCases(
			Pair<BpmnModel, List<GenerationInfo>> model, 
			Multimap<BpmnModel, GenerationInfo> notFilteredResults, 
			IGenerator generator, 
			List<IGenerator> generators) throws Exception {
		
		Iterator<Pair<BpmnModel, GenerationInfo>> iterator = generator.generate(model.getLeft());
		
		if (!iterator.hasNext()) {
			
			persistOrDoNextIteration(model, notFilteredResults, generators);
			
		} else {
		
			while (iterator.hasNext()) {
				Pair<BpmnModel, GenerationInfo> nextModel = iterator.next();
				
				List<GenerationInfo> generationInfos = Lists.newArrayList(model.getRight());
				generationInfos.add(nextModel.getRight());
				
				persistOrDoNextIteration(Pair.of(nextModel.getLeft(), generationInfos), notFilteredResults, generators);
			}
		}
	}

	

	private void persistOrDoNextIteration(
			Pair<BpmnModel, List<GenerationInfo>> model, 
			Multimap<BpmnModel, GenerationInfo> notFilteredResults, 
			List<IGenerator> generators)
			throws IOException, Exception {
		
		if (generators.isEmpty()) {
			notFilteredResults.putAll(model.getLeft(), model.getRight());
			
		}else {
			generateTestCases(model, notFilteredResults, generators.get(0), generators.subList(1, generators.size()));
		}
	}


	@Override
	public String getName() {
		return processorName;
	}	

	
	
	private List<List<GenerationInfo>> getValuesInSameOrderAsModels(
			List<BpmnModel> models,
			Multimap<BpmnModel, GenerationInfo> notFilteredResults) {
		
		List<List<GenerationInfo>> infos = Lists.newArrayList();
		
		for (BpmnModel model : models) {
			infos.add(Lists.newArrayList(notFilteredResults.get(model)));
		}
		
		return infos;
	}
	
	
	
	
	
	
	
	
	private boolean allTestRequirementCovered(
			BpmnModel model,
			List<GenerationInfo> currentInfoSet) {
		
		for (IGenerator generator : generators) {
			if (!generator.allTestRequirementsCovered(model, currentInfoSet)) {
				logger.info("Some TC from generator: %s are not covered!", generator.getClass().getName());
				return false;
			}
		}
		
		return true;
	}
	
	private int countCoveredTestRequirementsNumber(
			BpmnModel model,
			List<GenerationInfo> currentInfoSet) {
		
		int number = 0;
		for (IGenerator generator : generators) {
			number += generator.countCoveredTestRequirementsNumber(model, currentInfoSet);
		}
		
		return number;
	}
	
	

	private void store(
			TestCasePersister persister, 
			List<BpmnModel> foundTestCasesSet) throws IOException {
		
		for (BpmnModel m : foundTestCasesSet)
			persister.persist(processorName, m);
	}
	
}
