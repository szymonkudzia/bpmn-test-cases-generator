package com.edu.uj.sk.btcg.persistance;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.processors.Stats;
import com.edu.uj.sk.btcg.logging.CLogger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TestCasePersister {
	private static final CLogger logger = CLogger.getLogger(TestCasePersister.class);
	public static final String FILE_EXTENSION = "bpmn";
	
	private Map<String, Integer> total = Maps.newHashMap();
	private Map<String, Integer> unique = Maps.newHashMap();
	private Map<String, String> models = Maps.newHashMap(); 
	
	private int fileIndex = 0;
	private File outputDirectory;
	
	public TestCasePersister(File outputDirectory) throws IOException {
		this.outputDirectory = outputDirectory;
	}
	
	
	public void persist(String namespace, BpmnModel model) throws IOException {
		persist(namespace, model, false);
	}

	
	public void persist(String namespace, BpmnModel model, boolean preserveDuplications) throws IOException {
		String content = BpmnUtil.toString(model);

		incrementCount(total, namespace);
		
		if (modelNotUnique(content)) {
			if (!preserveDuplications) {
				return;
			}
		} else {
			incrementCount(unique, namespace);
		}
		
		registerModelAsUsed(content);
		
		File destination = determineModelDestination(namespace);
		saveOutput(destination, content);
	}


	public List<Stats> getStats() {
		List<Stats> stats = Lists.newArrayList();
		
		for (String strategyName : total.keySet()) {
			Stats stat = new Stats(
					strategyName, 
					total.getOrDefault(strategyName, 0), 
					unique.getOrDefault(strategyName, 0)
			);
			
			stats.add(stat);
		}
		
		return stats;
	}
	
	


	protected void saveOutput(
			File destination,
			String generatedTestCase) throws IOException {
		
		Path path = createPath(destination, ++fileIndex);
			
		Files.write(path, generatedTestCase.getBytes(Charset.forName("UTF-8")), StandardOpenOption.CREATE);
	}
	
	
	
	private Path createPath(File outputDirectory, int i) {
		String fileName = String.format("%s_%03d.%s", this.outputDirectory.getName().replace("_test_cases", ""), i, FILE_EXTENSION);
		File file = new File(outputDirectory,  fileName);
		
		return file.toPath();
	}

	
	private void registerModelAsUsed(String content) {
		models.put(content, "");
	}
	
	
	private boolean modelNotUnique(String content) {
		return models.containsKey(content);
	}

	
	private File determineModelDestination(String namespace) {
		File destination = new File(this.outputDirectory, namespace);
		destination.mkdirs();
		
		return destination;
	}
	
	
	private void incrementCount(Map<String, Integer> countMap, String namespace) {
		Integer count = countMap.getOrDefault(namespace, 0);
		
		countMap.put(namespace, ++count);
	}
}

