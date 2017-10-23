/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.starter.server.service.build;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
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
public class GradleBuildGenerator implements BuildGenerator, Versions {
    @Inject
    private TemplateRenderer tpl;

    void register(@Observes final GeneratorRegistration init) {
        init.registerBuildType("Gradle", this);
    }

    @Override
    public Build createBuild(final ProjectRequest.BuildConfiguration buildConfiguration,
                             final String packageBase,
                             final Collection<Dependency> dependencies,
                             final Collection<String> facets) {
        final GradleBuild model = new GradleBuild(
                buildConfiguration,
                dependencies.stream()
                        .map(d -> "test".equals(d.getScope()) ? new Dependency(d, "testCompile") : d) // just renaming
                        .map(d -> "runtime".equals(d.getScope()) ? new Dependency(d, "compile") : d) // otherwise not there for tests
                        .collect(toList()),
                emptySet(), emptySet(),
                singleton("java"),
                emptySet());
        return new Build(
                buildConfiguration.getArtifact(),
                "src/main/java", "src/test/java",
                "src/main/resources", "src/test/resources",
                "src/main/webapp", "build.gradle",
                tpl.render("generator/gradle/build.gradle", model),
                "build");
    }

    @Data
    public static class GradleBuild {
        private final ProjectRequest.BuildConfiguration build;
        private final Collection<Dependency> dependencies;
        private final Collection<String> buildDependencies;
        private final Collection<String> configurations;
        private final Collection<String> plugins;
        private final Collection<Map.Entry<String, Collection<Map.Entry<String, String>>>> pluginConfigurations;
    }
}
