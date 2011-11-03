/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2011  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.core.builder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;


/**
 * A general builder used to build every <code>FeatureProject</code>. Using an
 * extension point the real composition algorithm is given, that builds the
 * compiled files.
 * 
 * @author Tom Brosch
 * @author Thomas Thuem
 */
public class ExtensibleFeatureProjectBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = CorePlugin.PLUGIN_ID
			+ ".extensibleFeatureProjectBuilder";
	public static final String COMPOSER_KEY = "composer";
	
	private IFeatureProject featureProject;
	private IComposerExtension composerExtension;

	private boolean featureProjectLoaded() {
		if (featureProject != null && composerExtension != null)
			return true;

		if (getProject() == null) {
			CorePlugin.getDefault().logWarning("no project got");
			return false;
		}
		featureProject = CorePlugin.getFeatureProject(getProject());
		if (featureProject == null) {
		//	CorePlugin.getDefault().logWarning("Unable to get feature project");
			return false;
		}

		if ((composerExtension = featureProject.getComposer()) == null) {
			CorePlugin.getDefault().logWarning("No composition tool found");
			featureProject.createBuilderMarker(featureProject.getProject(),
					"Could not load the assigned composition engine: "
							+ featureProject.getComposerID(), 0,
					IMarker.SEVERITY_WARNING);
			return false;
		}

		composerExtension.loadComposerExtension();
		return true;
	}
	
	private boolean cleanBuild = false;
	
	private boolean cleaned = false;
	
	protected void clean(IProgressMonitor monitor) throws CoreException {
		if (!featureProjectLoaded())
			return;
		
		featureProject.deleteBuilderMarkers(featureProject.getSourceFolder(),
				IResource.DEPTH_INFINITE);
		composerExtension.initialize(featureProject);
		if (!composerExtension.clean()) {
			cleaned = false;
			
			featureProject.getProject().refreshLocal(IResource.DEPTH_INFINITE,
					monitor);
			return;
		}
		boolean hasOtherNature = true;
		if (featureProject.getProject().getDescription().getNatureIds().length == 1
				&& featureProject.getProject().hasNature(FeatureProjectNature.NATURE_ID)) {
			hasOtherNature = false;
		}

		featureProject.getBuildFolder().refreshLocal(IResource.DEPTH_INFINITE,
				monitor);
		if (!hasOtherNature) {
			if (featureProject.getBinFolder() != null && 
					featureProject.getBinFolder().exists()) {
				featureProject.getBinFolder().refreshLocal(IResource.DEPTH_INFINITE,
						monitor);
			}
		}
		
		if (cleanBuild) {
			IFile configFile = featureProject.getCurrentConfiguration();
			if (configFile == null) {
				return;
			}
		}else{
			cleaned = true;
		}
		if (!hasOtherNature) {
			for (IResource member : featureProject.getBinFolder().members())
				member.delete(true, monitor);
		}
		for (IResource member : featureProject.getBuildFolder().members()) {
			member.delete(true, monitor);
		}
		
		featureProject.getBuildFolder().refreshLocal(IResource.DEPTH_INFINITE,
				monitor);
		if (!hasOtherNature) {
			featureProject.getBinFolder().refreshLocal(IResource.DEPTH_INFINITE,
				monitor);
		}
		cleanBuild = false;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) {
		if (!featureProjectLoaded())
			return null;

		if (!featureProject.buildRelavantChanges() && !cleaned && kind == AUTO_BUILD)
			return null;

		cleaned = false;
		IFile config = featureProject.getCurrentConfiguration();
		featureProject.deleteBuilderMarkers(getProject(),
				IResource.DEPTH_INFINITE);
		
		try {
			for (IResource res : featureProject.getConfigFolder().members())
				res.refreshLocal(IResource.DEPTH_ZERO, null);
			featureProject.getProject().refreshLocal(IResource.DEPTH_ONE, null);
			cleanBuild = true;
			clean(monitor);
		} catch (CoreException e) {
			CorePlugin.getDefault().logError(e);
		}

		if (config == null) {
			return null;
		}
		if(featureProject.getFeatureModel()==null||featureProject.getFeatureModel().getRoot()==null){
			return null;
		}
		composerExtension.performFullBuild(config);
		
		featureProject.builded();
		try {
			featureProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
			CorePlugin.getDefault().fireBuildUpdated(featureProject);
		} catch (CoreException e) {
			CorePlugin.getDefault().logError(e);
		}
		try {
			if (!composerExtension.copyNotComposedFiles()) {
				copy(config);
			}
		} catch (CoreException e1) {
			CorePlugin.getDefault().logError(e1);
		}
		try {
			featureProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
		} catch (CoreException e) {
			CorePlugin.getDefault().logError(e);
		}
		return null;
		
	}
	
	/**
	 *  copies all not composed Files of selected Features from src to build
	 * @throws CoreException
	 */
	private void copy(IFile config) throws CoreException {
		ArrayList<String > selectedFeatures = getSelectedFeatures(config);
		if (selectedFeatures != null)
			for (String feature : selectedFeatures) {
				IFolder folder = featureProject.getSourceFolder().getFolder(feature);
				copy(folder, featureProject.getBuildFolder());
			}
	}
	
	private void copy(IFolder featureFolder, IFolder buildFolder) throws CoreException {
		if (!featureFolder.exists()) {
			return;
		}
		
		for (IResource res : featureFolder.members()) {
			if (res instanceof IFolder) {
				IFolder folder = buildFolder.getFolder(res.getName());
				if (!folder.exists()) {
					folder.create(false, true, null);
				}
				copy((IFolder)res, folder);
			} else if (res instanceof IFile) {
				if (!composerExtension.extensions().contains("." + res.getName().split("[.]")[1])) {
					IFile file = buildFolder.getFile(res.getName());
					if (!file.exists()) {
						res.copy(file.getFullPath(), true, null);
					}
				}
			}
		}
	}

	private static ArrayList<String> getSelectedFeatures(IFile config) {
		File configFile = config.getRawLocation().toFile();
		return getTokenListFromFile(configFile);
	}

	/**
	 * returns a List of the tokens in file+
	 * this method is public for testing purposes 
	 * 
	 * @param file
	 * @return List of tokens
	 */
	public static ArrayList<String> getTokenListFromFile(File file) {
		ArrayList<String> list = null;
		Scanner scanner = null;

		try {
			scanner = new Scanner(file);

			if (scanner.hasNext()) {
				list = new ArrayList<String>();
				while (scanner.hasNext()) {
					list.add(scanner.next());
				}

			}

		} catch (FileNotFoundException e) {
			CorePlugin.getDefault().logError(e);
		} finally {
			if(scanner!=null)scanner.close();
		}
		return list;
	}

}
