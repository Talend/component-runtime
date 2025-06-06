/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service.jcache;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.CacheClear;
import org.talend.sdk.component.server.service.ComponentManagerService;
import org.talend.sdk.component.server.test.ComponentClient;

@MonoMeecrowaveConfig
class FrontCacheResolverTest {

    @Inject
    private ComponentClient client;

    @Inject
    private WebTarget base;

    @Inject
    private FrontCacheResolver cacheResolver;

    @Inject
    private ComponentManagerService service;

    @BeforeEach
    void beforeEach() {
        // Clean caches
        cacheResolver.cleanupCaches();
    }

    @Test
    void cleanupCaches() {
        assertEquals(0, cacheResolver.countActiveCaches());
        client.fetchIndex();
        assertEquals(1, cacheResolver.countActiveCaches());
        client.fetchConfigTypeNodes();
        assertEquals(2, cacheResolver.countActiveCaches());
    }

    @Test
    void clearCaches() throws JsonProcessingException {
        final int connectors = service.getConnectors().getPluginsList().size();

        // Initialize 2 caches
        client.fetchIndex();
        client.fetchConfigTypeNodes();

        // Clear caches
        final Response resp = base
                .path("cache/clear")
                .request(APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, resp.getStatus());
        final CacheClear cleared = resp.readEntity(CacheClear.class);

        assertEquals(2, cleared.getClearedCacheCount());
        assertEquals(0, cacheResolver.countActiveCaches());
        assertEquals(connectors, service.getConnectors().getPluginsList().size());
    }

}