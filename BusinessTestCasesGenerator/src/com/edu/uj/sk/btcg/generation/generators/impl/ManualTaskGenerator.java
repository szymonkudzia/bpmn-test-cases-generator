package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;

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
		// TODO Auto-generated method stub
		return false;
	}







	private class Generator extends AbstractGenerationIterator {
		private List<FlowElement> userTasks = Lists.newArrayList();
		
		public Generator(BpmnModel originalModel) {
			super(originalModel);
			
			userTasks.addAll(BpmnQueries.selectAllOfType(originalModel, UserTask.class));
			userTasks.addAll(BpmnQueries.selectAllOfType(originalModel, ManualTask.class));
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
			
			return Pair.of(currentTestCase, null);
		}
	}
}
