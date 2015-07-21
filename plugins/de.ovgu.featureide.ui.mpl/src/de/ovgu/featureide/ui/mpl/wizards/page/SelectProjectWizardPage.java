/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.ui.mpl.wizards.page;

import static de.ovgu.featureide.fm.core.localization.StringTable.HERE_YOU_SELECT_THE_PROJECT_YOU_WANT_TO_IMPORT_FROM_;
import static de.ovgu.featureide.fm.core.localization.StringTable.SELECT_A_PROJECT_FROM_THE_LIST_;
import static de.ovgu.featureide.fm.core.localization.StringTable.SELECT_PROJECT;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.mpl.builder.MSPLNature;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizardPage;
import de.ovgu.featureide.fm.ui.wizards.WizardConstants;

/**
 * A wizard page to select the project the user wants to import from. Shows all
 * project in workbench.
 * 
 * @author Christoph Giesel
 * @author Sebastian Krieter
 */
public class SelectProjectWizardPage extends AbstractWizardPage implements SelectionListener {

	private Composite container;
	private Tree projectTree;
	
	private IFeatureProject selectedProject = null;

	public SelectProjectWizardPage() {
		super(SELECT_PROJECT);
		setTitle(SELECT_PROJECT);
		setDescription(HERE_YOU_SELECT_THE_PROJECT_YOU_WANT_TO_IMPORT_FROM_);
	}

	@Override
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NONE);

		FillLayout layout = new FillLayout();
		container.setLayout(layout);
		setControl(container);
		
		projectTree = new Tree(container, SWT.NORMAL);
		projectTree.addSelectionListener(this);

		for (IFeatureProject project : CorePlugin.getFeatureProjects()) {
			try {
				IProject projectHandle = project.getProject();
				if (projectHandle != null && projectHandle.isAccessible() && !projectHandle.isNatureEnabled(MSPLNature.NATURE_ID)) {
					TreeItem item = new TreeItem(projectTree, SWT.NORMAL);
					item.setImage(GUIDefaults.FEATURE_SYMBOL);
					item.setText(project.getProjectName());
					item.setData(project);
				}
			} catch (CoreException e) {
				CorePlugin.getDefault().logError(e);
			}
		}
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		final TreeItem[] items = projectTree.getSelection();
		selectedProject = (items.length == 0) ? null : (IFeatureProject) items[0].getData();
		updatePage();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		updatePage();
	}
	
	@Override
	protected void putData() {
		abstractWizard.putData(WizardConstants.KEY_OUT_PROJECT, selectedProject);
	}
	
	@Override
	protected String checkPage() {
		if (selectedProject == null) {
			return SELECT_A_PROJECT_FROM_THE_LIST_;
		}
		return null;
	}

}
