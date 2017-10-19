// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.starter.server.service.build;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.talend.components.starter.server.service.domain.Build;
import org.talend.components.starter.server.service.domain.Dependency;
import org.talend.components.starter.server.service.domain.ProjectRequest;
import org.talend.components.starter.server.service.event.GeneratorRegistration;
import org.talend.components.starter.server.service.facet.Versions;
import org.talend.components.starter.server.service.template.TemplateRenderer;

import lombok.Data;

@ApplicationScoped
public class MavenBuildGenerator implements BuildGenerator, Versions {
    @Inject
    private TemplateRenderer renderer;

    private Map<String, Collection<Plugin>> plugins;

    void register(@Observes final GeneratorRegistration init) {
        init.registerBuildType("Maven", this);
        plugins = new HashMap<>();
        plugins.put("jar", emptyList());
    }

    @Override
    public Build createBuild(final ProjectRequest.BuildConfiguration buildConfiguration,
                             final String packageBase,
                             final Collection<Dependency> dependencies,
                             final Collection<String> facets) {
        return new Build(
                buildConfiguration.getArtifact(),
                "src/main/java", "src/test/java",
                "src/main/resources", "src/test/resources",
                "src/main/webapp", "pom.xml",
                renderer.render("generator/maven/pom.xml", new Pom(buildConfiguration, dependencies,
                                createPlugins(plugins.get(buildConfiguration.getPackaging())))),
                "target");
    }

    private Collection<Plugin> createPlugins(final Collection<Plugin> plugins) {
        final Collection<Plugin> buildPlugins = new ArrayList<>(plugins);

        buildPlugins.add(new Plugin(
                "org.apache.maven.plugins", "maven-surefire-plugin", SUREFIRE, emptySet(), new LinkedHashMap<String, String>() {{
            put("trimStackTrace", "false");
            put("runOrder", "alphabetical");
        }}.entrySet()));

        return buildPlugins;
    }

    @Data
    public static class Pom {
        private final ProjectRequest.BuildConfiguration build;
        private final Collection<Dependency> dependencies;
        private final Collection<Plugin> plugins;
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
