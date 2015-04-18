package com.edu.uj.sk.btcg.generation;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.edu.uj.sk.btcg.generation.processors.IProcessor;
import com.edu.uj.sk.btcg.generation.processors.Processors;

public class GenerationStrategyPickerDialog extends Dialog {
	private Composite container;
	private List<IProcessor> chosenProcessors = new ArrayList<IProcessor>();
	
	private boolean asSingle = false;
	private Button asSingleOne;
	
	private boolean optimize = false;
	private Button optimizeRes;
	
	private Button naiveEdgeCoverage;
	private Button manualTask;
	private Button retrivingInfoFailed;
	private Button exceptionInScriptServiceTask;
	private Button hasRights;
	private Button coverageByUniquePaths;
	private Button coverageByInputManipulation;
	private Button corruptedOutgoingMessages;
	private Button corruptedIncomingMessages;
	private Button brokenIncomingFlowsToParallelGateway;
	private Button artifactGenerationFailed;
	
	
	private Text kInput;
	
	
	public List<IProcessor> getChosenProcessors() {
		return chosenProcessors;
	}

	public boolean asSingleStrategy() {
		return asSingle;
	}
	
	public boolean optimizeResult() {
		return optimize;
	}
	
	
	protected GenerationStrategyPickerDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		container = (Composite) super.createDialogArea(parent);
		

		asSingleOne = createCheckBox(container, "Combine strategies as single one");
		optimizeRes = createCheckBox(container, "Optimize result set");
		optimizeRes.setSelection(true);
		optimizeRes.setEnabled(false);
		
		asSingleOne.addSelectionListener(selectHandler(e -> {
			if (asSingleOne.getSelection()) {
				optimizeRes.setEnabled(true);
			} else {
				optimizeRes.setEnabled(false);
			}
		}));
		
		
		createHorizontalSeparator();
		
		Composite selectDeselectContainer = createHorizontalPanel(container);
		
		Button selectAll = new Button(selectDeselectContainer, SWT.PUSH);
		selectAll.setText("Select All");
		selectAll.addSelectionListener(selectAllHandler());
		
		Button deselectAll = new Button(selectDeselectContainer, SWT.PUSH);
		deselectAll.setText("Deselect All");
		deselectAll.addSelectionListener(deselectAllHandler());
		
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(container, SWT.V_SCROLL | SWT.BORDER);
		scrolledComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL));
		
		Composite sc = new Composite(scrolledComposite, SWT.NONE);
		sc.setLayout(new GridLayout());
		sc.setLayoutData(new GridData(GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL));
		
		scrolledComposite.setContent(sc);
		
		
		manualTask = createCheckBox(sc, "Manual task");
		retrivingInfoFailed = createCheckBox(sc, "Information retrival task");
		exceptionInScriptServiceTask = createCheckBox(sc, "Exceptions in automated tasks");
		hasRights = createCheckBox(sc, "Has rights");
		naiveEdgeCoverage = createCheckBox(sc, "Coverage by all paths");
		coverageByUniquePaths = createCheckBox(sc, "K-Edge coverage");
		
		Composite c = createHorizontalPanel(sc);
	    
	    new Label(c, SWT.LEFT);
	    
		Label l = new Label(c, SWT.LEFT);
		l.setText("k: ");

		kInput = new Text(c, SWT.BORDER);
		kInput.setEnabled(false);
		kInput.setText("1");
		kInput.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		coverageByUniquePaths.addSelectionListener(selectHandler(e -> {
			if (coverageByUniquePaths.getSelection()) {
				kInput.setEnabled(true);
			} else {
				kInput.setEnabled(false);
			}
		}));
		
		coverageByInputManipulation = createCheckBox(sc, "Coverage by input manipulation");
		corruptedOutgoingMessages = createCheckBox(sc, "Corrupted outgoing messages");
		corruptedIncomingMessages = createCheckBox(sc, "Corrupted incoming messages");
		brokenIncomingFlowsToParallelGateway = createCheckBox(sc, "Lock in parallel gateway");
		artifactGenerationFailed = createCheckBox(sc, "Incorrect artifacts generation");
		
		sc.setSize(sc.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.layout();
		scrolledComposite.layout();
		
		return container;
	}



	private Composite createHorizontalPanel(Composite container) {
		Composite c = new Composite(container, SWT.NONE);
	    GridLayout layout = new GridLayout();
	    layout.numColumns = 3;
	    layout.horizontalSpacing = 10;
	    c.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    c.setLayout(layout);
		return c;
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
	
	private Button createCheckBox(Composite container, String label) {
		Button button = new Button(container, SWT.CHECK);
		button.setText(label);
		
		return button;
	}

	@Override
	protected void okPressed() {
		int k = 1;
		
		if (kInput.isEnabled()) {
			try {
				k = Integer.parseInt(kInput.getText());
				if (k <= 0) throw new IllegalArgumentException();
				
			} catch (Throwable e) {
				MessageDialog.openError(getParentShell(), "Error", "K is not valid number! Only Integers greater than 0 are acceptable.");
				return;
			}
		}
		
		if (naiveEdgeCoverage.getSelection())
			chosenProcessors.add(Processors.newCoverageByAllPaths());
		
		
		if (manualTask.getSelection())
			chosenProcessors.add(Processors.newManualTask());
		
		
		if (retrivingInfoFailed.getSelection())
			chosenProcessors.add(Processors.newInformationRetrivalTask());
		
		
		if (exceptionInScriptServiceTask.getSelection())
			chosenProcessors.add(Processors.newExceptionInAutomatedTasks());
		
		
		if (hasRights.getSelection())
			chosenProcessors.add(Processors.newHasRights());
		
		if (coverageByUniquePaths.getSelection())
			chosenProcessors.add(Processors.newKEdgeCoverage(k));
		
		
		if (coverageByInputManipulation.getSelection())
			chosenProcessors.add(Processors.newCoverageByInputManipulation());
		
		
		if (corruptedOutgoingMessages.getSelection())
			chosenProcessors.add(Processors.newCorruptedOutgoingMessages());
		
		
		if (corruptedIncomingMessages.getSelection())
			chosenProcessors.add(Processors.newCorruptedIncomingMessage());
		
		
		if (brokenIncomingFlowsToParallelGateway.getSelection())
			chosenProcessors.add(Processors.newLockInParallelGateway());
		
		
		if (artifactGenerationFailed.getSelection())
			chosenProcessors.add(Processors.newIncorrectArtifactsGeneration());
		
		asSingle = asSingleOne.getSelection();
		
		if (asSingle)
			optimize = optimizeRes.getSelection();
		
		super.okPressed();
	}
	
	

	
	
	
	private void createHorizontalSeparator() {
		Label separator = new Label(container, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	
	
	
	private SelectionListener selectHandler(Consumer<SelectionEvent> consumer) {
		return new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				consumer.accept(arg0);
			}
			

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		};
	}
	
	
	private SelectionListener selectAllHandler() {
		return selectHandler(e -> selectAllStrategies(true));
	}
	
	
	private SelectionListener deselectAllHandler() {
		return selectHandler(e -> selectAllStrategies(false));
	}
	
	
	private void selectAllStrategies(boolean selected) {
		naiveEdgeCoverage.setSelection(selected);
		manualTask.setSelection(selected);
		retrivingInfoFailed.setSelection(selected);
		exceptionInScriptServiceTask.setSelection(selected);
		hasRights.setSelection(selected);
		coverageByUniquePaths.setSelection(selected);
		coverageByInputManipulation.setSelection(selected);
		corruptedOutgoingMessages.setSelection(selected);
		corruptedIncomingMessages.setSelection(selected);
		brokenIncomingFlowsToParallelGateway.setSelection(selected);
		artifactGenerationFailed.setSelection(selected);
		
		
		coverageByUniquePaths.notifyListeners(SWT.Selection, new Event());
	}
}
