/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
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
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
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
                .map(dep -> resolveArtifact(dep, dep.getClassifier(), "jar"))
                .map(art -> {
                    try (final JarFile file = new JarFile(art.getFile())) {
                        return ofNullable(file.getEntry("TALEND-INF/dependencies.txt")).map(entry -> {
                            try (final InputStream ignored = file.getInputStream(entry)) {
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

    /**
     * Resolves an artifact.
     * This method:
     * <ul>
     * <li>searches for the artifact in the local maven repository</li>
     * <li>if found, returns it (can return an old snapshot if not run with --update-snapshots)</li>
     * <li>if not found, searches online with
     * {@link ComponentDependenciesBase#resolveArtifactOnRemoteRepositories(Artifact)}</li>
     * </ul>
     * The provided classifier &amp; type override the artifact's.
     *
     * @param artifact The artifact to resolve
     * @param classifier The classifier of the artifact to resolve
     * @param type The type of the artifact to resolve
     */
    protected Artifact resolveArtifact(final Artifact artifact, final String classifier, final String type) {
        final LocalRepositoryManager lrm = repositorySystemSession.getLocalRepositoryManager();
        final Artifact overriddenArtifact = new DefaultArtifact(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                classifier,
                type,
                getVersion(artifact));
        final File location = new File(
                lrm.getRepository().getBasedir(),
                lrm.getPathForLocalArtifact(overriddenArtifact));

        if (!location.exists()) {
            return resolveArtifactOnRemoteRepositories(overriddenArtifact);
        }
        return overriddenArtifact.setFile(location);
    }

    protected String computeCoordinates(final Artifact artifact) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + getVersion(artifact);
    }

    private String getVersion(final Artifact dep) {
        return ofNullable(dep.getBaseVersion()).orElseGet(dep::getVersion);
    }

    /**
     * Resolves an artifact in remote repositories.
     * This method:
     * <ul>
     * <li>finds the repository in which the artifact is stored</li>
     * <li>requests that repository to find the artifact</li>
     * <li>downloads it in the local maven repository</li>
     * <li>throws if any step above can't be done</li>
     * </ul>
     *
     * @param artifact The artifact to resolve
     */
    protected Artifact resolveArtifactOnRemoteRepositories(final Artifact artifact) {
        final String gav = computeCoordinates(artifact);
        final String repositoryIds = remoteRepositories.stream()
                .map(RemoteRepository::getId)
                .collect(joining(", "));
        getLog().debug(format("Resolving %s in [%s]", gav, repositoryIds));

        final ArtifactRepository artifactRepository = resolveArtifactRepository(artifact);
        final String artifactRepositoryId = artifactRepository.getId();

        try {
            final ArtifactRequest artifactRequest = new ArtifactRequest()
                    .setArtifact(artifact)
                    .setRepositories(remoteRepositories.stream()
                            .filter(remoteRepository -> remoteRepository.getId()
                                    .equals(artifactRepositoryId))
                            .collect(toList()));
            final ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
            if (result.isMissing()) {
                throw new IllegalStateException("Can't find " + artifact);
            }
            return result.getArtifact();

        } catch (final ArtifactResolutionException e) {
            final String message = format("Could not find artifact %s in repository %s",
                    gav, artifactRepositoryId);
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * Resolves an artifact descriptor to find in which remote repository it is stored.
     */
    private ArtifactRepository resolveArtifactRepository(final Artifact artifact) {
        final String gav = computeCoordinates(artifact);
        try {
            final ArtifactDescriptorRequest artifactDescriptorRequest =
                    new ArtifactDescriptorRequest(artifact, remoteRepositories, null);
            final ArtifactDescriptorResult descriptorResult =
                    repositorySystem.readArtifactDescriptor(repositorySystemSession, artifactDescriptorRequest);
            final ArtifactRepository repository = descriptorResult.getRepository();
            getLog().debug(format("Artifact %s comes from repository %s", gav, repository.getId()));
            return repository;

        } catch (final ArtifactDescriptorException e) {
            final String repositoryIds = remoteRepositories.stream()
                    .map(RemoteRepository::getId)
                    .collect(joining(", "));

            final String message = format(
                    "Cannot find the remote repository where artifact %s is hosted. Tried: [%s]",
                    gav, repositoryIds);
            throw new IllegalStateException(message, e);
        }
    }
}
