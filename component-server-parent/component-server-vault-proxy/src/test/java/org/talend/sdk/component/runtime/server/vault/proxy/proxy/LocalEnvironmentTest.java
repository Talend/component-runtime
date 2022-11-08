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
package org.talend.sdk.component.runtime.server.vault.proxy.proxy;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http.Type.TALEND_COMPONENT_KIT;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.meecrowave.testing.ConfigurationInject;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.server.vault.proxy.endpoint.proxy.LocalEnvironmentResource;
import org.talend.sdk.component.runtime.server.vault.proxy.service.http.Http;

@MonoMeecrowaveConfig
class LocalEnvironmentTest {

    @ConfigurationInject
    private Meecrowave.Builder serverConfig;

    @Inject
    @Http(TALEND_COMPONENT_KIT) // not important in tests
    private Client client;

    @Test
    void environment() {
        final LocalEnvironmentResource.Environment environment = client
                .target("http://localhost:" + serverConfig.getHttpPort())
                .path("api/v1/proxy/environment")
                .request(APPLICATION_JSON_TYPE)
                .get(LocalEnvironmentResource.Environment.class);
        assertEquals(1, environment.getLatestApiVersion());
        assertEquals(1, environment.getProxiedApiVersion());
        assertNotNull(environment.getCommit());
        assertNotNull(environment.getTime());
        assertNotNull(environment.getVersion());
    }
}
