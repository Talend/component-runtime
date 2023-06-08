/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.test.websocket.WebsocketClient;

@MonoMeecrowaveConfig
class ConfigurationTypeResourceImplTest {

    @Inject
    private WebsocketClient ws;

    @Inject
    private WebTarget base;

    @RepeatedTest(2)
    void webSocketGetIndex() {
        final ConfigTypeNodes index = ws.read(ConfigTypeNodes.class, "get", "/configurationtype/index", "");
        assertIndex(index);
        validateJdbcHierarchy(index);
    }

    @RepeatedTest(2)
    void ensureConsistencyBetweenPathsAndNames() {
        final ConfigTypeNodes index =
                ws.read(ConfigTypeNodes.class, "get", "/configurationtype/index?lightPayload=false", "");
        validateConsistencyBetweenNamesAndKeys(index.getNodes().get("amRiYy1jb21wb25lbnQjamRiYyNkYXRhc2V0I2pkYmM"));
    }

    @RepeatedTest(2)
    void webSocketDetail() {
        final ConfigTypeNodes index = ws
                .read(ConfigTypeNodes.class, "get",
                        "/configurationtype/details?identifiers=amRiYy1jb21wb25lbnQjamRiYyNkYXRhc3RvcmUjamRiYw", "");
        assertEquals(1, index.getNodes().size());
        final ConfigTypeNode jdbcConnection = index.getNodes().get("amRiYy1jb21wb25lbnQjamRiYyNkYXRhc3RvcmUjamRiYw");
        assertNotNull(jdbcConnection);
        assertEquals("[{\"description\":\"D1\",\"driver\":\"d1\"},{\"description\":\"D2\",\"driver\":\"d2\"}]",
                jdbcConnection
                        .getProperties()
                        .stream()
                        .filter(p -> "configuration.configurations".equals(p.getPath()))
                        .findFirst()
                        .get()
                        .getDefaultValue());

    }

    @Test
    void migrate() {
        final Map<String, String> config = ws
                .read(Map.class, "post", "/configurationtype/migrate/amRiYy1jb21wb25lbnQjamRiYyNkYXRhc2V0I2pkYmM/-2",
                        "{}");
        assertEquals("true", config.get("configuration.migrated"));
        assertEquals("1", config.get("configuration.size"));
    }

    @Test
    void migrateNotFound() {
        final ErrorPayload error =
                ws.read(ErrorPayload.class, "post", "/configurationtype/migrate/aMissingConfig/-2", "{}");
        assertNotNull(error);
        assertEquals(ErrorDictionary.CONFIGURATION_MISSING, error.getCode());
        assertEquals("Didn't find configuration aMissingConfig", error.getDescription());
    }

    @Test
    void migrateUnexpected() {
        final ErrorPayload error = ws
                .read(ErrorPayload.class, "post",
                        "/configurationtype/migrate/amRiYy1jb21wb25lbnQjamRiYyNkYXRhc2V0I2pkYmM/-3", "{}");
        assertNotNull(error);
        assertEquals(ErrorDictionary.UNEXPECTED, error.getCode());
        assertEquals("Migration execution failed with: Error thrown for testing!", error.getDescription());
    }

    @Test
    void migrateUnexpectedWithNPE() {
        final ErrorPayload error = ws
                .read(ErrorPayload.class, "post",
                        "/configurationtype/migrate/amRiYy1jb21wb25lbnQjamRiYyNkYXRhc2V0I2pkYmM/-4", "{}");
        assertNotNull(error);
        assertEquals(ErrorDictionary.UNEXPECTED, error.getCode());
        assertEquals("Migration execution failed with: unexpected null", error.getDescription());
    }

    @Test
    void migrateWithEncrypted() {
        final JsonBuilderFactory factory = JsonProvider.provider().createBuilderFactory(emptyMap());
        final JsonObject json = factory
                .createObjectBuilder()
                .add("configuration.url", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==")
                .add("configuration.username", "username0")
                .add("configuration.password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==")
                .build();
        final Map<String, String> config = base
                .path("/configurationtype/migrate/amRiYy1jb21wb25lbnQjamRiYyNkYXRhc2V0I2pkYmM/-2")
                .request(APPLICATION_JSON_TYPE)
                .post(Entity.entity(new HashMap<String, String>() {

                    {
                        put("configuration.url", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                        put("configuration.username", "username0");
                        put("configuration.password", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                        put("configuration.connection.password",
                                "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                    }
                }, APPLICATION_JSON_TYPE))
                .readEntity(Map.class);
        assertEquals("true", config.get("configuration.migrated"));
        assertEquals("5", config.get("configuration.size"));
        assertEquals("vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==", config.get("configuration.url"));
        assertEquals("username0", config.get("configuration.username"));
        // should not be deciphered
        assertEquals("vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==", config.get("configuration.connection.password"));
    }

    @Test
    void migrateOver8196DefaultByteBuffer() {
        final String fakeString = IntStream.range(0, 8196 * 2).mapToObj(String::valueOf).collect(Collectors.joining());
        final String fakeUrl = "https://somefakeurl.ua";

        final JsonBuilderFactory factory = JsonProvider.provider().createBuilderFactory(emptyMap());
        final JsonObject json = factory
                .createObjectBuilder()
                .add("configuration.url", fakeUrl)
                .add("configuration.username", "username0")
                .add("configuration.password", "fake")
                .add("configuration.connection.password", "test")
                .add("configuration.fake", fakeString)
                .build();

        final Map<String, String> config = ws.read(Map.class, "post",
                "/configurationtype/migrate/amRiYy1jb21wb25lbnQjamRiYyNkYXRhc2V0I2pkYmM/-2",
                json.toString());

        assertEquals("true", config.get("configuration.migrated"));
        assertEquals("6", config.get("configuration.size"));
        assertEquals(fakeUrl, config.get("configuration.url"));
        assertEquals("username0", config.get("configuration.username"));
        assertEquals("test", config.get("configuration.connection.password"));
        assertEquals(fakeString, config.get("configuration.fake"));
    }

    private void assertIndex(final ConfigTypeNodes index) {
        assertEquals(5, index.getNodes().size());
        index.getNodes().keySet().forEach(Assertions::assertNotNull); // assert no null ids
        // assert there is at least one parent node
        assertTrue(index.getNodes().values().stream().anyMatch(n -> n.getParentId() == null));
        // assert all edges nodes are in the index
        index
                .getNodes()
                .values()
                .stream()
                .filter(n -> !n.getEdges().isEmpty())
                .flatMap(n -> n.getEdges().stream())
                .forEach(e -> assertTrue(index.getNodes().containsKey(e)));

    }

    private void validateJdbcHierarchy(final ConfigTypeNodes index) {
        final ConfigTypeNode jdbcRoot = index.getNodes().get("amRiYy1jb21wb25lbnQjamRiYw");
        assertNotNull(jdbcRoot);
        assertEquals(singleton("amRiYy1jb21wb25lbnQjamRiYyNkYXRhc3RvcmUjamRiYw"), jdbcRoot.getEdges());
        assertEquals("jdbc", jdbcRoot.getName());

        final ConfigTypeNode jdbcConnection = index.getNodes().get("amRiYy1jb21wb25lbnQjamRiYyNkYXRhc3RvcmUjamRiYw");
        assertNotNull(jdbcConnection);
        assertEquals(singleton("amRiYy1jb21wb25lbnQjamRiYyNkYXRhc2V0I2pkYmM"), jdbcConnection.getEdges());
        assertEquals("jdbc", jdbcConnection.getName());
        assertEquals("JDBC DataStore", jdbcConnection.getDisplayName());
        assertEquals("datastore", jdbcConnection.getConfigurationType());

        final ConfigTypeNode jdbcDataSet = index.getNodes().get("amRiYy1jb21wb25lbnQjamRiYyNkYXRhc2V0I2pkYmM");
        assertNotNull(jdbcDataSet);
        assertEquals(-1, jdbcDataSet.getVersion());
        assertEquals("jdbc", jdbcDataSet.getName());
        assertEquals("JDBC DataSet", jdbcDataSet.getDisplayName());
        assertEquals("dataset", jdbcDataSet.getConfigurationType());
    }

    private void validateConsistencyBetweenNamesAndKeys(final ConfigTypeNode node) {
        // we had a bug where the paths were not rebased and therefore this test was failing
        assertTrue(node.getProperties().stream().anyMatch(it -> it.getName().equals(it.getPath())));
    }
}
