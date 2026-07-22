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
package org.talend.sdk.component.server.front;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.HealthStatus;
import org.talend.sdk.component.server.service.FatalState;

@MonoMeecrowaveConfig
class LivenessResourceImplIT {

    @Inject
    private WebTarget base;

    @Inject
    private FatalState fatalState;

    @Test
    void livenessReturns200WhenNoFatalError() {
        final Response response = base.path("liveness").request(APPLICATION_JSON_TYPE).get();
        assertEquals(200, response.getStatus());
        final HealthStatus status = response.readEntity(HealthStatus.class);
        assertNotNull(status);
        assertEquals("UP", status.getStatus());
        assertNull(status.getCause());
    }

    @Test
    void readinessReturns200WhenReady() {
        final Response response = base.path("readiness").request(APPLICATION_JSON_TYPE).get();
        assertEquals(200, response.getStatus());
        final HealthStatus status = response.readEntity(HealthStatus.class);
        assertNotNull(status);
        assertEquals("UP", status.getStatus());
    }

    @Test
    void livenessAndReadinessAreIndependentFromEnvironment() {
        final Response envResponse = base.path("environment").request(APPLICATION_JSON_TYPE).get();
        assertEquals(200, envResponse.getStatus());

        final Response livenessResponse = base.path("liveness").request(APPLICATION_JSON_TYPE).get();
        assertEquals(200, livenessResponse.getStatus());

        final Response readinessResponse = base.path("readiness").request(APPLICATION_JSON_TYPE).get();
        assertEquals(200, readinessResponse.getStatus());
    }

    @Test
    void livenessReturns503WhenFatalErrorRecorded() {
        fatalState.markFatal("simulated OOM during test");
        try {
            final Response response = base.path("liveness").request(APPLICATION_JSON_TYPE).get();
            assertEquals(503, response.getStatus());
            final HealthStatus status = response.readEntity(HealthStatus.class);
            assertNotNull(status);
            assertEquals("DOWN", status.getStatus());
            assertNotNull(status.getCause());
        } finally {
            // reset the singleton state so other tests are not affected
            fatalState.reset();
        }
    }
}
