/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

/**
 * Bundles the component as a component archive (.car).
 */
public abstract class DependencyAwareMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}")
    private List<RemoteRepository> remoteRepositories;

    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repositorySystemSession;

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = ",", property = "talend.bundle.exclude.artifacts")
    private String excludeArtifactsFilter;

    @Parameter(defaultValue = "-", property = "talend.bundle.include.artifacts")
    private String includeExtraArtifacts;

    protected org.eclipse.aether.artifact.Artifact resolve(final org.eclipse.aether.artifact.Artifact dep,
            final String classifier, final String type) {
        final LocalRepositoryManager lrm = repositorySystemSession.getLocalRepositoryManager();
        final org.eclipse.aether.artifact.Artifact artifact = new org.eclipse.aether.artifact.DefaultArtifact(
                dep.getGroupId(), dep.getArtifactId(), classifier, type, dep.getVersion());
        final File location = new File(lrm.getRepository().getBasedir(), lrm.getPathForLocalArtifact(artifact));
        if (!location.exists()) {
            return resolve(artifact);
        }
        return artifact.setFile(location);
    }

    protected org.eclipse.aether.artifact.Artifact resolve(final org.eclipse.aether.artifact.Artifact art) {
        final ArtifactRequest request = new ArtifactRequest().setArtifact(art).setRepositories(remoteRepositories);
        try {
            final ArtifactResult result = repositorySystem.resolveArtifact(repositorySystemSession, request);
            if (result.isMissing()) {
                throw new IllegalStateException("Can't find " + art);
            }
            return result.getArtifact();
        } catch (final ArtifactResolutionException e) {
            getLog().error(e.getMessage());
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected Stream<Artifact> extraArtifacts(final String pom) throws MojoFailureException {
        try {
            getLog().info("Reading extra dependencies from " + pom);
            final MavenXpp3Reader reader = new MavenXpp3Reader();
            final Model model = reader.read(new FileReader(pom));
            final MavenProject project = new MavenProject(model);
            final List<Dependency> dependencies = project.getDependencies();
            return dependencies
                    .stream()
                    .map(d -> resolve(
                            new org.eclipse.aether.artifact.DefaultArtifact(
                                    String.format("%s:%s:%s", d.getGroupId(), d.getArtifactId(), d.getVersion())),
                            d.getClassifier(), "jar"))
                    .map(dep -> {
                        final DefaultArtifact ae = new DefaultArtifact(dep.getGroupId(), dep.getArtifactId(),
                                dep.getVersion(), "runtime", "jar", ofNullable(dep.getClassifier()).orElse(""),
                                new DefaultArtifactHandler());
                        ae.setFile(dep.getFile());
                        getLog().info("Adding extra artifact " + ae + " to car file.");
                        return ae;
                    });
        } catch (Exception e) {
            getLog().error(e);
            throw new MojoFailureException(e.getMessage());
        }
    }

    protected Map<String, File> artifacts() throws MojoFailureException {
        final Predicate<String> excluded = (test -> Arrays
                .stream(excludeArtifactsFilter.split(","))
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(s -> {
                    if (test.startsWith(s)) {
                        getLog().info("Removing artifact " + test + " from car file due to filter " + s + ".");
                        return true;
                    }
                    return false;
                }));
        Stream<Artifact> extraArtifacts = Stream.empty();
        if (!"-".equals(includeExtraArtifacts)) {
            extraArtifacts = extraArtifacts(includeExtraArtifacts);
        }
        final Map<String, File> artifacts =
                Stream
                        .concat(project
                                .getArtifacts()
                                .stream()
                                .filter(a -> !"org.talend.sdk.component".equals(a.getGroupId())
                                        && ("compile".equals(a.getScope()) || "runtime".equals(a.getScope()))
                                        && !excluded
                                                .test(String
                                                        .format("%s:%s:%s", a.getGroupId(), a.getArtifactId(),
                                                                a.getVersion()))),
                                extraArtifacts)
                        .collect(toMap(
                                a -> String
                                        .format("%s:%s:%s%s:%s:%s", a.getGroupId(), a.getArtifactId(),
                                                ofNullable(a.getType()).orElse("jar"),
                                                a.getClassifier() == null || a.getClassifier().isEmpty() ? ""
                                                        : (":" + a.getClassifier()),
                                                getVersion(a), ofNullable(a.getScope()).orElse("compile")),
                                Artifact::getFile, (a1, a2) -> {
                                    getLog().info(a1 + " already exists " + a2);
                                    return a1;
                                }));

        final String mainGav = mainGav();
        artifacts
                .putIfAbsent(mainGav, new File(project.getBuild().getDirectory(), project.getBuild().getFinalName()
                        + "." + ("bundle".equals(project.getPackaging()) ? "jar" : project.getPackaging())));
        return artifacts;
    }

    private String getVersion(final Artifact a) {
        return ofNullable(a.getBaseVersion()).orElseGet(a::getVersion);
    }

    protected String mainGav() {
        return String.format("%s:%s:%s", project.getGroupId(), project.getArtifactId(), project.getVersion());
    }
}
