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
package org.talend.sdk.component.server.front.beam;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.Dependencies;
import org.talend.sdk.component.server.front.model.DependencyDefinition;
import org.talend.sdk.component.server.test.ComponentClient;

@MonoMeecrowaveConfig
class BeamComponentResourceImplTest {

    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    @Test
    void getDependencies() {
        final String compId = client.getBeamSampleId();
        final Dependencies dependencies = base
                .path("component/dependencies")
                .queryParam("identifier", compId)
                .request(APPLICATION_JSON_TYPE)
                .get(Dependencies.class);
        assertEquals(1, dependencies.getDependencies().size());
        final DependencyDefinition definition = dependencies.getDependencies().get(compId);
        assertNotNull(definition);
        assertEquals(1, definition.getDependencies().size());
        assertEquals(
                "org.talend.sdk.component:component-runtime-beam:jar:"
                        + System.getProperty("components.sample.beam.version"),
                definition.getDependencies().iterator().next());
    }

    @Test
    void getDependenciesNonBeam() {
        final String compId = client.getJdbcId();
        final Dependencies dependencies = base
                .path("component/dependencies")
                .queryParam("identifier", compId)
                .request(APPLICATION_JSON_TYPE)
                .get(Dependencies.class);
        assertEquals(1, dependencies.getDependencies().size());
        final DependencyDefinition definition = dependencies.getDependencies().get(compId);
        assertNotNull(definition);
        assertEquals(1, definition.getDependencies().size());
        assertEquals("org.apache.tomee:ziplock:jar:8.0.14", definition.getDependencies().iterator().next());
    }

}
