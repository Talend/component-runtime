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
package org.talend.sdk.component.starter.server.service.facet.wadl;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.talend.sdk.component.starter.server.service.domain.Build;
import org.talend.sdk.component.starter.server.service.domain.Dependency;
import org.talend.sdk.component.starter.server.service.domain.ProjectRequest;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.info.ServerInfo;

@ApplicationScoped
public class WADLFacet implements FacetGenerator {

    private String specification;

    public void register(@Observes final GeneratorRegistration init) {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream("generator/facet/wadl/specification.xml")))) {
            specification = reader.lines().collect(joining("\n"));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        init.registerFacetType(this);
    }

    @Override
    public String readme() {
        return "Generates the needed classes to call HTTP endpoints defined by a WADL located at `src/main/resources/wadl/client.xml`.\n"
                + "The `wadl2java` CXF plugin generates interfaces representing the endpoints and you can create a client using CXF `JAXRSClientFactoryBean` "
                + "class:\n\n" + "[source,java]\n" + "----\n"
                + "import org.talend.sdk.component.api.service.Service;\n\n"
                + "import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;\n\n" + "@Service\n"
                + "public class MyService {\n" + "  public MyGeneratedEndpointApi newClient(final String url) {\n"
                + "      return JAXRSClientFactoryBean.create(url, MyGeneratedEndpointApi.class);\n" + "  }\n" + "}\n"
                + "----\n\n"
                + "Don't forget you need to build once the project to have the generated interfaces and be able to write your client from the "
                + "using the sources created in `generated-sources` folder.\n\n"
                + "TIP: you can customize the way the client is created from the interface - including the providers/serializers used,\n"
                + "more details available at http://cxf.apache.org/docs/jax-rs-client-api.html.";
    }

    @Override
    public Stream<InMemoryFile> create(final String packageBase, final Build build, final Collection<String> facets,
            final Collection<ProjectRequest.SourceConfiguration> sources,
            final Collection<ProjectRequest.ProcessorConfiguration> processors, final ServerInfo.Snapshot versions) {
        return Stream.of(new InMemoryFile(build.getMainResourcesDirectory() + "/wadl/client.xml", specification));
    }

    @Override
    public Stream<Dependency> dependencies(final Collection<String> facets, final ServerInfo.Snapshot versions) {
        return Stream.of(new Dependency("org.apache.cxf", "cxf-rt-rs-client", versions.getCxf(), "compile"));
    }

    @Override
    public String description() {
        return "Generates a HTTP client from a WADL.";
    }

    @Override
    public String name() {
        return Constants.NAME;
    }

    @Override
    public Category category() {
        return Category.LIBRARIES;
    }

    public interface Constants {

        String NAME = "WADL Client Generation";
    }
}
