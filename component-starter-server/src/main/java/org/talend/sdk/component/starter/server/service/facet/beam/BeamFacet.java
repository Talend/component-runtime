/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.starter.server.service.facet.beam;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.Dependency;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.facet.Versions;

@ApplicationScoped
public class BeamFacet implements FacetGenerator, Versions {

    private List<Dependency> dependencies;

    void register(@Observes final GeneratorRegistration init) {
        init.registerFacetType(this);
        dependencies = asList(Dependency.junit(),
                new Dependency("org.talend.sdk.component", "component-runtime-beam", KIT, "test"),
                new Dependency("org.hamcrest", "hamcrest-all", "1.3", "test"),
                new Dependency("org.apache.beam", "beam-runners-direct-java", BEAM, "test"));
    }

    @Override
    public Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
            final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors) {
        return Stream.empty(); // TODO: rmb
    }

    @Override
    public Stream<Dependency> dependencies(final Collection<String> facets) {
        return dependencies.stream();
    }

    @Override
    public String description() {
        return "Generates some tests using beam runtime instead of Talend Component Kit Testing framework.";
    }

    @Override
    public String readme() {
        return "Beam facet generates component tests using Apache Beam testing framework. It executes the tests using a real "
                + "beam pipeline to go through the runtime constraints of Apache Beam which can be more strict than Talend Component Kit ones.";
    }

    @Override
    public String name() {
        return "Apache Beam";
    }

    @Override
    public Category category() {
        return Category.RUNTIME;
    }
}
