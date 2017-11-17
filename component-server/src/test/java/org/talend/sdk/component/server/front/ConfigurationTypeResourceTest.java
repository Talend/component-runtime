package org.talend.sdk.component.server.front;

import java.io.IOException;
import javax.inject.Inject;
import javax.websocket.DeploymentException;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.test.websocket.WebsocketClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
@RunWith(MonoMeecrowave.Runner.class)
public class ConfigurationTypeResourceTest {

    @Inject
    private WebsocketClient ws;

    @Test
    public void webSocketGetIndex() throws IOException, DeploymentException {
        assertIndex(ws.read(ConfigTypeNodes.class, "get", "/configurationtype/index", ""));
    }

    private void assertIndex(final ConfigTypeNodes index) {
        assertEquals(5, index.getNodes().size());
        index.getNodes().keySet().forEach(Assert::assertNotNull); // assert no null ids
        //assert there is at least one parent node
        Assert.assertTrue(index.getNodes().values().stream().filter(n -> n.getParentId() == null)
                               .findAny().isPresent());
        //assert all edges nodes are in the index
        index.getNodes().values().stream().filter(n -> !n.getEdges().isEmpty())
             .flatMap(n -> n.getEdges().stream())
             .forEach(e -> assertTrue(index.getNodes().containsKey(e)));

    }

}
