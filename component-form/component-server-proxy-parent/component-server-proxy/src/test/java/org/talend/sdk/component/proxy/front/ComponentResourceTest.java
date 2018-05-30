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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.proxy.IO.slurp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.model.Node;
import org.talend.sdk.component.proxy.model.Nodes;
import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
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
        assertEquals("myicon", uiNode.getMetadata().getIcon());
        assertEquals("test-component", uiNode.getMetadata().getPlugin());
    }

    @Test
    void getComponentFormWithInvalidId(final WebTarget webTarget) {
        final WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> webTarget.path("components/x0invalidid/form").request(MediaType.APPLICATION_JSON_TYPE).get(
                        UiNode.class));
        assertNotNull(ex);
        assertEquals(400, ex.getResponse().getStatus());
        final ProxyErrorPayload proxyErrorPayload = ex.getResponse().readEntity(ProxyErrorPayload.class);
        assertEquals("COMPONENT_MISSING", proxyErrorPayload.getCode());
        assertTrue(proxyErrorPayload.getMessage().contains("x0invalidid"));
    }

    @Test
    void getComponentIcon(final WebTarget webTarget) throws IOException {
        final Node node = webTarget
                .path("components")
                .request()
                .get(Nodes.class)
                .getNodes()
                .values()
                .stream()
                .min(Comparator.comparing(Node::getName))
                .orElseThrow(() -> new IllegalStateException("test failed, no component is found"));
        final byte[] serverIcon =
                webTarget.path("components/" + node.getId() + "/icon").request(APPLICATION_OCTET_STREAM_TYPE).get(
                        byte[].class);
        assertNotNull(serverIcon);
        byte[] testIcon;
        try (final InputStream customIs = getClass().getClassLoader().getResourceAsStream("icons/myicon_icon32.png")) {
            testIcon = slurp(customIs, customIs.available());
        }
        assertNotNull(testIcon);
        assertTrue(Arrays.equals(testIcon, serverIcon), "Icon are not the same as defined by the component");
    }

    @Test
    void getComponentIconWithMissingId(final WebTarget webTarget) {
        final WebApplicationException ex = assertThrows(WebApplicationException.class,
                () -> webTarget
                        .path("components/x0invalidid/icon")
                        .request(APPLICATION_OCTET_STREAM_TYPE, APPLICATION_JSON_TYPE)
                        .get(byte[].class));
        assertEquals(404, ex.getResponse().getStatus());
        final ProxyErrorPayload proxyErrorPayload = ex.getResponse().readEntity(ProxyErrorPayload.class);
        assertEquals("COMPONENT_MISSING", proxyErrorPayload.getCode());
        assertTrue(proxyErrorPayload.getMessage().contains("x0invalidid"));
    }

}
