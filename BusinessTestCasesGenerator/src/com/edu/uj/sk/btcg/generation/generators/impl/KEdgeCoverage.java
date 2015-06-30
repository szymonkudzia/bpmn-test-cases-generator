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
import org.apache.commons.lang3.tuple.Pair;

import com.edu.uj.sk.btcg.bpmn.BpmnGraphTraversal;
import com.edu.uj.sk.btcg.bpmn.BpmnGraphTraversalWithDefaultElementMarking;
import com.edu.uj.sk.btcg.bpmn.BpmnQueries;
import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.generators.IGenerator;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class KEdgeCoverage implements IGenerator {
	private int kNearestDecisionNodes = 1;

	
	public KEdgeCoverage(int kNearestDecisionNodes) {
		this.kNearestDecisionNodes = kNearestDecisionNodes;
	}


	@Override
	public Iterator<Pair<BpmnModel, GenerationInfo>> generate(BpmnModel originalModel) {
		return new It(originalModel);
	}

	
	
	
	@Override
	public boolean allTestRequirementsCovered(BpmnModel model,
			List<GenerationInfo> generationInfos) {
		
		List<List<String>> allTestRequirements = allTestRequirements(model);
		if (allTestRequirements.isEmpty()) return true;

		
		List<List<String>> coveredTestRequirements = coveredTestRequirements(generationInfos);
		
		
		
		return coveredTestRequirements.containsAll(allTestRequirements);
	}

	
	@Override
	public int countCoveredTestRequirementsNumber(BpmnModel model,
			List<GenerationInfo> currentInfoSet) {
		List<List<String>> allTestRequirements = allTestRequirements(model);
		int allCount = allTestRequirements.size();
		
		allTestRequirements.removeAll(coveredTestRequirements(currentInfoSet));
		
		return allCount - allTestRequirements.size();
	}
	

	private List<List<String>> coveredTestRequirements(
			List<GenerationInfo> generationInfos) {
		List<List<String>> coveredTestRequirements = generationInfos
				.stream()
				.filter(i -> i instanceof KEdgeInfo)
				.map(i -> (KEdgeInfo) i)
				.map(i -> i.path)
				.collect(Collectors.toList());
		return coveredTestRequirements;
	}


	private List<List<String>> allTestRequirements(BpmnModel model) {
		List<List<String>> allTestRequirements = new It(model).getTestRequirements();
		return allTestRequirements;
	}




	private class It extends AbstractGenerationIterator {
		List<List<String>> testRequirements = Lists.newArrayList();
		
		public It(BpmnModel originalModel) {
			super(originalModel);
			List<List<SequenceFlow>> testPaths = Lists.newArrayList();

			
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
			
			testRequirements = BpmnQueries.toListOfIdList(testPaths);
		}
		
		
		public List<List<String>> getTestRequirements() {
			return testRequirements;
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
			return !testRequirements.isEmpty();
		}

		
		@Override
		public Pair<BpmnModel, GenerationInfo> next() {
			BpmnModel currentTestCase = BpmnUtil.clone(originalModel);		
			
			List<String> pathIds = testRequirements.remove(0);
			
			List<SequenceFlow> connections = BpmnQueries
					.selectAllOfType(currentTestCase, SequenceFlow.class);
			
			List<SequenceFlow> toRemove = Lists.newArrayList();
			
			for (SequenceFlow connection : connections) {
				if (!pathIds.contains(connection.getId()))
					toRemove.add(connection);				
			}
			
			for (SequenceFlow tr : toRemove) 
				BpmnQueries.removeSequenceFlow(currentTestCase, tr);
			
			BpmnQueries.removeUnconnectedElements(currentTestCase);
			
			return Pair.of(currentTestCase, KEdgeInfo.create(pathIds));
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
			
			Preconditions.checkArgument(!startEvents.isEmpty(), "No start event in the model");
			
			return startEvents.get(0);
		}
		
	}
	
}



class KEdgeInfo extends GenerationInfo {
	public List<String> path = Lists.newArrayList();
	
	public KEdgeInfo() {}
	
	public KEdgeInfo(List<String> path) {
		this.path.addAll(path);
	}
	
	
	public static KEdgeInfo create() {
		return new KEdgeInfo();
	}
	
	public static KEdgeInfo create(List<String> path) {
		return new KEdgeInfo(path);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		KEdgeInfo other = (KEdgeInfo) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	
	
}
