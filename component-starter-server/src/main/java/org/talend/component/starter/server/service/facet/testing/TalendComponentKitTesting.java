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
package org.talend.component.starter.server.service.facet.testing;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.talend.component.starter.server.service.domain.Build;
import org.talend.component.starter.server.service.domain.Dependency;
import org.talend.component.starter.server.service.domain.ProjectRequest;
import org.talend.component.starter.server.service.event.GeneratorRegistration;
import org.talend.component.starter.server.service.facet.FacetGenerator;
import org.talend.component.starter.server.service.facet.Versions;
import org.talend.component.starter.server.service.template.TemplateRenderer;

@ApplicationScoped
public class TalendComponentKitTesting implements FacetGenerator, Versions {
    @Inject
    private TemplateRenderer tpl;

    private Collection<Dependency> dependencies;

    void register(@Observes final GeneratorRegistration init) {
        init.registerFacetType(this);
        dependencies = asList(
                Dependency.junit(),
                new Dependency("org.talend.components", "component-runtime-junit", KIT, "test"));
    }

    @Override
    public Stream<Dependency> dependencies(final Collection<String> facets) {
        return dependencies.stream();
    }

    @Override
    public Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
                                       final Collection<ProjectRequest.SourceConfiguration> sources,
                                       final Collection<ProjectRequest.ProcessorConfiguration> processors) {
        final Collection<InMemoryFile> files = new ArrayList<>();

        /* TODO: generate tests for each source/processor
        final String testBase = build.getTestJavaDirectory() + '/' + packageBase.replace('.', '/');
        final Map<String, String> model = new HashMap<String, String>() {{
            put("package", packageBase);
        }};

        if (facets.contains(jaxrs.name())) {
            files.add(new InMemoryFile(
                    testBase + "/jaxrs/ArquillianHelloResourceTest.java",
                    tpl.render("factory/jaxrs/ArquillianHelloResourceTest.java", model)));
        }
        if (facets.contains(openjpa.name())) {
            files.add(new InMemoryFile(
                    testBase + "/jpa/ArquillianHelloEntityTest.java",
                    tpl.render("factory/openjpa/ArquillianHelloEntityTest.java", model)));
        }
        if (facets.contains(deltaspikeConfiguration.name())) {
            files.add(new InMemoryFile(
                    testBase + "/deltaspike/ArquillianConfigurationTest.java",
                    tpl.render("factory/deltaspike/ArquillianConfigurationTest.java", model)));
        }
        files.add(new InMemoryFile(
                build.getTestResourcesDirectory() + "/arquillian.xml",
                tpl.render("factory/arquillian/arquillian.xml", new HashMap<String, String>() {{
                    put("buildDir", build.getBuildDir());
                }})));
        */
        return files.stream();
    }

    @Override
    public String name() {
        return "Talend Component Kit Testing";
    }

    @Override
    public Category category() {
        return Category.TEST;
    }

    @Override
    public String readme() {
        return "Talend Component Kit Testing skeleton generator. For each component selected it generates an associated test suffixed with `Test`.";
    }

    @Override
    public String description() {
        return "Generates test(s) for each component.";
    }
}
