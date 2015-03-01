package com.edu.uj.sk.btcg.logic.extractors;

import org.activiti.bpmn.model.ScriptTask;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class VariableValueExtractorFromScriptTask extends AbstractVariableValueExtractor<ScriptTask> {

	private VariableValueExtractorFromScriptTask() {}
	
	public static VariableValueExtractorFromScriptTask create() {
		return new VariableValueExtractorFromScriptTask();
	}

	@Override
	protected Multimap<String, Object> doExtraction(ScriptTask element) {
		return HashMultimap.create();
	}

}
