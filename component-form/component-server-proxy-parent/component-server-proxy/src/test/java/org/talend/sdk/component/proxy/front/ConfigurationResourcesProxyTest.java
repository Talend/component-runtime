/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.front;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;

import javax.ws.rs.client.WebTarget;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.model.ConfigType;
import org.talend.sdk.component.proxy.model.Configurations;
import org.talend.sdk.component.proxy.test.WithServer;

@WithServer
class ConfigurationResourcesProxyTest {

    @Test
    void listRootConfigs(final WebTarget proxyClient) {
        final Configurations roots =
                proxyClient.path("configuration/roots").request(APPLICATION_JSON_TYPE).get(Configurations.class);
        assertNotNull(roots);
        assertEquals(3, roots.getConfigurations().size());
        roots.getConfigurations().forEach((k, c) -> assertNotNull(c.getIcon()));
        assertEquals(asList("Connection-1", "Connection-2", "Connection-3"),
                roots.getConfigurations().values().stream().map(ConfigType::getLabel).sorted().collect(toList()));
    }

    @Test
    void getEmptyConfigDetails(final WebTarget proxyClient) {
        final Configurations configurations = proxyClient
                .path("configuration/details")
                .queryParam("identifiers", emptyList())
                .request(APPLICATION_JSON_TYPE)
                .get(Configurations.class);
        assertNotNull(configurations);
        assertTrue(configurations.getConfigurations().isEmpty());
    }

    @Test
    void getConfigDetails(final WebTarget proxyClient) {
        final ConfigType config = proxyClient
                .path("configuration/roots")
                .request(APPLICATION_JSON_TYPE)
                .get(Configurations.class)
                .getConfigurations()
                .values()
                .stream()
                .sorted(Comparator.comparing(ConfigType::getLabel))
                .iterator()
                .next();
        final Configurations configurations = proxyClient
                .path("configuration/details")
                .queryParam("identifiers", config.getChildren().stream().toArray())
                .request(APPLICATION_JSON_TYPE)
                .get(Configurations.class);
        assertNotNull(configurations);
        configurations.getConfigurations().forEach((k, v) -> assertTrue(v.getChildren().isEmpty()));
    }

    @Ignore("not ye supported by the server, invalid ids are ignored")
    void getConfigDetailsInvalidId(final WebTarget proxyClient) {
        final Configurations configurations = proxyClient
                .path("configuration/details")
                .queryParam("identifiers", singletonList("0invalidxyz"))
                .request(APPLICATION_JSON_TYPE)
                .get(Configurations.class);
        assertNotNull(configurations);
        configurations.getConfigurations().forEach((k, v) -> assertTrue(v.getChildren().isEmpty()));
    }

}
