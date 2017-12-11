/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.server.front;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.inject.Inject;
import javax.websocket.DeploymentException;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.test.websocket.WebsocketClient;

@RunWith(MonoMeecrowave.Runner.class)
public class ConfigurationTypeResourceTest {

    @Inject
    private WebsocketClient ws;

    @Test
    public void webSocketGetIndex() throws IOException, DeploymentException {
        final ConfigTypeNodes index = ws.read(ConfigTypeNodes.class, "get", "/configurationtype/index", "");
        assertIndex(index);
        validateJdbcHierarchy(index);
    }

    private void assertIndex(final ConfigTypeNodes index) {
        assertEquals(5, index.getNodes().size());
        index.getNodes().keySet().forEach(Assert::assertNotNull); // assert no null ids
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
        final ConfigTypeNode jdbcRoot = index.getNodes().get("amRiYw");
        assertNotNull(jdbcRoot);
        assertEquals(singleton("amRiYyNkYXRhc3RvcmUjamRiYw"), jdbcRoot.getEdges());
        assertEquals("jdbc", jdbcRoot.getName());

        final ConfigTypeNode jdbcConnection = index.getNodes().get("amRiYyNkYXRhc3RvcmUjamRiYw");
        assertNotNull(jdbcConnection);
        assertEquals(singleton("amRiYyNkYXRhc2V0I2pkYmM"), jdbcConnection.getEdges());
        assertEquals("jdbc", jdbcConnection.getName());
        assertEquals("JDBC DataStore", jdbcConnection.getDisplayName());
        assertEquals("datastore", jdbcConnection.getConfigurationType());
        assertEquals("[{\"description\":\"D1\",\"driver\":\"d1\"},{\"description\":\"D2\",\"driver\":\"d2\"}]",
                jdbcConnection
                        .getProperties()
                        .stream()
                        .filter(p -> "configuration.connection.configurations".equals(p.getPath()))
                        .findFirst()
                        .get()
                        .getDefaultValue());

        final ConfigTypeNode jdbcDataSet = index.getNodes().get("amRiYyNkYXRhc2V0I2pkYmM");
        assertNotNull(jdbcDataSet);
        assertEquals("jdbc", jdbcDataSet.getName());
        assertEquals("JDBC DataSet", jdbcDataSet.getDisplayName());
        assertEquals("dataset", jdbcDataSet.getConfigurationType());
    }

}
