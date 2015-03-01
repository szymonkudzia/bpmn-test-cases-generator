package com.edu.uj.sk.btcg.bpmn;

import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;

public class BpmnQueries {
	/**
	 * Select all flow elements of main process in given model
	 * 
	 * @param model
	 * @return not null list
	 */
	public static List<FlowElement> selectAll(BpmnModel model) {
		return model.getMainProcess().getFlowElements().stream().collect(Collectors.toList());
	}
	
	/**
	 * Select all flow elements of main process in given @model of type @clazz
	 * 
	 * @param model
	 * @param clazz
	 * @return not null list
	 */
	public static <T> List<T> selectAllOfType(BpmnModel model, Class<T> clazz) {
		return selectAll(model).stream()
				.filter(x -> clazz.isAssignableFrom(x.getClass()))
				.map(x -> clazz.cast(x))
				.collect(Collectors.toList());
	}
}
