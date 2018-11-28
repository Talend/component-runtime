/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.container.maven.shade;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.NONE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.IncludesArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.resource.ResourceTransformer;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.ArtifactDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.FilteringDependencyNodeVisitor;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;

import lombok.Data;
import lombok.Setter;

@Setter
public abstract class ArtifactTransformer implements ResourceTransformer {

    private MavenSession session;

    private String scope = Artifact.SCOPE_COMPILE_PLUS_RUNTIME + ",-" + Artifact.SCOPE_PROVIDED;

    private String include;

    private String exclude;

    private boolean includeTransitiveDependencies = true;

    private boolean includeProjectComponentDependencies = false;

    protected List<UserArtifact> userArtifacts;

    @Setter(NONE)
    protected List<Artifact> artifacts;

    @Override
    public boolean hasTransformedResource() {
        artifacts = new ArrayList<>();

        final ArtifactFilter filter = getFilter();

        if (userArtifacts != null && !userArtifacts.isEmpty()) {
            final ArtifactResolver resolver;
            final DependencyGraphBuilder graphBuilder;
            final ProjectBuilder projectBuilder;
            final PlexusContainer container = session.getContainer();
            try {
                resolver = ArtifactResolver.class.cast(container.lookup(ArtifactResolver.class, "default"));
                projectBuilder = ProjectBuilder.class.cast(container.lookup(ProjectBuilder.class, "default"));
                graphBuilder = includeTransitiveDependencies
                        ? DependencyGraphBuilder.class.cast(container.lookup(DependencyGraphBuilder.class, "default"))
                        : null;
            } catch (final ComponentLookupException e) {
                throw new IllegalArgumentException(e);
            }
            artifacts.addAll(userArtifacts.stream().flatMap(coords -> {
                try {
                    final String type = ofNullable(coords.type).filter(s -> !s.isEmpty()).orElse("jar");
                    final Artifact art = new DefaultArtifact(coords.groupId, coords.artifactId, coords.version,
                            coords.scope, type, ofNullable(coords.classifier).orElse(""), new DefaultArtifactHandler() {

                                {
                                    setExtension(type);
                                }
                            });
                    final Artifact artifact =
                            resolver.resolveArtifact(session.getProjectBuildingRequest(), art).getArtifact();
                    if (includeTransitiveDependencies) {
                        final MavenProject fakeProject;
                        try {
                            fakeProject = projectBuilder
                                    .build(resolver
                                            .resolveArtifact(session.getProjectBuildingRequest(),
                                                    new DefaultArtifact(art.getGroupId(), art.getArtifactId(),
                                                            art.getVersion(), art.getScope(), "pom", null,
                                                            new DefaultArtifactHandler() {

                                                                {
                                                                    setExtension("pom");
                                                                }
                                                            }))
                                            .getArtifact()
                                            .getFile(), session.getProjectBuildingRequest())
                                    .getProject();
                        } catch (final ProjectBuildingException e) {
                            throw new IllegalStateException(e);
                        }
                        fakeProject.setArtifact(artifact);
                        final DefaultProjectBuildingRequest request =
                                new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
                        request.setProject(fakeProject);

                        try {
                            final DependencyNode transitives = graphBuilder.buildDependencyGraph(request, filter);
                            final CollectingDependencyNodeVisitor visitor = new CollectingDependencyNodeVisitor();
                            transitives
                                    .accept(new FilteringDependencyNodeVisitor(visitor,
                                            new ArtifactDependencyNodeFilter(filter)));
                            return Stream
                                    .concat(Stream.of(artifact),
                                            visitor.getNodes().stream().map(DependencyNode::getArtifact).map(a -> {
                                                try {
                                                    return resolver.resolveArtifact(request, a).getArtifact();
                                                } catch (final ArtifactResolverException e) {
                                                    throw new IllegalStateException(e);
                                                }
                                            }));
                        } catch (final DependencyGraphBuilderException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    return Stream.of(artifact);
                } catch (final ArtifactResolverException e) {
                    throw new IllegalArgumentException(e);
                }
            }).collect(toSet()));
        }

        if (includeProjectComponentDependencies) { // this is a weird category of projects but can exist
            final MavenProject project = session.getCurrentProject();
            if (project.getArtifacts() != null && !project.getArtifacts().isEmpty()) {
                project.setArtifactFilter(filter);
                try {
                    artifacts.addAll(project.getArtifacts());
                } finally {
                    // shade plugin uses it OOTB so reset it for the end of the execution (in case
                    // another transformer needs it)

                    project.setArtifactFilter(new CumulativeScopeArtifactFilter(singletonList("runtime")));
                }
            }
        }

        return !artifacts.isEmpty();
    }

    private ArtifactFilter getFilter() {
        final List<ArtifactFilter> filters = new ArrayList<>(2);
        if (include != null) {
            filters.add(new IncludesArtifactFilter(Stream.of(include.split(",")).collect(toList())));
        }
        if (exclude != null) {
            filters.add(new ExcludesArtifactFilter(Stream.of(exclude.split(",")).collect(toList())));
        }
        if (scope != null) {
            filters
                    .addAll(Stream
                            .of(scope.split(","))
                            .map(singleScope -> singleScope.startsWith("-") ? new ArtifactFilter() {

                                private final ArtifactFilter delegate = newScopeFilter(singleScope.substring(1));

                                @Override
                                public boolean include(final Artifact artifact) {
                                    return !delegate.include(artifact);
                                }
                            } : newScopeFilter(singleScope))
                            .collect(toList()));
        }
        return new AndArtifactFilter(filters);
    }

    @Override
    public boolean canTransformResource(final String resource) {
        return false;
    }

    @Override
    public void processResource(final String resource, final InputStream inputStream, final List<Relocator> list) {
        // no-op
    }

    // provided is not well handled so add a particular case for it
    private ArtifactFilter newScopeFilter(final String singleScope) {
        return "provided".equals(singleScope) ? artifact -> Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
                : new ScopeArtifactFilter(singleScope);
    }

    protected boolean isComponent(final Artifact artifact) {
        final File file = artifact.getFile();
        if (file.isDirectory()) {
            return new File(file, "TALEND-INF/dependencies.txt").exists();
        }
        try (final JarFile jar = new JarFile(file)) {
            return jar.getEntry("TALEND-INF/dependencies.txt") != null;
        } catch (final IOException ioe) {
            return false;
        }
    }

    @Data
    public static final class UserArtifact {

        private String groupId;

        private String artifactId;

        private String version;

        private String scope;

        private String classifier;

        private String type;

        private String file;
    }
}
