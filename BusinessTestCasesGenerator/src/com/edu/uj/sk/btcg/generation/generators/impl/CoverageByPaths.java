package com.edu.uj.sk.btcg.generation.generators.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.EndEvent;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.SequenceFlow;
import org.activiti.bpmn.model.StartEvent;

import com.edu.uj.sk.btcg.bpmn.BpmnGraphTraversal;
import com.edu.uj.sk.btcg.bpmn.BpmnGraphTraversalWithDefaultElementMarking;
import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
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
		private List<List<SequenceFlow>> testPaths = Lists.newArrayList();
		
		public It(BpmnModel originalModel) {
			super(originalModel);

			
			Traverser traverser = new Traverser();			
			traverser.traverse(originalModel);

			List<List<SequenceFlow>> allPaths = traverser.getPaths();
			List<List<SequenceFlow>> kConnectionsTuples = calculateAllKTuples(allPaths);
			
			
			int currentBestCoverage = Integer.MAX_VALUE;
			for (List<List<SequenceFlow>> paths : CCollections.powerSet(allPaths)) {
				if (paths.size() > currentBestCoverage) continue;
				
				if (coversAllPaths(paths, kConnectionsTuples)) {
					currentBestCoverage = paths.size();
					testPaths = paths;
				}
			}
		}



		private List<List<SequenceFlow>> calculateAllKTuples(
				List<List<SequenceFlow>> allPaths) {
			
			Set<List<SequenceFlow>> uniqueTuples = new HashSet<>();
			
			for (List<SequenceFlow> path : allPaths) {
				for (int i = 0; i < path.size() - kNearestDecisionNodes + 1; ++i) {
					uniqueTuples.add(path.subList(i, i + kNearestDecisionNodes));
				}
			}
			
			return Lists.newArrayList(uniqueTuples);
		}



		/**
		 * Check if source paths covers all paths in target.
		 * If all paths from target are sublists of one or more paths from source.
		 * 
		 * @param source
		 * @param target
		 * @return
		 */
		private boolean coversAllPaths(
			List<List<SequenceFlow>> source, 
			List<List<SequenceFlow>> target) {
			
			int covered = 0;
			for(List<SequenceFlow> tuple : target) {
				for (List<SequenceFlow> path : source) {
					if (path.size() < tuple.size()) continue;
					
					if (Collections.indexOfSubList(path, tuple) >= 0) {
						++covered;
						break;
					}
				}
			}
			
			return covered == target.size();
		}



		@Override
		public boolean hasNext() {
			return !testPaths.isEmpty();
		}

		
		@Override
		public BpmnModel next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);		
			
			List<SequenceFlow> path = testPaths.remove(0);
			List<String> pathIds = path.stream().map(p -> p.getId()).collect(Collectors.toList());
			
			List<SequenceFlow> connections = BpmnQueries
					.selectAllOfType(currentTestCase, SequenceFlow.class);
			
			List<SequenceFlow> toRemove = Lists.newArrayList();
			
			for (SequenceFlow connection : connections) {
				if (!pathIds.contains(connection.getId()))
					toRemove.add(connection);				
			}
			
			for (SequenceFlow tr : toRemove) 
				removeSequenceFlow(currentTestCase, tr);
			
			removeUnconnectedElements(currentTestCase);
			
			return currentTestCase;
		}
		
		
		
		
		private class Traverser extends BpmnGraphTraversal<Traverser.Context> {
			private Context currentContext;
			private List<List<SequenceFlow>> paths = Lists.newArrayList();
			
			public List<List<SequenceFlow>> getPaths() {
				return paths;
			}
			
			
			@Override
			protected void doProcessing(Context context, BpmnModel model) {
				currentContext = context;
				FlowElement element = context.getCurrentElement();
				
				if (element instanceof EndEvent) {
					paths.add(context.getPath());
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
					path.add((SequenceFlow) element);
				}
				
				
				Context context = new Context(
					element, 
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
				public List<SequenceFlow> connections = Lists.newArrayList();
				public Map<String, FlowElement> visited = Maps.newHashMap();
				
				
				public Context(FlowElement element) {
					this.element = element;
				}
				
				public Context(
						FlowElement element, 
						List<SequenceFlow> connections,
						Map<String, FlowElement> visited) {
					
					this.element = element;
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
				
			}
		}
		
		
		
		
		
		
		private StartEvent selectStartEvent(BpmnModel currentTestCase) {
			List<StartEvent> startEvents = BpmnQueries.selectAllOfType(currentTestCase, StartEvent.class);
			
			Preconditions.checkArgument(!startEvents.isEmpty());
			
			return startEvents.get(0);
		}
		
	}
	
}
