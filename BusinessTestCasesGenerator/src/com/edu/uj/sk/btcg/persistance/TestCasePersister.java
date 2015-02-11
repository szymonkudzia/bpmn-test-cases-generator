package com.edu.uj.sk.btcg.persistance;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.activiti.bpmn.model.BpmnModel;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.logging.CLogger;
import com.google.common.collect.Maps;

public class TestCasePersister {
	private static final CLogger logger = CLogger.getLogger(TestCasePersister.class);
	public static final String FILE_EXTENSION = "bpmn";
	
	private Map<String, String> models = Maps.newHashMap(); 
	
	private int fileIndex = 0;
	private File outputDirectory;
	
	public TestCasePersister(File outputDirectory) throws IOException {
		this.outputDirectory = outputDirectory;
	}
	
	
	public void persist(String namespace, BpmnModel model) throws IOException {
		String content = BpmnUtil.toString(model);

		if (modelNotUnique(content)) return;
		registerModelAsUsed(content);
		
		File destination = determineModelDestination(namespace);
		saveOutput(destination, content);
	}


	


	private void saveOutput(
			File destination,
			String generatedTestCase) throws IOException {
		
		Path path = createPath(destination, ++fileIndex);
			
		logger.info("Writing output nr: %d to file: \"%s\"", fileIndex, path.toString());
			
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
}

