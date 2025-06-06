/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package com.github.gtoison.caster.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.fileset.FileSet;
import org.apache.maven.shared.model.fileset.util.FileSetManager;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;

import com.github.gtoison.caster.transformer.HibernateProxyCasterClassTransformer;
import com.github.gtoison.caster.transformer.HibernateProxyJandexIndexer;

/**
 * @author Guillaume Toison
 */
@Mojo(name = "transform", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class HibernateProxyCasterTransformerMojo extends AbstractMojo {

	// info messages
	private static final String SUCCESFULLY_TRANSFORMED_CLASS_FILE = "Succesfully transformed class file: %s";
	private static final String SKIPPING_FILE = "Skipping file: %s";
	private static final String ADDED_FILE_TO_SOURCE_SET = "Added file to source set: %s";

	// error messages
	private static final String ERROR_WRITING_BYTES_TO_FILE = "Error writing bytes to file : %s";
	private static final String ERROR_OPENING_FILE_FOR_WRITING = "Error opening file for writing : %s";
	private static final String ERROR_WHILE_ENHANCING_CLASS_FILE = "An exception occurred while trying to class file: %s";
	private static final String UNEXPECTED_ERROR_WHILE_CONSTRUCTING_CLASSLOADER = "An unexpected error occurred while constructing the classloader";

	// debug messages
	private static final String AMOUNT_BYTES_WRITTEN_TO_FILE = "%s bytes were succesfully written to file: %s";
	private static final String WRITING_BYTE_CODE_TO_FILE = "Writing byte code to file: %s";
	private static final String DETERMINE_CLASS_NAME_FOR_FILE = "Determining class name for file: %s";
	private static final String TRYING_TO_ENHANCE_CLASS_FILE = "Trying to enhance class file: %s";
	private static final String STARTING_CLASS_TRANSFORM = "Starting class transform";
	private static final String SETTING_LASTMODIFIED_FAILED_FOR_CLASS_FILE = "Setting lastModified failed for class file: %s";
	private static final String ENDING_CLASS_TRANSFORM = "Ending class transform";
	private static final String CREATE_URL_CLASSLOADER_FOR_FOLDER = "Creating URL ClassLoader for folder: %s";
	private static final String PROCESSING_FILE_SET = "Processing FileSet";
	private static final String USING_BASE_DIRECTORY = "Using base directory: %s";
	private static final String SKIPPING_NON_CLASS_FILE = "Skipping non '.class' file: %s";
	private static final String FILESET_PROCESSED_SUCCESFULLY = "FileSet was processed succesfully";
	private static final String STARTING_ASSEMBLY_OF_SOURCESET = "Starting assembly of the source set";
	private static final String ENDING_ASSEMBLY_OF_SOURCESET = "Ending the assembly of the source set";
	private static final String ADDED_DEFAULT_FILESET_WITH_BASE_DIRECTORY = "Addded a default FileSet with base directory: %s";
	private static final String STARTING_EXECUTION_OF_ENHANCE_MOJO = "Starting execution of enhance mojo";
	private static final String ENDING_EXECUTION_OF_ENHANCE_MOJO = "Ending execution of enhance mojo";

	private final List<Path> sourceSet = new ArrayList<>();
	private HibernateProxyCasterClassTransformer enhancer;

	@Parameter
	private FileSet[] fileSets;

	@Parameter(defaultValue = "${project.build.directory}/classes", readonly = true, required = true)
	private File classesDirectory;

	@Parameter(defaultValue = "${project.build.directory}/test-classes", readonly = true, required = true)
	private File testClassesDirectory;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().debug(STARTING_EXECUTION_OF_ENHANCE_MOJO);
		
		processParameters();
		assembleSourceSet();

		try (URLClassLoader classLoader = createClassLoader()) {
			try {
				classLoader.loadClass("org.hibernate.Hibernate");
			} catch (ClassNotFoundException e) {
				throw new MojoFailureException("org.hibernate.Hibernate is not on the project compile classpath", e);
			}
			
			performEnhancement(classLoader);
		} catch (IOException e) {
			throw new MojoFailureException("Error while closing classloader", e);
		}

		getLog().debug(ENDING_EXECUTION_OF_ENHANCE_MOJO);
	}

	private void processParameters() {
		if (fileSets == null) {
			List<FileSet> defaultFileSets = new ArrayList<>();

			addFileSet(defaultFileSets, classesDirectory);
			addFileSet(defaultFileSets, testClassesDirectory);

			fileSets = defaultFileSets.toArray(FileSet[]::new);
		}
	}

	private void addFileSet(List<FileSet> defaultFileSets, File file) {
		if (file.exists()) {
			FileSet fileSet = new FileSet();
			fileSet.setDirectory(file.getAbsolutePath());
			getLog().debug(ADDED_DEFAULT_FILESET_WITH_BASE_DIRECTORY.formatted(fileSet.getDirectory()));

			defaultFileSets.add(fileSet);
		}
	}

	private void assembleSourceSet() {
		getLog().debug(STARTING_ASSEMBLY_OF_SOURCESET);
		for (FileSet fileSet : fileSets) {
			addFileSetToSourceSet(fileSet);
		}
		getLog().debug(ENDING_ASSEMBLY_OF_SOURCESET);
	}

	private void addFileSetToSourceSet(FileSet fileSet) {
		getLog().debug(PROCESSING_FILE_SET);
		String directory = fileSet.getDirectory();
		FileSetManager fileSetManager = new FileSetManager();
		Path baseDir = null;
		if (classesDirectory != null) {
			baseDir = classesDirectory.toPath();
		}
		if (directory != null && classesDirectory != null) {
			baseDir = Path.of(directory);
		}
		getLog().debug(USING_BASE_DIRECTORY.formatted(baseDir));
		for (String fileName : fileSetManager.getIncludedFiles(fileSet)) {
			Path candidateFile = baseDir.resolve(fileName);
			if (fileName.endsWith(".class")) {
				sourceSet.add(candidateFile);
				getLog().info(ADDED_FILE_TO_SOURCE_SET.formatted(candidateFile));
			} else {
				getLog().debug(SKIPPING_NON_CLASS_FILE.formatted(candidateFile));
			}
		}
		getLog().debug(FILESET_PROCESSED_SUCCESFULLY);
	}

	private URLClassLoader createClassLoader() throws MojoFailureException {
		getLog().debug(CREATE_URL_CLASSLOADER_FOR_FOLDER.formatted(classesDirectory));
		Set<URL> urls = new HashSet<>();
		try {
			urls.add(classesDirectory.toURI().toURL());
		} catch (MalformedURLException e) {
			getLog().error(UNEXPECTED_ERROR_WHILE_CONSTRUCTING_CLASSLOADER, e);
		}

		try {
			for (String element : project.getCompileClasspathElements()) {
				urls.add(Path.of(element).toUri().toURL());
			}
		} catch (DependencyResolutionRequiredException | MalformedURLException e) {
			throw new MojoFailureException("Error resolving project runtime classpath", e);
		}

		return new URLClassLoader(urls.toArray(new URL[0]),	HibernateProxyCasterClassTransformer.class.getClassLoader());
	}

	private String determineClassName(Path classFile) {
		getLog().debug(DETERMINE_CLASS_NAME_FOR_FILE.formatted(classFile));
		String classFilePath = classFile.toAbsolutePath().toString();
		String classesDirectoryPath = classesDirectory.getAbsolutePath();
		return classFilePath.substring(classesDirectoryPath.length() + 1, classFilePath.length() - ".class".length())
				.replace(File.separatorChar, '.');
	}

	private void performEnhancement(ClassLoader classLoader) {
		getLog().debug(STARTING_CLASS_TRANSFORM);
		try (InputStream input = Files.newInputStream(classesDirectory.toPath().resolve("META-INF", "jandex.idx"))) {
			IndexReader reader = new IndexReader(input);
			Index index = reader.read();
			HibernateProxyJandexIndexer indexer = new HibernateProxyJandexIndexer(index);

			enhancer = new HibernateProxyCasterClassTransformer(indexer);
			for (Path classFile : sourceSet) {
				FileTime lastModified = Files.getLastModifiedTime(classFile);
				enhanceClass(classFile, classLoader);

				try {
					Files.setLastModifiedTime(classFile, lastModified);
				} catch (IOException e) {
					getLog().debug(SETTING_LASTMODIFIED_FAILED_FOR_CLASS_FILE.formatted(classFile), e);
				}
			}
		} catch (IOException e) {
			getLog().error("Error loading index", e);
		}
		getLog().debug(ENDING_CLASS_TRANSFORM);
	}

	private void enhanceClass(Path classFile, ClassLoader classLoader) {
		getLog().debug(TRYING_TO_ENHANCE_CLASS_FILE.formatted(classFile));
		try {
			byte[] newBytes = enhancer.transform(classLoader, determineClassName(classFile),
					Files.readAllBytes(classFile));
			if (newBytes != null) {
				writeByteCodeToFile(newBytes, classFile);
				getLog().info(SUCCESFULLY_TRANSFORMED_CLASS_FILE.formatted(classFile));
			} else {
				getLog().info(SKIPPING_FILE.formatted(classFile));
			}
		} catch (IllegalClassFormatException | IOException e) {
			getLog().error(ERROR_WHILE_ENHANCING_CLASS_FILE.formatted(classFile), e);
		}
	}

	private void writeByteCodeToFile(byte[] bytes, Path file) {
		getLog().debug(WRITING_BYTE_CODE_TO_FILE.formatted(file));

		try {
			Files.write(file, bytes, StandardOpenOption.TRUNCATE_EXISTING);
			getLog().debug(AMOUNT_BYTES_WRITTEN_TO_FILE.formatted(bytes.length, file));
		} catch (FileNotFoundException e) {
			getLog().error(ERROR_OPENING_FILE_FOR_WRITING.formatted(file), e);
		} catch (IOException e) {
			getLog().error(ERROR_WRITING_BYTES_TO_FILE.formatted(file), e);
		}
	}
}
