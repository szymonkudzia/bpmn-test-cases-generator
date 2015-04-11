package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.ExclusiveGateway;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;

import com.edu.uj.sk.btcg.bpmn.BpmnGraphTraversal;
import com.edu.uj.sk.btcg.bpmn.BpmnGraphTraversalWithDefaultElementMarking;
import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class CoverageByPaths implements IGenerator {
	private int kNearestDecisionNodes = 1;

	
	public CoverageByPaths(int kNearestDecisionNodes) {
		this.kNearestDecisionNodes = kNearestDecisionNodes;
	}


	@Override
	public Iterator<BpmnModel> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}

	
	private class It extends AbstractGenerationIterator {
		private List<String> notUsedConnectionsIds = Lists.newArrayList();
		
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			
			List<ExclusiveGateway> gateways =
					BpmnQueries.selectAllOfType(originalModel, ExclusiveGateway.class);
			
			for (ExclusiveGateway gateway : gateways) {
				for (SequenceFlow connection : gateway.getOutgoingFlows()) {
					notUsedConnectionsIds.add(connection.getId());
				}
			}
		}



		@Override
		public boolean hasNext() {
			return !notUsedConnectionsIds.isEmpty();
		}

		
		@Override
		public BpmnModel next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);		
			
			
			Traverser traverser = new Traverser();
			
			traverser.traverse(currentTestCase);
			
			List<SequenceFlow> path = traverser.getPath();
			
			for (SequenceFlow connection : path) {
				notUsedConnectionsIds.remove(connection.getId());
				
				String sourceRef = connection.getSourceRef();
				ExclusiveGateway gateway = 
					(ExclusiveGateway) currentTestCase.getFlowElement(sourceRef);
				
				List<SequenceFlow> toRemove = Lists.newArrayList(gateway.getOutgoingFlows());
				toRemove.remove(connection);
				
				for (SequenceFlow tr : toRemove) 
					removeSequenceFlow(currentTestCase, tr);
			}
			
			
			removeUnconnectedElements(currentTestCase);
			
			return currentTestCase;
		}
		
		
		
		
		private class Traverser extends BpmnGraphTraversal<Traverser.Context> {
			private Context currentContext;

			private int maxNewConnectionsCount = 0;
			private int longestPathLength = 0;
			private List<SequenceFlow> connectionsInPath = Lists.newArrayList();
			
			public List<SequenceFlow> getPath() {
				return connectionsInPath;
			}
			
			
			@Override
			protected void doProcessing(Context context, BpmnModel model) {
				currentContext = context;
				FlowElement element = context.getCurrentElement();
				
				if (element instanceof EndEvent) {
					int newConnectionsCount = 0;
					
					for (SequenceFlow c : currentContext.getPath()) {
						if (notUsedConnectionsIds.contains(c.getId())) {
							++newConnectionsCount;
						}
					}

					if (newConnectionsCount > maxNewConnectionsCount) {
						maxNewConnectionsCount = newConnectionsCount;
						longestPathLength = currentContext.getPathLength();
						connectionsInPath = currentContext.getPath();
						
					}/* else if (currentContext.getPathLength() > longestPathLength) {
						boolean containsUnusedConnection = newConnectionsCount > 0;
						
						if (containsUnusedConnection) {
							longestPathLength = currentContext.getPathLength();
							connectionsInPath = currentContext.getPath();
						}
					}*/
				}				
			}


			@Override
			protected Optional<Context> getInitialContext(BpmnModel model) {
				StartEvent startEvent = selectStartEvent(model);
				
				return Optional.of(new Context(startEvent));
			}
			
			

			@Override
			protected Optional<Context> getContext(
					FlowElement element,
					BpmnModel model) {	
				
				List<SequenceFlow> path = Lists.newArrayList(currentContext.getPath());
				
				if (element instanceof SequenceFlow) {
					SequenceFlow connection = (SequenceFlow) element;
					
					FlowElement sourceElement = model.getFlowElement(connection.getSourceRef());
					
					if (sourceElement instanceof ExclusiveGateway) {
						path.add(connection);
					}
				}
				
				
				Context context = new Context(
					element, 
					currentContext.getPathLength() + 1, 
					path,
					currentContext.visited);
											
				
				return Optional.of(context);
			}
			
			
			
			@Override
			protected void markAsVisited(Context context, FlowElement element) {
				context.markAsVisited(element);
			}


			@Override
			protected boolean wasVisited(Context context, FlowElement element) {
				return context.wasVisisted(element);
			}
			
			

			private class Context implements BpmnGraphTraversalWithDefaultElementMarking.IContext {
				public FlowElement element;
				public int length = 0;
				public List<SequenceFlow> connections = Lists.newArrayList();
				public Map<String, FlowElement> visited = Maps.newHashMap();
				
				
				public Context(FlowElement element) {
					this.element = element;
				}
				
				public Context(
						FlowElement element, 
						int length, 
						List<SequenceFlow> connections,
						Map<String, FlowElement> visited) {
					
					this.element = element;
					this.length = length;
					this.connections = Lists.newArrayList(connections);
					this.visited = Maps.newHashMap(visited);
				}
				
				public boolean wasVisisted(FlowElement element) {
					return visited.containsKey(element.getId());
				}

				public void markAsVisited(FlowElement element) {
					visited.put(element.getId(), element);

				}

				public List<SequenceFlow> getPath() {
					return connections;
				}

				@Override
				public FlowElement getCurrentElement() {
					return element;
				}
				
				public int getPathLength() {
					return length;
				}
			}
		}
		
		
		
		
		
		
		private StartEvent selectStartEvent(BpmnModel currentTestCase) {
			List<StartEvent> startEvents = BpmnQueries.selectAllOfType(currentTestCase, StartEvent.class);
			
			Preconditions.checkArgument(!startEvents.isEmpty());
			
			return startEvents.get(0);
		}
		
	}
	
}
