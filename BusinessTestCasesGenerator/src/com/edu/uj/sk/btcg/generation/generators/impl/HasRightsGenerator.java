package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ScriptTask;
import org.activiti.bpmn.model.ServiceTask;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.collect.Lists;

public class HasRightsGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT = "No rights";

	
	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(BpmnModel originalModel) {
		return new Generator(originalModel);
	}
	
	
	
	
	
	@Override
	public boolean allTestRequirementsCovered(BpmnModel model,
			List<GenerationInfo> generationInfos) {
		
		List<String> allTestRequirements = allTestRequirements(model);
		
		if (allTestRequirements.isEmpty()) return true;

		
		List<String> coveredTestRequirements = coveredTestRequirements(generationInfos);
		
		
		return coveredTestRequirements.containsAll(allTestRequirements);
	}

	@Override
	public int countCoveredTestRequirementsNumber(BpmnModel model,
			List<GenerationInfo> currentInfoSet) {
		List<String> allTestRequirements = allTestRequirements(model);
		int allCount = allTestRequirements.size();
		
		allTestRequirements.removeAll(coveredTestRequirements(currentInfoSet));
		
		return allCount - allTestRequirements.size();
	}




	private List<String> coveredTestRequirements(
			List<GenerationInfo> generationInfos) {
		List<String> coveredTestRequirements =
				generationInfos.stream()
					.filter(i -> i instanceof HasRightsInfo)
					.map(i -> (HasRightsInfo) i)
					.map(i -> i.task)
					.collect(Collectors.toList());
		return coveredTestRequirements;
	}





	private List<String> allTestRequirements(BpmnModel model) {
		List<String> allTestRequirements =
				new Generator(model).getAllTestRequirements();
		return allTestRequirements;
	}





	private class Generator extends AbstractGenerationIterator {
		private List<FlowElement> tasks = Lists.newArrayList();
		
		public Generator(BpmnModel originalModel) {
			super(originalModel);
			
			tasks.addAll(BpmnQueries.selectAllOfType(originalModel, ScriptTask.class));
			tasks.addAll(BpmnQueries.selectAllOfType(originalModel, ServiceTask.class));
			
			tasks = tasks.stream()
					.filter(chooseArtifactProducer())
					.collect(Collectors.toList());
		}
		
		public List<String> getAllTestRequirements() {
			return BpmnQueries.toIdList(tasks);
		}
		



		@Override
		public boolean hasNext() {
			return !tasks.isEmpty();
		}

		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);
			FlowElement task = tasks.remove(0);
			
			task = currentTestCase.getFlowElement(task.getId());
			
			BpmnQueries.createAnnotationForElement(currentTestCase, ANNOTATION_TEXT, task);
			
			return Pair.of(currentTestCase, HasRightsInfo.create(task));
		}
		
		
		private Predicate<? super FlowElement> chooseArtifactProducer() {
			return e -> {
				String name = StringUtils
						.defaultString(e.getName())
						.toLowerCase();
				
				return 
					name.contains("has access") ||
					name.contains("has rights") ||
					name.contains("is permitted") ||
					name.contains("is allowed");
			
			};
		}
	}
}

class HasRightsInfo extends GenerationInfo {
	public String task;

	public HasRightsInfo(String task) {
		this.task = task;
	}

	public static HasRightsInfo create(FlowElement task) {
		return new HasRightsInfo(task.getId());
	}
}