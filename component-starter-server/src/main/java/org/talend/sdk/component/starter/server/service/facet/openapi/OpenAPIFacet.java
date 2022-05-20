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
package org.talend.sdk.component.starter.server.service.facet.openapi;

import java.util.Collection;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.talend.sdk.component.starter.server.service.domain.Dependency;
import org.talend.sdk.component.starter.server.service.event.GeneratorRegistration;
import org.talend.sdk.component.starter.server.service.facet.FacetGenerator;
import org.talend.sdk.component.starter.server.service.info.ServerInfo.Snapshot;

@ApplicationScoped
public class OpenAPIFacet implements FacetGenerator {

    public static final String NAME = "OpenAPIFacet";

    @Override
    public String description() {
        return "OpenAPI OAS20/OAS30 Facet";
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Category category() {
        return Category.LIBRARIES;
    }

    @Override
    public String loggingScope() {
        return "compile";
    }

    @Override
    public Stream<Dependency> dependencies(final Collection<String> facets, final Snapshot versions) {
        return Stream.of(
                new Dependency("org.apache.logging.log4j", "log4j-slf4j-impl", versions.getLog4j2(), "compile"),
                new Dependency("org.talend.components", "stream-json", versions.getStreamJson(), "compile"));
    }

    public void register(@Observes final GeneratorRegistration init) {
        init.registerFacetType(this);
    }
}
