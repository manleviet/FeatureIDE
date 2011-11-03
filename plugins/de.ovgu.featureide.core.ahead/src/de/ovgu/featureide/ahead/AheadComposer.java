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
package de.ovgu.featureide.ahead;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import de.ovgu.featureide.ahead.wrapper.AheadBuildErrorEvent;
import de.ovgu.featureide.ahead.wrapper.AheadBuildErrorListener;
import de.ovgu.featureide.ahead.wrapper.AheadWrapper;
import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.builder.ComposerExtensionClass;
import de.ovgu.featureide.fm.core.configuration.Configuration;


/**
 * Composes source jak files into merged jak files.
 * 
 * @author Tom Brosch
 */
public class AheadComposer extends ComposerExtensionClass {

	public static final String COMPOSER_ID = "de.ovgu.featureide.composer.ahead";

	public static final String OLD_BUILD_COMMAND = "FeatureIDE_Core.jakBuilder";

	private static final String LAYER_REPLACING = "LAYER_REPLACING";

	private AheadWrapper ahead;

	private class BuilderErrorListener implements AheadBuildErrorListener {
		public void parseErrorFound(AheadBuildErrorEvent event) {
			if (featureProject != null)
				featureProject.createBuilderMarker(event.getResource(),
						event.getMessage(), event.getLine(),
						IMarker.SEVERITY_ERROR);
		}
	}

	public boolean initialize(IFeatureProject project) {
		super.initialize(project);
		if (project == null) {
			return false;
		}
		ahead = new AheadWrapper(project);
		ahead.addBuildErrorListener(new BuilderErrorListener());
		
		try {
			ahead.setConfiguration(featureProject.getCurrentConfiguration());
			return true;
		} catch (IOException e) {
			featureProject.createBuilderMarker(featureProject.getProject(),
					e.getMessage(), 0, IMarker.SEVERITY_ERROR);
			return false;
		}
		
	}

	public void performFullBuild(IFile config) {
		assert (ahead != null) : "Ahead instance not initialized";
		try {
			correctSourceFiles(featureProject.getSourceFolder());
			ahead.setConfiguration(config);
			ahead.buildAll();
		} catch (Exception e) {
			AheadCorePlugin.getDefault().logError(e);
		}
	}
	
	/**
	 * Iterated through all jak files of the source folder.<br><br>
	 * 
	 * The first line of a jak file must not start with imports.<br>
	 * Removes derived layer declarations.
	 * 
	 * @param folder
	 * @throws CoreException
	 */
	private void correctSourceFiles(IFolder folder) throws CoreException {
		for (IResource res : folder.members()) {
			if (res instanceof IFolder) {
				correctSourceFiles((IFolder)res);
			} else if(res instanceof IFile){
				if (res.getName().endsWith(".jak")) {
					correctSourceFile((IFile)res);
				}
			}
		}
	}
	
	/**
	 * Corrects the given source jak file.<br><br>
	 * 
	 * The first line of a jak file must not start with imports.<br> 
	 * Removes derived layer declarations.
	 * 
	 * @param file
	 */
	private void correctSourceFile(IFile file) {
		String text = getFileText(file);
		if (text != null) {
			text = correctFileText(text);
			if (text != null) {
				setFileText(file, text);
			}
		}
	}

	/**
	 * Corrects the given file content of the source jak file.<br><br>
	 * 
	 * The first line of a jak file must not start with imports.<br>
	 * Removes derived layer declarations.
	 * 
	 * @param fileContent The file content.
	 * @return
	 */
	public static String correctFileText(String fileContent) {
		boolean changed = false;
		if (fileContent.startsWith("import ")) {
			changed = true;
			fileContent = "\r\n" + fileContent;
		}
		if (!fileContent.equals(fileContent.replaceFirst("layer\\s+\\w+\\s*;", ""))
				&& fileContent.replaceFirst("layer\\s+\\w+\\s*;", LAYER_REPLACING).indexOf(LAYER_REPLACING) < fileContent.indexOf("{")) {
			return fileContent.replaceFirst("layer\\s+\\w+\\s*;", "");
		} else if (changed) {
			return fileContent;
		}
		return null;
	}

	/**
	 * Returns the content of a file.
	 * @param file
	 * @return the file content
	 */
	private String getFileText(IFile file) {
		Scanner scanner = null;
		try {
			StringBuffer fileText = new StringBuffer();
			scanner = new Scanner(file.getRawLocation().toFile());
			while (scanner.hasNext()) {
				fileText.append(scanner.nextLine());
				fileText.append("\r\n");
			}
			return fileText.toString();
		} catch (FileNotFoundException e) {
			AheadCorePlugin.getDefault().logError(e);
		}  finally {
			if(scanner!=null) {
				scanner.close();
			}
		}
		return null;
	}

	/**
	 * Sets the content of a file.
	 * @param file
	 * @param content
	 */
	private void setFileText(IFile file, String content) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(file.getRawLocation().toFile());
			fw.write(content);
			try {
				file.refreshLocal(IResource.DEPTH_ZERO, null);
			} catch (CoreException e) {
				AheadCorePlugin.getDefault().logError(e);
			}
		} catch (IOException e) {
			AheadCorePlugin.getDefault().logError(e);
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					AheadCorePlugin.getDefault().logError(e);
				}
			}
		}
	}

	@Override
	public ArrayList<String> extensions() {
		ArrayList<String> extensions = new ArrayList<String>();
		extensions.add(".jak");
		return extensions;
	}

	/**
	 * Renames all java-files into jak-files and replaces "package" by "layer"
	 */
	@Override
	public boolean postAddNature(IFolder source, IFolder destination) {
		try {
			for (IResource res : source.members()) {
				if (res instanceof IFolder) {
					performRenamings(source);
				} else {
					if (res instanceof IFile) {
						IFile file = (IFile) res;
						if (file.getName().endsWith(".java")) {
							res.move(source.getFile(file.getName()
									.replaceFirst(".java", ".jak"))
									.getFullPath(), true, null);
						}
					}
				}
			}

		} catch (CoreException e) {
			CorePlugin.getDefault().logError(e);
		}
		return false;
	}

	private void performRenamings(IFolder folder) throws CoreException {
		for (IResource res : folder.members()) {
			if (res instanceof IFolder) {
				performRenamings((IFolder) res);
			} else if (res instanceof IFile) {
				IFile file = (IFile) res;
				if (file.getName().endsWith(".java")) {
					performRenamings(file);
					res.move(folder.getFile(file.getName()
							.replaceFirst(".java", ".jak"))
							.getFullPath(), true, null);
				}
			}

		}
	}

	private void performRenamings(IFile iFile) {
		try {
			File file = iFile.getRawLocation().toFile();
			StringBuffer fileTextBuffer = new StringBuffer();
			Scanner scanner = new Scanner(file);
			while (scanner.hasNext()) {
				fileTextBuffer.append(scanner.nextLine() + "\r\n");
			}
			scanner.close();

			String fileText = fileTextBuffer.toString().replaceFirst("package", "layer");
			FileWriter fw = new FileWriter(file);
			fw.write(fileText);
			fw.close();
		} catch (FileNotFoundException e) {
			AheadCorePlugin.getDefault().logError(e);
		} catch (IOException e) {
			AheadCorePlugin.getDefault().logError(e);
		}
	}

	@Override
	public void buildFSTModel() {
		performFullBuild(null);
	}

	@Override
	public ArrayList<String[]> getTemplates() {
		ArrayList<String[]> list = new ArrayList<String[]>();
		String[] jak = { "Jak", "jak",
				"public #refines# class #classname# {\n\n}" };
		list.add(jak);
		return list;
	}
	
	@Override
	public String replaceMarker(String text, List<String> list) {
		if (list != null && list.contains("refines"))
			text = text.replace("#refines#", "refines ");
		else
			text = text.replace("#refines#", "");
		
		return text;
	}

	@Override
	public void postCompile(IResourceDelta delta, IFile file) {
		super.postCompile(delta, file);
		if (ahead != null && file.getName().endsWith(".java")) {
			ahead.postCompile(file);
		}
	}

	@Override
	public void addCompiler(IProject project, String sourcePath,
			String configPath, String buildPath) {
		super.addCompiler(project, sourcePath, configPath, buildPath);
		addSettings(project);
		removeOldBuildCommand(project);
	}
	
	/**
	 * Removes the old build command from project setup.
	 * "FeatureIDE_Core.jakBuilder"
	 * @param project 
	 */
	private void removeOldBuildCommand(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			LinkedList<ICommand> newCommandList = new LinkedList<ICommand>();
			for (ICommand command : description.getBuildSpec()) {
				if (command.getBuilderName().equals(COMPOSER_ID)) {
					newCommandList.addFirst(command);
				}
				if (!command.getBuilderName().equals(OLD_BUILD_COMMAND)) {
					newCommandList.add(command);
				}
			}
			ICommand[] newCommandArray = new ICommand[newCommandList.size()];
			int i = 0;
			for (ICommand c : newCommandList) {
				newCommandArray[i] = c;
				i++;
			}
			description.setBuildSpec(newCommandArray);
			project.setDescription(description, null);
		} catch (CoreException ex) {
		}
	}

	private void addSettings(IProject project) {
		IFolder settingsFolder = project.getFolder(".settings");
		if (!settingsFolder.exists()) {
			try {
				settingsFolder.create(true, true, null);
			} catch (CoreException e) {
				AheadCorePlugin.getDefault().logError(e);
			}
		}
		IFile settingsFile = settingsFolder.getFile("org.eclipse.jdt.core.prefs");
		if (!settingsFile.exists()) {
			String text = 
				"eclipse.preferences.version=1\r\n" +
				"org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled\r\n" +
				"org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.6\r\n" +
				"org.eclipse.jdt.core.compiler.codegen.unusedLocal=preserve\r\n" +
				"org.eclipse.jdt.core.compiler.compliance=1.6\r\n" +
				"org.eclipse.jdt.core.compiler.debug.lineNumber=generate\r\n" +
				"org.eclipse.jdt.core.compiler.debug.localVariable=generate\r\n" +
				"org.eclipse.jdt.core.compiler.debug.sourceFile=generate\r\n" +
				"org.eclipse.jdt.core.compiler.problem.assertIdentifier=error\r\n" +
				"org.eclipse.jdt.core.compiler.problem.enumIdentifier=error\r\n" +
				"org.eclipse.jdt.core.compiler.source=1.6\r\n" +
				"org.eclipse.jdt.core.builder.resourceCopyExclusionFilter=*.jak";
			InputStream source = new ByteArrayInputStream(text.getBytes());
			try {
				settingsFile.create(source, true, null);
			} catch (CoreException e) {
				AheadCorePlugin.getDefault().logError(e);
			}
		}		
	}
	
	/* (non-Javadoc)
	 * @see de.ovgu.featureide.core.builder.ComposerExtensionClass#buildConfiguration(org.eclipse.core.resources.IFolder, de.ovgu.featureide.fm.core.configuration.Configuration)
	 */
	@Override
	public void buildConfiguration(IFolder folder, Configuration configuration) {
		super.buildConfiguration(folder, configuration);
		ahead.setCompositionFolder(folder);
		performFullBuild(folder.getFile(folder.getName() + getConfigurationExtension()));
		ahead.setCompositionFolder(featureProject.getBuildFolder());
	}
}
