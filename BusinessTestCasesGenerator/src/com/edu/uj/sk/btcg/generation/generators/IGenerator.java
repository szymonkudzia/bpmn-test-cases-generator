package com.edu.uj.sk.btcg.generation.generators;

import java.util.Iterator;

import org.activiti.bpmn.model.BpmnModel;

public interface IGenerator {
	Iterator<BpmnModel> generate(BpmnModel originalModel);
}
