package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.Association;
import org.activiti.bpmn.model.BaseElement;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.ExclusiveGateway;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.FlowNode;
import org.activiti.bpmn.model.GraphicInfo;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.bpmn.model.SubProcess;
import org.activiti.bpmn.model.TextAnnotation;

import com.google.common.collect.Lists;

abstract class AbstractGenerationIterator implements Iterator<BpmnModel> {
	protected BpmnModel originalModel;
	
	public AbstractGenerationIterator(BpmnModel originalModel) {
		this.originalModel = originalModel;
	}
	
	
	@SuppressWarnings("unchecked")
	protected <T extends FlowElement> T getFlowElement(BpmnModel model, T flowElement) {
		return (T)model.getFlowElement(flowElement.getId());
	}
	
	/**
	 * Return graphic information for specified element 
	 * 
	 * @param model
	 * @param element
	 * @return
	 */
	protected GraphicInfo getGraphicInfo(BpmnModel model, BaseElement element) {
		GraphicInfo gi = model.getGraphicInfo(element.getId());
		if (gi != null) return gi;

		List<GraphicInfo> gis = model.getFlowLocationGraphicInfo(element.getId());
		
		if (!gis.isEmpty()) return gis.get(0);

		throw new IllegalStateException("Could not find GraphicInfo for element with id: " + element.getId());
	}
	
	protected ExclusiveGateway getExclusiveGateway(BpmnModel model, ExclusiveGateway gateway) {
		return (ExclusiveGateway)getFlowElement(model, gateway);		
	}
	
	
	/**
	 * Remove given connection from model (with graphical information about it)
	 * 
	 * @param model from which connection will be deleted
	 * @param connection which will be deleted
	 */
	protected void removeSequenceFlow(
			BpmnModel model,
			SequenceFlow connection) {

		ConnectionRemover.removeSequenceFlow(model, connection);
	}

	
		
	
	/**
	 * Remove collection of flow elements from given model
	 * 
	 * @param model
	 * @param toBeRemoved
	 */
	protected void removeFlowElements(
			BpmnModel model,
			Collection<FlowElement> toBeRemoved) {
		
		FlowElementsRemover.removeFlowElements(model, toBeRemoved);
	}
	
	/**
	 * Remove single flow element from given model
	 * 
	 * @param model
	 * @param toBeRemoved
	 */
	protected void removeFlowElement(
			BpmnModel model,
			FlowElement toBeRemoved) {
		
		removeFlowElements(model, Lists.newArrayList(toBeRemoved));
	}
	
	
	protected void removeUnconnectedElements(BpmnModel model) {
		FlowElementsRemover.removeUnconnectedElements(model);
	}
	
	
	
	
	
	/**
	 * Select all sub elements for given list of elements
	 * (it is recursively executed until all sub elements are found)
	 * 
	 * @param flowElements
	 * @return list of all sub elements for given list
	 */
	protected Collection<FlowElement> selectAllSubFlowElements(Collection<FlowElement> flowElements) {
		List<FlowElement> result = Lists.newArrayList();
		
		List<SubProcess> subProcesses = flowElements
				.stream()
				.filter(e -> e instanceof SubProcess)
				.map(SubProcess.class::cast)
				.collect(Collectors.toList());
		
		for (SubProcess process : subProcesses) {
			Collection<FlowElement> subElements = process.getFlowElements();
			
			result.addAll(subElements);
			result.addAll(selectAllSubFlowElements(subElements)); 
		}
		
		return result;
	}
	
	
	
	/**
	 * Create graphic annotation for given element
	 * 
	 * @param model
	 * @param annotationText
	 * @param e
	 */
	protected void createAnnotationForElement(
			BpmnModel model,
			String annotationText,
			FlowElement e) {
		
		FlowElement event = getFlowElement(model, e);
		GraphicInfo eventGraphicInfo = getGraphicInfo(model, e);
		
		TextAnnotation textAnnotation = new TextAnnotation();
		textAnnotation.setId(event.getId() + "_textAnnotation");
		textAnnotation.setText(annotationText);
		
		Association association = new Association();
		association.setSourceRef(event.getId());
		association.setTargetRef(textAnnotation.getId());
		
		model.getMainProcess().addArtifact(textAnnotation);
		model.getMainProcess().addArtifact(association);
		
		int height = computeAnnotationHeight(annotationText);
		
		GraphicInfo graphicInfo = new GraphicInfo();
		graphicInfo.setX(eventGraphicInfo.getX());
		graphicInfo.setY(eventGraphicInfo.getY() - height - 50);
		graphicInfo.setWidth(100);
		graphicInfo.setHeight(height);
		
		model.addGraphicInfo(textAnnotation.getId(), graphicInfo);
	}


	private int computeAnnotationHeight(String text) {
		final int CHARACTERS_NUMBER_PER_LINE = 14;
		
		int currentLineLen = 0;
		int lineCount = 1;
		for (String word : text.split("\\s")) {
			currentLineLen += word.length() + 1;
			
			if (currentLineLen > CHARACTERS_NUMBER_PER_LINE) {
				currentLineLen = 0;
				++lineCount; 
			}
			
		}
		
		return lineCount * 25;
	}
	
	
}














class FlowElementsRemover {
	public static void removeFlowElements(
			BpmnModel model,
			Collection<FlowElement> toBeRemoved) {
		
		for (FlowElement element : toBeRemoved) {
			if (element instanceof FlowNode) {
				FlowNode flowNode = (FlowNode) element;
				
				for (SequenceFlow sequenceFlow : flowNode.getOutgoingFlows()) {
					ConnectionRemover.removeSequenceFlow(model, sequenceFlow);
				}
				
				for (SequenceFlow sequenceFlow : flowNode.getIncomingFlows()) {
					ConnectionRemover.removeSequenceFlow(model, sequenceFlow);
				}
			}
		}
		
		model.getMainProcess().getFlowElements().removeAll(toBeRemoved);
		
		for (FlowElement flowElement : toBeRemoved)
			model.removeGraphicInfo(flowElement.getId());
	}
	

	
	
	public static void removeUnconnectedElements(BpmnModel model) {
		boolean elementWasRemoved = false;
		do {
			elementWasRemoved = false;
			
			List<FlowElement> toBeRemoved = new ArrayList<FlowElement>();
			for (FlowElement element : model.getMainProcess().getFlowElements()) {
				if (element instanceof FlowNode) {
					if (element instanceof StartEvent) continue;
					
					FlowNode flowNode = (FlowNode)element;
					if (flowNode.getIncomingFlows().isEmpty()) {
						toBeRemoved.add(flowNode);
						elementWasRemoved = true;
					}
				}
			}
			
			removeFlowElements(model, toBeRemoved);
			
		} while (elementWasRemoved);
	}
}

















class ConnectionRemover {
	private static ConnectionRemover remover = new ConnectionRemover();
	
	public static void removeSequenceFlow(BpmnModel model, SequenceFlow connection) {
		remover.remove(model, connection);
	}
	
	
	
	private void remove(BpmnModel model, SequenceFlow connection) {
		Process process = model.getMainProcess();
		
		if (processContainsConnection(process, connection))
			removeSequenceFlowFromProcess(process, connection);
		else {
			Optional<SubProcess> subProcess = 
					selectSubProcessContainingConnection(process.getFlowElements(), connection);
			
			if (subProcess.isPresent()) 
				removeSequenceFlowFromProcess(subProcess.get(), connection);
		}
		
		model.removeGraphicInfo(connection.getId());
	}
	

	private Optional<SubProcess> selectSubProcessContainingConnection(Collection<FlowElement> flowElements, SequenceFlow connection) {
		List<FlowElement> subProcesses = flowElements
				.stream()
				.filter(e -> e instanceof SubProcess)
				.map(SubProcess.class::cast)
				.collect(Collectors.toList());
		
		if (subProcesses.isEmpty()) return Optional.empty();
		
		
		for (FlowElement subProcess : subProcesses) {
			if (processContainsConnection(subProcess, connection)) {
				return Optional.of((SubProcess)subProcess);
			}
		}
		
		
		return selectSubProcessContainingConnection(subProcesses, connection);
	}
	


	private void removeSequenceFlowFromProcess(Process process, SequenceFlow connection) {
		FlowNode target = (FlowNode)process.getFlowElement(connection.getTargetRef());
		
		target.getIncomingFlows().remove(connection);
		process.getFlowElements().remove(connection);
	}
	
	
	private void removeSequenceFlowFromProcess(SubProcess process, SequenceFlow connection) {
		FlowNode target = (FlowNode)process.getFlowElement(connection.getTargetRef());
		
		target.getIncomingFlows().remove(connection);
		process.getFlowElements().remove(connection);
	}
	
	
	private boolean processContainsConnection(Process process, SequenceFlow connection) {
		return process.getFlowElement(connection.getTargetRef()) != null;
	}
	
	
	private boolean processContainsConnection(FlowElement element, SequenceFlow connection) {
		if (!(element instanceof SubProcess)) return false;
		
		SubProcess process = (SubProcess) element;
		return process.getFlowElement(connection.getTargetRef()) != null;
	}


}
