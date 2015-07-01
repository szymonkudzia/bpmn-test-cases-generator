package com.edu.uj.sk.btcg;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.collections.CCollections;
import com.edu.uj.sk.btcg.generation.processors.IProcessor;
import com.edu.uj.sk.btcg.generation.processors.Processors;
import com.edu.uj.sk.btcg.generation.processors.ProcessorsExecuter;
import com.edu.uj.sk.btcg.generation.processors.Stats;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class BestStrategyCombinationsFinder {
	private List<IProcessor> processors = Lists.newArrayList(
			Processors.newCorruptedIncomingMessage(),
			Processors.newCorruptedOutgoingMessages(),
			Processors.newCoverageByAllPaths(),
			Processors.newCoverageByInputManipulation(),
			Processors.newExceptionInAutomatedTasks(),
			Processors.newHasRights(),
			Processors.newIncorrectArtifactsGeneration(),
			Processors.newInformationRetrivalTask(),
			Processors.newKEdgeCoverage(1),
			Processors.newLockInParallelGateway(),
			Processors.newManualTask()
			);
	
	private List<BpmnModel> models = Lists.newArrayList();
	
	private File outputDirectory;
	private File summaryFile;
	
	@Before
	public void beforeClass() throws IOException {
		File[] files = new File("best_strategy_combinations/input").listFiles();
		
		for (File file : files) {
			String content = Files.toString(file, Charsets.UTF_8);
			BpmnModel model = BpmnUtil.toBpmnModel(content);
			
			models.add(model);
		}
		
		outputDirectory = new File("target/best_strategy_combinations/output");
		FileUtils.deleteDirectory(outputDirectory);
		
		summaryFile = new File(outputDirectory, "summary.html");
		summaryFile.delete();
	}
	
	
	@Test
	public void calculateAllCombinationsStats() throws IOException {
		HtmlOutputBuilder outputBuilder = new HtmlOutputBuilder();
		Integer permutationIndex = 0;
		
		List<List<IProcessor>> combinations = 
				CCollections.powerSet(processors)
					.stream()
					.filter(x -> x.size() > 1)
					.collect(Collectors.toList());
		
		try {
			for (List<IProcessor> combination : combinations) {
				for (List<IProcessor> permutation : CCollections.permutations(combination)) {
					int index = ++permutationIndex;
					
					TestCasePersister persister = new TestCasePersister(new File(outputDirectory, "PermIdx_" + index));
					
					for (BpmnModel model : models) {
						ProcessorsExecuter.process(permutation, true, true, false, model, persister);
					}
					
					Stats stats = new Stats("", 0, 0);
					if (!persister.getStats().isEmpty())
						stats = persister.getStats().get(0);
					
					outputBuilder
						.addHeader(permutation)
						.addPermIndex(index)
						.addTotalModels(stats.getTotal())
						.addUniqueModels(stats.getUnique());
				}
			}
		} finally {
			String output = outputBuilder.build();
			Files.write(output, summaryFile, Charsets.UTF_8);
		}
	}
}


class HtmlOutputBuilder {
	private static final String DOCUMENT_TEMPLATE = 
			"<html><header><style>"	+
				"td { border-bottom: 1px solid rgb(160, 160, 160); }" +
				"th { background-color: grey; }" +
			"</style></header><body>" + 
				"<table style='border: 1px solid black'>" +
					"<tr>TABLE_HEADER</tr>" +
					"<tr>PERM_INDEXES</tr>" +
					"<tr>TOTAL_MODELS</tr>" +
					"<tr>UNIQUE_MODELS</tr>" +
				"</table>" +
			"</body></html>"
			;
	
	private static final String TABLE_CELL_TEMPLATE = "<td>CONTENT</td>";
	
	private static final String LIST_TEMPLATE = "<th><ol>CONTENT</ol></th>";
	private static final String LIST_ENTRY_TEMPLATE = "<li>ENTRY</li>";
	
	private StringBuilder tableHeader = new StringBuilder();
	private StringBuilder permIndexes = new StringBuilder();
	private StringBuilder totalModels = new StringBuilder();
	private StringBuilder uniqueModels = new StringBuilder();
	
	
	
	
	
	public HtmlOutputBuilder addHeader(List<IProcessor> processors) {
		StringBuilder content = new StringBuilder();
		
		for (IProcessor processor : processors) {
			content.append(LIST_ENTRY_TEMPLATE.replace("ENTRY", processor.getName()));
		}
		
		tableHeader.append(LIST_TEMPLATE.replace("CONTENT", content.toString()));
		
		return this;
	}
	
	
	public HtmlOutputBuilder addPermIndex(int permIdx) {
		permIndexes.append(TABLE_CELL_TEMPLATE.replace("CONTENT", Integer.toString(permIdx)));
		
		return this;
	}
	
	public HtmlOutputBuilder addTotalModels(int total) {
		totalModels.append(TABLE_CELL_TEMPLATE.replace("CONTENT", Integer.toString(total)));
		
		return this;
	}
	
	public HtmlOutputBuilder addUniqueModels(int unique) {
		uniqueModels.append(TABLE_CELL_TEMPLATE.replace("CONTENT", Integer.toString(unique)));
		
		return this;
	}
	
	
	
	
	
	public String build() {
		return DOCUMENT_TEMPLATE
				.replace("TABLE_HEADER", tableHeader.toString())
				.replace("PERM_INDEXES", permIndexes.toString())
				.replace("TOTAL_MODELS", totalModels.toString())
				.replace("UNIQUE_MODELS", uniqueModels.toString());
	}
}
