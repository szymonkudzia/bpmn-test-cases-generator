package com.edu.uj.sk.btcg.generation.generators;

import java.util.Iterator;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.generation.generators.impl.GenerationInfo;

public interface IGenerator {
	Iterator<Pair<BpmnModel, GenerationInfo>> generate(BpmnModel originalModel);
	
	boolean allTestRequirementsCovered(BpmnModel model, List<GenerationInfo> generationInfos);
	
	int countCoveredTestRequirementsNumber(BpmnModel model, List<GenerationInfo> currentInfoSet);
}
