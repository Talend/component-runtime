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

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Comparator;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.Nodes;
import org.talend.sdk.component.proxy.model.UiNode;
import org.talend.sdk.component.proxy.test.WithServer;

@WithServer
class ComponentResourceTest {

    @Test
    void getComponents(final WebTarget webTarget) {
        final Nodes components = webTarget.path("components").request(MediaType.APPLICATION_JSON_TYPE).get(Nodes.class);

        assertNotNull(components);
        assertEquals(3, components.getNodes().size());
        assertEquals("TheMapper1,TheMapper2,TheMapper3",
                components.getNodes().entrySet().stream().map(e -> e.getValue().getName()).sorted().collect(
                        joining(",")));
    }

    @Test
    void getComponentForm(final WebTarget webTarget) {
        final String id = webTarget
                .path("components")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(Nodes.class)
                .getNodes()
                .values()
                .stream()
                .sorted(Comparator.comparing(Node::getName))
                .map(Node::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("test failed, no component is found"));

        final UiNode uiNode =
                webTarget.path("components/" + id + "/form").request(MediaType.APPLICATION_JSON_TYPE).get(UiNode.class);

        assertNotNull(uiNode);
        assertNotNull(uiNode.getUi());
        assertNotNull(uiNode.getMetadata());
        assertEquals("TheMapper1", uiNode.getMetadata().getName());
        assertEquals("star", uiNode.getMetadata().getIcon());
        assertEquals("test-component", uiNode.getMetadata().getPlugin());
    }

}
