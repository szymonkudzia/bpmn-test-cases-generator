package com.edu.uj.sk.btcg.logic.extractors;

import java.util.Map;

import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.ScriptTask;
import org.activiti.bpmn.model.SequenceFlow;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

public class VariableValueExtractor implements IVariableValueExtractor {
	private 
	Map<Class<? extends FlowElement>, IVariableValueExtractor> 
		extractors = 
			getMapBuilder()
				.put(SequenceFlow.class, VariableValueExtractorFromCondition.create())
				.put(ScriptTask.class, VariableValueExtractorFromScriptTask.create())
				.build();
	
	private VariableValueExtractor() {}
	public static VariableValueExtractor create() {
		return new VariableValueExtractor();
	}
	
	
	/**
	 * Extract Multimap of variable values for given @flowElement
	 * 
	 * @param flowElement (cannot be null)
	 */
	public Multimap<String, Object> extract(FlowElement flowElement) {
		Preconditions.checkNotNull(flowElement, nullValueMsg());
		
		IVariableValueExtractor extractor = extractors.get(flowElement.getClass());
		
		return extractor != null ? 
			extractor.extract(flowElement) : HashMultimap.create();
	}

	
	
	
	private Object nullValueMsg() {
		return "FlowElement cannot be null";
	}
	
	
	
	
	
	
	
	private static ImmutableMap.Builder<Class<? extends FlowElement>, IVariableValueExtractor> 
		getMapBuilder() {
		
		return ImmutableMap.<Class<? extends FlowElement>, IVariableValueExtractor>builder();
	}
}
