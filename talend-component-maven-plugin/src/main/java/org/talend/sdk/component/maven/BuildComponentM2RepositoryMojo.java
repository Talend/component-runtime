/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.maven;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;
import static org.apache.maven.plugins.annotations.ResolutionScope.COMPILE_PLUS_RUNTIME;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.ziplock.Files;
import org.apache.ziplock.IO;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

// @Experimental, resolve each car for each component which is a dependency of th eproject
@Mojo(name = "prepare-repository", defaultPhase = PACKAGE, threadSafe = true,
        requiresDependencyResolution = COMPILE_PLUS_RUNTIME)
public class BuildComponentM2RepositoryMojo extends AbstractMojo {

    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "talend-m2.scopes", defaultValue = "compile,runtime")
    private List<String> scopes;

    @Parameter(property = "talend-m2.packagings", defaultValue = "jar,bundle")
    private List<String> packagings;

    @Parameter(property = "talend-m2.registryBase")
    private File componentRegistryBase;

    @Parameter(property = "talend-m2.root",
            defaultValue = "${maven.multiModuleProjectDirectory}/target/talend-component-kit/maven")
    private File m2Root;

    @Parameter(property = "talend-m2.clean", defaultValue = "true")
    private boolean cleanBeforeGeneration;

    @Parameter(defaultValue = "false", property = "talend.skip")
    private boolean skip;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> remoteRepositories;

    @Parameter(defaultValue = "component", property = "talend.car.classifier")
    private String classifier;

    @Component
    private RepositorySystem repositorySystem;

    @Override
    public void execute() {
        if (skip) {
            getLog().info("Execution skipped");
            return;
        }

        final LocalRepositoryManager lrm = repositorySystemSession.getLocalRepositoryManager();
        final Set<Artifact> componentArtifacts = project
                .getArtifacts()
                .stream()
                .filter(art -> scopes.contains(art.getScope()))
                .filter(dep -> packagings.contains(dep.getType()))
                .map(dep -> new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(),
                        dep.getType(), dep.getVersion()))
                .map(dep -> resolve(lrm, dep, dep.getClassifier(), "jar"))
                .filter(art -> {
                    try (final JarFile file = new JarFile(art.getFile())) { // filter components with this marker
                        return file.getEntry("TALEND-INF/dependencies.txt") != null;
                    } catch (final IOException e) {
                        return false;
                    }
                })
                .map(art -> resolve(lrm, art, classifier, "car"))
                .collect(toSet());

        if (cleanBeforeGeneration && m2Root.exists()) {
            Files.remove(m2Root);
        }
        m2Root.mkdirs();
        final List<String> coordinates = componentArtifacts.stream().map(car -> {
            String gav = null;
            try (final ZipInputStream read =
                    new ZipInputStream(new BufferedInputStream(new FileInputStream(car.getFile())))) {
                ZipEntry entry;
                while ((entry = read.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    final String path = entry.getName();
                    if ("TALEND-INF/metadata.properties".equals(path)) {
                        final Properties properties = new Properties();
                        properties.load(read);
                        gav = properties.getProperty("component_coordinates").replace("\\:", "");
                        continue;
                    }
                    if (!path.startsWith("MAVEN-INF/repository/")) {
                        continue;
                    }

                    final File file = new File(m2Root, path.substring("MAVEN-INF/repository/".length()));
                    Files.mkdir(file.getParentFile());
                    IO.copy(read, file);

                    final long lastModified = entry.getTime();
                    if (lastModified > 0) {
                        file.setLastModified(lastModified);
                    }
                }
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
            return gav;
        }).filter(Objects::nonNull).distinct().sorted().collect(toList());

        if (getLog().isDebugEnabled()) {
            coordinates.forEach(it -> getLog().debug("Including " + it));
        } else {
            getLog().info("Included " + String.join(", ", coordinates));
        }

        final Properties components = new Properties();
        if (componentRegistryBase != null && componentRegistryBase.exists()) {
            try (final InputStream source = new FileInputStream(componentRegistryBase)) {
                components.load(source);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        coordinates.stream().filter(it -> it.contains(":")).forEach(it -> components.put(it.split(":")[1], it.trim()));
        try (final Writer output = new FileWriter(new File(m2Root, "component-registry.properties"))) {
            components.store(output, "Generated by Talend Component Kit " + getClass().getSimpleName());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        getLog().info("Created component repository at " + m2Root);
    }

    private Artifact resolve(final LocalRepositoryManager lrm, final Artifact dep, final String classifier,
            final String type) {
        final Artifact artifact =
                new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), classifier, type, dep.getVersion());
        final File location = new File(lrm.getRepository().getBasedir(), lrm.getPathForLocalArtifact(artifact));
        if (!location.exists()) {
            return resolve(artifact);
        }
        return artifact.setFile(location);
    }

    private Artifact resolve(final Artifact art) {
        final ArtifactRequest artifactRequest =
                new ArtifactRequest().setArtifact(art).setRepositories(remoteRepositories);
        try {
            final ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
            if (result.isMissing()) {
                throw new IllegalStateException("Can't find " + art);
            }
            return result.getArtifact();
        } catch (final ArtifactResolutionException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
