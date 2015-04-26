package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ManualTask;
import org.activiti.bpmn.model.UserTask;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.collect.Lists;

public class ManualTaskGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT;
	
	static {
		ANNOTATION_TEXT = "Performer of this task:\n\n"
			+ "1. Got sick and could not come to work.\n\n"
			+ "2. Is overloaded so he/she needs more time to proceed this task.\n\n"
			+ "3. Made mistake.\n\n"
			+ "4. Is about to quit this job.\n\n"
			;
	}

	
	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(BpmnModel originalModel) {
		return new Generator(originalModel);
	}
	
	
	
	@Override
	public boolean allTestRequirementsCovered(BpmnModel model,
			List<GenerationInfo> generationInfos) {
		
		List<String> allTestRequirements =
				new Generator(model).getAllTestRequirements();
		
		if (allTestRequirements.isEmpty()) return true;

		
		List<String> coveredTestRequirements =
				generationInfos.stream()
					.filter(i -> i instanceof ManualTaskInfo)
					.map(i -> (ManualTaskInfo) i)
					.map(i -> i.task)
					.collect(Collectors.toList());
		
		
		return coveredTestRequirements.containsAll(allTestRequirements);
	}







	private class Generator extends AbstractGenerationIterator {
		private List<FlowElement> userTasks = Lists.newArrayList();
		
		public Generator(BpmnModel originalModel) {
			super(originalModel);
			
			userTasks.addAll(BpmnQueries.selectAllOfType(originalModel, UserTask.class));
			userTasks.addAll(BpmnQueries.selectAllOfType(originalModel, ManualTask.class));
		}
		
		public List<String> getAllTestRequirements() {
			return BpmnQueries.toIdList(userTasks);
		}

		@Override
		public boolean hasNext() {
			return !userTasks.isEmpty();
		}

		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);
			FlowElement userTask = userTasks.remove(0);
			
			userTask = currentTestCase.getFlowElement(userTask.getId());
			
			BpmnQueries.createAnnotationForElement(currentTestCase, ANNOTATION_TEXT, userTask);
			
			return Pair.of(currentTestCase, ManualTaskInfo.create(userTask));
		}
	}
}


class ManualTaskInfo extends GenerationInfo {
	public String task;

	protected ManualTaskInfo(String task) {
		this.task = task;
	}

	public static ManualTaskInfo create(FlowElement task) {
		return new ManualTaskInfo(task.getId());
	}
}