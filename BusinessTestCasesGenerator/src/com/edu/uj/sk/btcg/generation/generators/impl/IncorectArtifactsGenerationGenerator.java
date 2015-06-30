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

public class IncorectArtifactsGenerationGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT = "Task produces incorrect artifact";

	
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
					.filter(i -> i instanceof IncorrectArtifactsInfo)
					.map(i -> (IncorrectArtifactsInfo) i)
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
			
			return Pair.of(currentTestCase, IncorrectArtifactsInfo.create(task));
		}
		
		

		private Predicate<? super FlowElement> chooseArtifactProducer() {
			return e -> {
				String name = StringUtils
						.defaultString(e.getName())
						.toLowerCase();
				
				return 
					name.contains("generate") ||
					name.contains("produce") ||
					name.contains("create");
			
			};
		}
	}
}


class IncorrectArtifactsInfo extends GenerationInfo {
	public String task;

	public IncorrectArtifactsInfo(String task) {
		this.task = task;
	}

	public static IncorrectArtifactsInfo create(FlowElement task) {
		return new IncorrectArtifactsInfo(task.getId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((task == null) ? 0 : task.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IncorrectArtifactsInfo other = (IncorrectArtifactsInfo) obj;
		if (task == null) {
			if (other.task != null)
				return false;
		} else if (!task.equals(other.task))
			return false;
		return true;
	}
	
	
}