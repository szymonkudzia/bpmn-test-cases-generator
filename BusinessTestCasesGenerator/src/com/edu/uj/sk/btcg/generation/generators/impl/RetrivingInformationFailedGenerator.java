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

public class RetrivingInformationFailedGenerator implements IGenerator {
	private static final String ANNOTATION_TEXT = "Could not fetch data!";

	
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
		private List<FlowElement> tasks = Lists.newArrayList();
		
		public Generator(BpmnModel originalModel) {
			super(originalModel);
			
			tasks.addAll(BpmnQueries.selectAllOfType(originalModel, ScriptTask.class));
			tasks.addAll(BpmnQueries.selectAllOfType(originalModel, ServiceTask.class));
			
			tasks = tasks.stream()
					.filter(chooseArtifactProducer())
					.collect(Collectors.toList());
		}

		private Predicate<? super FlowElement> chooseArtifactProducer() {
			return e -> {
				String name = StringUtils
						.defaultString(e.getName())
						.toLowerCase();
				
				return 
					name.contains("get") ||
					name.contains("fetch") ||
					name.contains("aquire") ||
					name.contains("retrive") ||
					name.contains("download") ||
					name.contains("read");
			
			};
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
			
			return Pair.of(currentTestCase, null);
		}
	}
}
