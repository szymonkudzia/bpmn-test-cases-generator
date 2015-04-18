package com.edu.uj.sk.btcg.generation.processors;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.edu.uj.sk.btcg.generation.generators.impl.GenerationInfo;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

public class MergingProcessor implements IProcessor {
	private List<IGenerator> generators;
	private String processorName;
	private boolean optimizeResult;
	
	public MergingProcessor(String processorName, List<IGenerator> generators, boolean optimizeResult) {
		Preconditions.checkNotNull(generators);
		Preconditions.checkArgument(!generators.isEmpty());

		this.generators = generators;
		this.processorName = processorName;
		this.optimizeResult = optimizeResult;
	}
	
	
	@Override
	public void process(BpmnModel model, TestCasePersister persister) throws Exception {
		Preconditions.checkNotNull(model);
		Preconditions.checkNotNull(persister);
		
		Multimap<BpmnModel, GenerationInfo> notFilteredResults = HashMultimap.create();
		Pair<BpmnModel, List<GenerationInfo>> modelGenerationInfo = Pair.of(model, Lists.newArrayList());
		
		generateTestCases(modelGenerationInfo, notFilteredResults, generators.get(0), generators.subList(1, generators.size()));
		
		
		List<BpmnModel> models = Lists.newArrayList(notFilteredResults.keySet());
		List<List<GenerationInfo>> infos = getValuesInSameOrderAsModels(models, notFilteredResults);
		
		if (optimizeResult) {
			findMinimalSetCoveringAllTestRequirements(model, persister, models, infos);
		} else {
			store(persister, models);
		}
	}


	
	private void findMinimalSetCoveringAllTestRequirements(BpmnModel model,
			TestCasePersister persister, List<BpmnModel> models,
			List<List<GenerationInfo>> infos) throws IOException {
		List<List<Integer>> allCombinations = calculateAllPossibleCombinations(models.size());
		
		for (List<Integer> currentSet : allCombinations) {
			List<GenerationInfo> currentInfoSet = pickInfosAndMergeAsSingleList(currentSet, infos);
			
			boolean found = allTestRequirementCovered(model, currentInfoSet);
			
			if (found) {
				storeFoundBestTestCases(persister, models, currentSet);
				
				return;
			}
		}
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
	
	
	
	
	
	

	private List<List<Integer>> calculateAllPossibleCombinations(int size) {
		List<List<Integer>> allCombinations = CCollections.powerSet(CCollections.range(size));
		Collections.sort(allCombinations, (a, b) -> a.size() - b.size());
		
		return allCombinations;
	}

	
	
	private boolean allTestRequirementCovered(
			BpmnModel model,
			List<GenerationInfo> currentInfoSet) {
		
		for (IGenerator generator : generators) {
			if (!generator.allTestRequirementsCovered(model, currentInfoSet)) {
				return false;
			}
		}
		
		return true;
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
