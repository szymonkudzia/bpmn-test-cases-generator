package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ScriptTask;
import org.activiti.bpmn.model.ServiceTask;

import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.collect.Lists;

public class ExceptionInAutomatedTasksGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT = "Execution of task was interrupted.";

	
	@Override
	public Iterator<BpmnModel> generate(BpmnModel originalModel) {
		return new Generator(originalModel);
	}
	
	
	private class Generator extends AbstractGenerationIterator {
		private List<FlowElement> userTasks = Lists.newArrayList();
		
		public Generator(BpmnModel originalModel) {
			super(originalModel);
			
			userTasks.addAll(BpmnQueries.selectAllOfType(originalModel, ScriptTask.class));
			userTasks.addAll(BpmnQueries.selectAllOfType(originalModel, ServiceTask.class));
		}

		@Override
		public boolean hasNext() {
			return !userTasks.isEmpty();
		}

		@Override
		public BpmnModel next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);
			FlowElement task = userTasks.remove(0);
			
			task = currentTestCase.getFlowElement(task.getId());
			
			createAnnotationForElement(currentTestCase, ANNOTATION_TEXT, task);
			
			return currentTestCase;
		}
	}
}
