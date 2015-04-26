package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ScriptTask;
import org.activiti.bpmn.model.ServiceTask;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.collect.Lists;

public class ExceptionInAutomatedTasksGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT = "Execution of task was interrupted.";

	
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
					.filter(i -> i instanceof ExceptionInTasksInfo)
					.map(i -> (ExceptionInTasksInfo) i)
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
		private List<FlowElement> automatedTasks = Lists.newArrayList();
		
		public Generator(BpmnModel originalModel) {
			super(originalModel);
			
			automatedTasks.addAll(BpmnQueries.selectAllOfType(originalModel, ScriptTask.class));
			automatedTasks.addAll(BpmnQueries.selectAllOfType(originalModel, ServiceTask.class));
		}
		
		public List<String> getAllTestRequirements() {
			return BpmnQueries.toIdList(automatedTasks);
		}

		@Override
		public boolean hasNext() {
			return !automatedTasks.isEmpty();
		}

		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);
			FlowElement task = automatedTasks.remove(0);
			
			task = currentTestCase.getFlowElement(task.getId());
			
			BpmnQueries.createAnnotationForElement(currentTestCase, ANNOTATION_TEXT, task);
			
			return Pair.of(currentTestCase, ExceptionInTasksInfo.create(task));
		}
	}
}


class ExceptionInTasksInfo extends GenerationInfo {
	public String task;

	public ExceptionInTasksInfo(String task) {
		this.task = task;
	}

	public static ExceptionInTasksInfo create(FlowElement task) {
		return new ExceptionInTasksInfo(task.getId());
	}
}
