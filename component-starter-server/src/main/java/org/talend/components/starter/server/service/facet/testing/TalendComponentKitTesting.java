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
package org.talend.components.starter.server.service.facet.testing;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.talend.components.starter.server.service.domain.Build;
import org.talend.components.starter.server.service.domain.Dependency;
import org.talend.components.starter.server.service.domain.ProjectRequest;
import org.talend.components.starter.server.service.event.GeneratorRegistration;
import org.talend.components.starter.server.service.facet.FacetGenerator;
import org.talend.components.starter.server.service.facet.Versions;
import org.talend.components.starter.server.service.template.TemplateRenderer;

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
