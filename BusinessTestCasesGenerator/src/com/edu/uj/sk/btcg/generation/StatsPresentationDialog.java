package com.edu.uj.sk.btcg.generation;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.edu.uj.sk.btcg.generation.processors.Stats;
import com.google.common.collect.Lists;

public class StatsPresentationDialog extends Dialog {
	private Composite container;
	private List<Stats> stats;
	private List<Stats> uncombinedStrategiesStats;

	protected StatsPresentationDialog(Shell parentShell,
			List<Stats> stats,
			List<Stats> uncombinedStrategiesStats) {
		super(parentShell);

		this.stats = stats;
		this.uncombinedStrategiesStats = uncombinedStrategiesStats;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		container = (Composite) super.createDialogArea(parent);

		createTable(stats);
		
		if (!uncombinedStrategiesStats.isEmpty()) {
			new Label(container, SWT.NONE).setText("  "); // separator
			new Label(container, SWT.NONE).setText("Results if strategies would not be combined: ");
			
			createTable(uncombinedStrategiesStats);
		}

		return container;
	}

	
	private void createTable(List<Stats> stats) {
		Table table = new Table(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);

		new TableColumn(table, SWT.NULL).setText("Strategy name");
		new TableColumn(table, SWT.NULL).setText("Total TC");
		new TableColumn(table, SWT.NULL).setText("Unique TC");

		for (Stats stat : stats) {
			TableItem item = new TableItem(table, SWT.NULL);
			item.setText(0, stat.getName());
			item.setText(1, Integer.toString(stat.getTotal()));
			item.setText(2, Integer.toString(stat.getUnique()));
		}

		
		for (int loopIndex : Lists.newArrayList(0, 1, 2)) {
			TableColumn column = table.getColumn(loopIndex);
			
			column.pack();
			
			if (loopIndex == 0) 
				column.setWidth(350);
			else 
				column.setWidth(100);
		}

		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.layout();
		
	}
	

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Generation finished successfully!");
		
		
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 800);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		
		getButton(IDialogConstants.OK_ID).setVisible(false);
		getButton(IDialogConstants.CANCEL_ID).setText("OK"); // I don't care if this is cancel or ok button :)
	}
	
	
	
}
