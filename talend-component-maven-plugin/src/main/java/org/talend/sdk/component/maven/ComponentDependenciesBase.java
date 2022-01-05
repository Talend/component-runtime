/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.maven;

import static java.util.Optional.ofNullable;
import static org.talend.sdk.component.maven.api.Audience.Type.TALEND_INTERNAL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.talend.sdk.component.maven.api.Audience;

@Audience(TALEND_INTERNAL)
public abstract class ComponentDependenciesBase extends AudienceAwareMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(property = "talend-m2.scopes", defaultValue = "compile,runtime")
    private List<String> scopes;

    @Parameter(property = "talend-m2.packagings", defaultValue = "jar,bundle")
    private List<String> packagings;

    @Parameter(defaultValue = "false", property = "talend.skip")
    private boolean skip;

    @Parameter(defaultValue = "${repositorySystemSession}")
    protected RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> remoteRepositories;

    @Component
    private RepositorySystem repositorySystem;

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Execution skipped");
            return;
        }
        super.execute();
        doExecute();
    }

    protected <T> Stream<T> getArtifacts(final Function<Artifact, T> onArtifact) {
        return project
                .getDependencies()
                .stream()
                .filter(art -> scopes.contains(art.getScope()))
                .filter(dep -> packagings.contains(dep.getType()))
                .map(dep -> new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), dep.getClassifier(),
                        dep.getType(), dep.getVersion()))
                .map(dep -> resolve(dep, dep.getClassifier(), "jar"))
                .map(art -> {
                    try (final JarFile file = new JarFile(art.getFile())) {
                        return ofNullable(file.getEntry("TALEND-INF/dependencies.txt")).map(entry -> {
                            try (final InputStream stream = file.getInputStream(entry)) {
                                return onArtifact.apply(art);
                            } catch (final IOException e) {
                                throw new IllegalStateException(e);
                            }
                        }).orElse(null);
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
    }

    protected Artifact resolve(final Artifact dep, final String classifier, final String type) {
        final LocalRepositoryManager lrm = repositorySystemSession.getLocalRepositoryManager();
        final Artifact artifact =
                new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(), classifier, type, getVersion(dep));
        final File location = new File(lrm.getRepository().getBasedir(), lrm.getPathForLocalArtifact(artifact));
        if (!location.exists()) {
            return resolve(artifact);
        }
        return artifact.setFile(location);
    }

    private String getVersion(final Artifact dep) {
        return ofNullable(dep.getBaseVersion()).orElseGet(dep::getVersion);
    }

    protected Artifact resolve(final Artifact art) {
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
