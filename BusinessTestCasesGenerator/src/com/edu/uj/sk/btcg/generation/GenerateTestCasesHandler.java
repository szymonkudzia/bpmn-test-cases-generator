package com.edu.uj.sk.btcg.generation;


import java.io.File;
import java.io.IOException;
import java.util.List;

import org.activiti.bpmn.model.BpmnModel;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.edu.uj.sk.btcg.bpmn.BpmnUtil;
import com.edu.uj.sk.btcg.generation.processors.IProcessor;
import com.edu.uj.sk.btcg.generation.processors.ProcessingStats;
import com.edu.uj.sk.btcg.generation.processors.ProcessorsExecuter;
import com.edu.uj.sk.btcg.logging.CLogger;
import com.edu.uj.sk.btcg.persistance.TestCasePersister;
import com.google.common.collect.Lists;


/**
 * Handle calling "Generate Test Cases" from context menu in
 * PackageExplorer tab
 * 
 * @author Szymon Kudzia
 *
 */
public class GenerateTestCasesHandler extends AbstractHandler {
	private static final CLogger logger = CLogger.getLogger(GenerateTestCasesHandler.class);
	
	
	public GenerateTestCasesHandler() {
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
	    ISelection sel = HandlerUtil.getActiveMenuSelection(event);
	    IStructuredSelection selection = (IStructuredSelection) sel;

	    
	    Object firstElement = selection.getFirstElement();
	    
	    if (isSupportedFile(firstElement)) {
	    	GenerationStrategyPickerDialog dialog = new GenerationStrategyPickerDialog(shell);
	    	if (dialog.open() == Window.CANCEL) return null;
	    	
	    	
	    	IFile modelToProcess = castToIFile(firstElement);
	    	
	    	processFile(shell, dialog.getChosenProcessors(), dialog.asSingleStrategy(), modelToProcess);

	    	refreshPackageExplorer(modelToProcess);
	    	
	    } else {
	    	showInformationAboutWrongFile(shell);
	    }
	    
	    return null;
	}

	private void refreshPackageExplorer(IFile modelToProcess) throws ExecutionException {
		try {
			modelToProcess.getParent().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			throw new ExecutionException("Could not refresh package explorer", e);
		}
	}
	
	
	
	private void processFile(Shell shell, List<IProcessor> processors, boolean asSingleStrategy, IFile modelToProcess) {
		try {
			String modelXml = IOUtils.toString(modelToProcess.getContents());
			BpmnModel bpmnModel = BpmnUtil.toBpmnModel(modelXml);
			
			File outputDirectory = new File(castToFile(modelToProcess).getAbsolutePath() + "_test_cases");
			
			if (outputDirectory.exists()) {
				boolean overwrite = askIfOverwriteTestCases(shell, outputDirectory);
				
				if (!overwrite) {
					return;
				} else {
					FileUtils.deleteDirectory(outputDirectory);
					refreshPackageExplorer(modelToProcess);
				}
			}
			
			outputDirectory.mkdirs();
			
			TestCasePersister persistTestCase = new TestCasePersister(outputDirectory);
			
			List<ProcessingStats> stats =
					ProcessorsExecuter.process(processors, asSingleStrategy, bpmnModel, persistTestCase);
			
			List<ProcessingStats> uncombinedStrategiesStats = Lists.newArrayList();
			
			if (asSingleStrategy) 
				uncombinedStrategiesStats = 
					ProcessorsExecuter.process(processors, false, bpmnModel, falsePersister());
			
			showInformationAboutFinishedGeneration(shell, stats, uncombinedStrategiesStats);
			
		} catch (IOException | CoreException e) {
			logger.warn("Execption during test cases generations!", e);
			showInformationAboutBrokenFile(shell);
			
		} catch (Throwable e) {
			logger.warn("Exception during test cases generations!", e);
			shwoInformationAboutGeneralError(shell);
		}
	}





	


	


	/**
	 * Display info message that user should select file with
	 * .bpmn2 extension which is supported 
	 * 
	 * @param shell
	 * @param messgae
	 */
	private void showInformationAboutWrongFile(Shell shell) {
		MessageDialog
      	.openInformation(
    		  shell, 
    		  "Info",
    		  "Please select a *." + TestCasePersister.FILE_EXTENSION + " file");
	}
	
	/**
	 * Display info message that user should select file with
	 * .bpmn2 extension which is supported 
	 * 
	 * @param shell
	 * @param messgae
	 */
	private void showInformationAboutBrokenFile(Shell shell) {
		MessageDialog
      	.openInformation(
    		  shell, 
    		  "Info",
    		  "There was error during test cases generation, make sure selected file is correct!");
	}
	
	
	private void showInformationAboutFinishedGeneration(
			Shell shell, 
			List<ProcessingStats> stats, 
			List<ProcessingStats> uncombinedStrategiesStats) {
		
		new StatsPresentationDialog(shell, stats, uncombinedStrategiesStats).open();
	}
	
	
	private void shwoInformationAboutGeneralError(Shell shell) {
		MessageDialog
      	.openError(
    		  shell, 
    		  "Error",
    		  "There was error during test cases generation.");
	}
	
	

	private boolean askIfOverwriteTestCases(Shell shell, File outputDirectory) {
		return MessageDialog
	      	.openQuestion(
	    		  shell, 
	    		  "Overwrite",
	    		  "Test cases exists already. Overwrite it?");
	}
	
	
	/**
	 * Determine if it is supported file (*.bpmn2)
	 * 
	 * @param file
	 * @return
	 */
	private boolean isSupportedFile(Object fileObject) {
		if (fileObject instanceof IAdaptable)
			return castToIFile(fileObject).getFileExtension().equals(TestCasePersister.FILE_EXTENSION);
			
		return false;
	}
	
	private IFile castToIFile(Object object) {
		IFile file = (IFile) ((IAdaptable) object).getAdapter(IFile.class);
		
		return file;
	}
	
	private File castToFile(IFile file) {
		String path = file.getLocation().toString();
		path = path.substring(0, path.lastIndexOf("."));
		
		return new File(path);
	}

	
	private TestCasePersister falsePersister() {
		try {
			return new TestCasePersister(null) {
				@Override
				public void persist(String namespace, BpmnModel model,
						boolean preserveDuplications) throws IOException {
				}
				
			};
			
		} catch (IOException e) {
			return null; // will never happen
		}
	}
}
