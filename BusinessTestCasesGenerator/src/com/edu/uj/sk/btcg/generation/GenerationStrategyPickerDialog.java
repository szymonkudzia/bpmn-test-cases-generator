package com.edu.uj.sk.btcg.generation;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.edu.uj.sk.btcg.generation.processors.IProcessor;
import com.edu.uj.sk.btcg.generation.processors.Processors;

public class GenerationStrategyPickerDialog extends Dialog {
	private Composite container;
	private List<IProcessor> chosenProcessors = new ArrayList<IProcessor>();
	
	boolean asSingle = false;
	Button asSingleOne;
	
	Button naiveEdgeCoverage;
	Button manualTask;
	Button retrivingInfoFailed;
	Button exceptionInScriptServiceTask;
	Button hasRights;
	Button coverageByUniquePaths;
	Button coverageByInputManipulation;
	Button corruptedOutgoingMessages;
	Button corruptedIncomingMessages;
	Button brokenIncomingFlowsToParallelGateway;
	Button artifactGenerationFailed;
	
	
	public List<IProcessor> getChosenProcessors() {
		return chosenProcessors;
	}

	public boolean asSingleStrategy() {
		return asSingle;
	}
	
	
	protected GenerationStrategyPickerDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		container = (Composite) super.createDialogArea(parent);


		asSingleOne = createCheckBox("Combine strategies as single one");
		
		createHorizontalSeparator();
		
		manualTask = createCheckBox("Manual task");
		retrivingInfoFailed = createCheckBox("Information retrival task");
		exceptionInScriptServiceTask = createCheckBox("Exceptions in automated tasks");
		hasRights = createCheckBox("Has rights");
		naiveEdgeCoverage = createCheckBox("Naive edge coverage");
		coverageByUniquePaths = createCheckBox("Coverage by unique paths");
		
		Composite c = new Composite(container, SWT.NONE);
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 3;
	    layout.horizontalSpacing = 10;
	    c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    c.setLayout(layout);
	    
	    new Label(c, SWT.LEFT);
	    
		Label l = new Label(c, SWT.LEFT);
		l.setText("k: ");

		Text text = new Text(c, SWT.BORDER);
		text.setText("1");
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		
		coverageByInputManipulation = createCheckBox("Coverage by input manipulation");
		corruptedOutgoingMessages = createCheckBox("Corrupted outgoing messages");
		corruptedIncomingMessages = createCheckBox("Corrupted incoming messages");
		brokenIncomingFlowsToParallelGateway = createCheckBox("Lock in parallel gateway");
		artifactGenerationFailed = createCheckBox("Incorrect artifacts generation");
		
		return container;
	}



	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Pick generation strategies");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 700);
	}
	
	private Button createCheckBox(String label) {
		Button button = new Button(container, SWT.CHECK);
		button.setText(label);
		
		return button;
	}

	@Override
	protected void okPressed() {
		
		if (naiveEdgeCoverage.getSelection())
			chosenProcessors.add(Processors.newNaiveCoverage());
		
		
		if (manualTask.getSelection())
			chosenProcessors.add(Processors.newManualTask());
		
		
		if (retrivingInfoFailed.getSelection())
			chosenProcessors.add(Processors.newFailedInformationRetrival());
		
		
		if (exceptionInScriptServiceTask.getSelection())
			chosenProcessors.add(Processors.newExceptionInScriptServiceTask());
		
		
		if (hasRights.getSelection())
			chosenProcessors.add(Processors.newHasRights());
		
		if (coverageByUniquePaths.getSelection())
			chosenProcessors.add(Processors.newCoverageByUniquePaths());
		
		
		if (coverageByInputManipulation.getSelection())
			chosenProcessors.add(Processors.newCoverageByInputManipulation());
		
		
		if (corruptedOutgoingMessages.getSelection())
			chosenProcessors.add(Processors.newCorruptedOutgoingMessages());
		
		
		if (corruptedIncomingMessages.getSelection())
			chosenProcessors.add(Processors.newCorruptedIncomingMessage());
		
		
		if (brokenIncomingFlowsToParallelGateway.getSelection())
			chosenProcessors.add(Processors.newParallelGatewayLock());
		
		
		if (artifactGenerationFailed.getSelection())
			chosenProcessors.add(Processors.newArtifactGenerationFail());
		
		asSingle = asSingleOne.getSelection();
		
		super.okPressed();
	}
	
	

	
	
	
	private void createHorizontalSeparator() {
		Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
}
