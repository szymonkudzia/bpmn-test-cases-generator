package com.edu.uj.sk.btcg.generation;


import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.Dialog;
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
		

		asSingleOne = createCheckBox(container, "Combine strategies as single one");
		
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
		naiveEdgeCoverage = createCheckBox(sc, "Naive edge coverage");
		coverageByUniquePaths = createCheckBox(sc, "Coverage by unique paths");
		
		Composite c = createHorizontalPanel(sc);
	    
	    new Label(c, SWT.LEFT);
	    
		Label l = new Label(c, SWT.LEFT);
		l.setText("k: ");

		Text text = new Text(c, SWT.BORDER);
		text.setEnabled(false);
		text.setText("1");
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		coverageByUniquePaths.addSelectionListener(selectHandler(e -> {
			if (coverageByUniquePaths.getSelection()) {
				text.setEnabled(true);
			} else {
				text.setEnabled(false);
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
