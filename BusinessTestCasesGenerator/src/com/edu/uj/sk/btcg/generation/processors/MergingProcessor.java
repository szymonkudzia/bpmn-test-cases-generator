package com.edu.uj.sk.btcg.generation.processors;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.GenerationInfo;
import com.edu.uj.sk.btcg.logging.CLogger;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

public class MergingProcessor implements IProcessor {
	private CLogger logger = CLogger.getLogger(MergingProcessor.class);
	
	private List<IGenerator> generators;
	private String processorName;
	private boolean greedyOptimization;
	private boolean fullOptimization;
	private boolean randomSamplingOptimization;
	private int sampleSize;
	
	public MergingProcessor(
			String processorName, 
			List<IGenerator> generators, 
			boolean greedyOptimization,
			boolean fullOptimization, 
			boolean randomSamplingOptimization, 
			int sampleSize) {
		
		Preconditions.checkNotNull(generators);
		Preconditions.checkArgument(!generators.isEmpty());

		this.generators = generators;
		this.processorName = processorName;
		this.greedyOptimization = greedyOptimization;
		this.fullOptimization = fullOptimization;
		this.randomSamplingOptimization = randomSamplingOptimization;
		this.sampleSize = sampleSize;
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
			greedyOptimization(models, infos, model, persister);
			
			
			
		} else if (fullOptimization) {
			logger.info("Full optimization started...");
			logger.info("Number of models: %s, number of combinations to check: %s", models.size(), Math.pow(2.0, models.size()));
			
			findMinimalSetCoveringAllTestRequirements(model, persister, models, infos);
			
			logger.info("Full optimization finished. Models stored");
			
		} else if (randomSamplingOptimization) {
			models = selectSample(models);
			infos = getValuesInSameOrderAsModels(models, notFilteredResults);
			
			logger.info("Random sampling optimization started...");
			logger.info("Number of models: %s, number of combinations to check: %s", models.size(), Math.pow(2.0, models.size()));
			
			findMinimalSetWithBestCovering(model, persister, models, infos);
			
			logger.info("Random sampling optimization finished. Models stored");
			
		} else {
			logger.info("Storing generated models...");
			store(persister, models);
			
			logger.info("Generated models stored");
		}
	}


	private void greedyOptimization(
			final List<BpmnModel> models,
			final List<List<GenerationInfo>> infos,
			final BpmnModel original,
			TestCasePersister persister) throws IOException {
		
		List<Triple<BpmnModel, List<GenerationInfo>, Integer>> mi = IntStream.range(0, models.size())
		.mapToObj(i -> Triple.of(models.get(i), infos.get(i), countCoveredTestRequirementsNumber(models.get(i), infos.get(i))))
		.collect(Collectors.toList());
		
		mi = mi.stream()
			.sorted((a1, a2) -> a2.getRight() - a1.getRight())
			.collect(Collectors.toList());
		
		List<GenerationInfo> infosTotal = Lists.newArrayList();
		for (int i = 0; i < mi.size(); ++i) {
			Triple<BpmnModel, List<GenerationInfo>, Integer> modelInfo = mi.get(i);
			
			
			if (infosTotal.containsAll(modelInfo.getMiddle()))
				continue;
			
			infosTotal.addAll(modelInfo.getMiddle());
			
			persister.persist(processorName, modelInfo.getLeft());
			if (allTestRequirementCovered(original, infosTotal))
				break;
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


	private List<BpmnModel> selectSample(List<BpmnModel> models) {
		Collections.shuffle(models);
		return models.subList(0, sampleSize);
	}
	
	
	private void findMinimalSetCoveringAllTestRequirements(BpmnModel model,
			TestCasePersister persister, List<BpmnModel> models,
			List<List<GenerationInfo>> infos) throws IOException {
		
		Iterator<List<Integer>> powerSetIterator = 
				CCollections.powerSetIterator(CCollections.range(models.size()));
		
		while (powerSetIterator.hasNext()) {
			List<Integer> currentSet = powerSetIterator.next();
			
			List<GenerationInfo> currentInfoSet = pickInfosAndMergeAsSingleList(currentSet, infos);
			
			boolean found = allTestRequirementCovered(model, currentInfoSet);
			
			if (found) {
				storeFoundBestTestCases(persister, models, currentSet);
				
				return;
			}
		}
	}
	
	
	
	private void findMinimalSetWithBestCovering(
			BpmnModel model,
			TestCasePersister persister, 
			List<BpmnModel> models,
			List<List<GenerationInfo>> infos) throws IOException {
		
		int bestCoveredTestRequirementsNumber = 0;
		List<Integer> bestSet = Lists.newArrayList();
		
		Iterator<List<Integer>> powerSetIterator = 
				CCollections.powerSetIterator(CCollections.range(models.size()));
		while (powerSetIterator.hasNext()) {
			List<Integer> currentSet = powerSetIterator.next();
			
			List<GenerationInfo> currentInfoSet = pickInfosAndMergeAsSingleList(currentSet, infos);
			
			int coveredTestRequirementsNumber = countCoveredTestRequirementsNumber(model, currentInfoSet);
			
			if (coveredTestRequirementsNumber > bestCoveredTestRequirementsNumber) {
				bestCoveredTestRequirementsNumber = coveredTestRequirementsNumber;
				bestSet = currentSet;
			}
		}
		
		
		storeFoundBestTestCases(persister, models, bestSet);
	}







	
	
	

	






	private List<GenerationInfo> pickInfosAndMergeAsSingleList(
			List<Integer> currentSet, 
			List<List<GenerationInfo>> infos) {
		
		return currentSet
				.stream()
				.map(i -> infos.get(i))
				.reduce(Lists.newArrayList(), (acc, i) -> {
					acc.addAll(i);
					
					return acc;
				});
				
	}


	private List<BpmnModel> pickModelsAndMergeAsSingleList(
			List<Integer> currentSet,
			List<BpmnModel> models) {
		
		return currentSet
				.stream()
				.map(i -> models.get(i))
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
	
	private void storeFoundBestTestCases(
			TestCasePersister persister,
			List<BpmnModel> models, 
			List<Integer> currentSet) throws IOException {
		
		List<BpmnModel> foundTestCasesSet = pickModelsAndMergeAsSingleList(currentSet, models);
		
		store(persister, foundTestCasesSet);
	}


	private void store(
			TestCasePersister persister, 
			List<BpmnModel> foundTestCasesSet) throws IOException {
		
		for (BpmnModel m : foundTestCasesSet)
			persister.persist(processorName, m);
	}
}
