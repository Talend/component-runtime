/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.build;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.talend.sdk.component.starter.server.service.Resources;
import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.Dependency;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.facet.wadl.WADLFacet;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;
import org.talend.sdk.component.starter.server.service.template.TemplateRenderer;

import lombok.Data;

@ApplicationScoped
public class MavenBuildGenerator implements BuildGenerator {

    @Inject
    private TemplateRenderer renderer;

    private Map<String, Collection<Plugin>> plugins;

    public void register(@Observes final GeneratorRegistration init) {
        init.registerBuildType("Maven", this);
        plugins = new HashMap<>();
        plugins.put("jar", emptyList());
    }

    @Override
    public Build createBuild(final ProjectRequest.BuildConfiguration buildConfiguration, final String packageBase,
            final Collection<Dependency> dependencies, final Collection<String> facets,
            final ServerInfo.Snapshot versions) {
        return new Build(buildConfiguration.getArtifact(), buildConfiguration.getGroup(),
                buildConfiguration.getVersion(), "src/main/java", "src/test/java", "src/main/resources",
                "src/test/resources", "src/main/webapp", "pom.xml",
                renderer
                        .render("generator/maven/pom.xml",
                                new Pom(buildConfiguration, dependencies,
                                        createPlugins(facets, packageBase,
                                                plugins.get(buildConfiguration.getPackaging()), versions),
                                        versions.getKit())),
                "target", generateWrapperFiles());
    }

    private Collection<Plugin> createPlugins(final Collection<String> facets, final String packageBase,
            final Collection<Plugin> plugins, final ServerInfo.Snapshot versions) {
        final Collection<Plugin> buildPlugins = new ArrayList<>(plugins);

        buildPlugins
                .add(new Plugin("org.apache.maven.plugins", "maven-surefire-plugin", versions.getSurefire(), emptySet(),
                        new LinkedHashMap<String, String>() {

                            {
                                put("trimStackTrace", "false");
                                put("runOrder", "alphabetical");
                            }
                        }.entrySet()));

        if (facets.contains(WADLFacet.Constants.NAME)) {
            buildPlugins
                    .add(new Plugin("org.apache.cxf", "cxf-wadl2java-plugin", versions.getCxf(),
                            singleton(new Execution("generate-http-client-from-wadl", "generate-sources", "wadl2java")),
                            new LinkedHashMap<String, String>() {

                                {
                                    put("wadlOptions", "\n            <wadlOption>\n"
                                            + "              <wadl>${project.basedir}/src/main/resources/wadl/client.xml</wadl>\n"
                                            + "              <packagename>" + packageBase
                                            + ".client.wadl</packagename>\n" + "            </wadlOption>\n          ");
                                }
                            }.entrySet()));
        }

        return buildPlugins;
    }

    private List<FacetGenerator.InMemoryFile> generateWrapperFiles() {
        final FacetGenerator.InMemoryFile mvnw =
                new FacetGenerator.InMemoryFile("mvnw", Resources.resourceFileToString("generator/maven/mvnw"));
        final FacetGenerator.InMemoryFile mvnwCmd =
                new FacetGenerator.InMemoryFile("mvnw.cmd", Resources.resourceFileToString("generator/maven/mvnw.cmd"));
        final FacetGenerator.InMemoryFile mvnwWrapperProps =
                new FacetGenerator.InMemoryFile(".mvn/wrapper/maven-wrapper.properties",
                        Resources.resourceFileToString("generator/maven/maven-wrapper.properties"));
        final FacetGenerator.InMemoryFile mvnwWrapperJar = new FacetGenerator.InMemoryFile(
                ".mvn/wrapper/maven-wrapper.jar", Resources.resourceFileToBytes("generator/maven/maven-wrapper.jar"));

        return asList(mvnw, mvnwCmd, mvnwWrapperProps, mvnwWrapperJar);
    }

    @Data
    public static class Pom {

        private final ProjectRequest.BuildConfiguration build;

        private final Collection<Dependency> dependencies;

        private final Collection<Plugin> plugins;

        private final String pluginVersion;
    }

    @Data
    public static class Plugin {

        private final String groupId;

        private final String artifactId;

        private final String version;

        private final Collection<Execution> executions;

        private final Collection<Map.Entry<String, String>> configuration;
    }

    @Data
    public static class Execution {

        private final String id;

        private final String phase;

        private final String goal;
    }
}
