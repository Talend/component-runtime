/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import static java.util.Collections.singletonList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.websocket.DeploymentException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.meecrowave.junit.MonoMeecrowave;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.Link;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.server.test.websocket.WebsocketClient;

@RunWith(MonoMeecrowave.Runner.class)
public class ComponentResourceTest {

    @Inject
    private WebTarget base;

    @Inject
    private WebsocketClient ws;

    @Test
    public void getIndexWebSocket() throws IOException, DeploymentException {
        assertIndex(ws.read(ComponentIndices.class, "get", "/component/index", ""));
    }

    @Test
    public void getIndex() {
        assertIndex(fetchIndex());
    }

    @Test
    public void migrate() {
        final Map<String, String> migrated = base.path("component/migrate/{id}/{version}")
                .resolveTemplate("id", fetchIndex().getComponents().stream()
                        .filter(c -> c.getId().getFamily().equals("jdbc") && c.getId().getName().equals("input")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("no jdbc#input component")).getId().getId())
                .resolveTemplate("version", 1).request(APPLICATION_JSON_TYPE).post(entity(new HashMap<String, String>() {

                    {
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        assertEquals(1, migrated.size());
        assertEquals("true", migrated.get("migrated"));
    }

    @Test
    public void getDetails() {
        final ComponentDetailList details = base.path("component/details")
                .queryParam("identifiers", fetchIndex().getComponents().stream()
                        .filter(c -> c.getId().getFamily().equals("chain") && c.getId().getName().equals("list")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("no chain#list component")).getId().getId())
                .request(APPLICATION_JSON_TYPE).get(ComponentDetailList.class);
        assertEquals(1, details.getDetails().size());

        final ComponentDetail detail = details.getDetails().iterator().next();
        assertEquals("the-test-component", detail.getId().getPlugin());
        assertEquals("chain", detail.getId().getFamily());
        assertEquals("list", detail.getId().getName());
        assertEquals("The List Component", detail.getDisplayName());

        final Collection<ActionReference> remoteActions = detail.getActions();
        assertEquals(1, remoteActions.size());
        final ActionReference action = remoteActions.iterator().next();
        assertEquals("default", action.getName());
        assertEquals("healthcheck", action.getType());
        assertEquals(6, action.getProperties().size());

        assertValidation("remote.urls", detail, validation -> validation.getMinItems() == 1);
        assertValidation("remote.urls", detail, validation -> validation.getUniqueItems() != null && validation.getUniqueItems());
        assertValidation("remote.user.user", detail,
                validation -> validation.getMinLength() != null && validation.getMinLength() == 2);
        assertValidation("remote.user.password", detail,
                validation -> validation.getMaxLength() != null && validation.getMaxLength() == 8);
        assertValidation("remote.user.password", detail,
                validation -> validation.getRequired() != null && validation.getRequired());

        assertEquals(0, detail.getLinks().size()); // for now
        /*
         * final Link link = detail.getLinks().iterator().next();
         * assertEquals("Detail", link.getName());
         * assertEquals("/component/...", link.getPath());
         */
    }

    @Test
    public void getDetailsMeta() {
        final ComponentDetailList details = base.path("component/details")
                .queryParam("identifiers", fetchIndex().getComponents().stream()
                        .filter(c -> c.getId().getFamily().equals("jdbc") && c.getId().getName().equals("input")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("no jdbc#input component")).getId().getId())
                .request(APPLICATION_JSON_TYPE).get(ComponentDetailList.class);
        assertEquals(1, details.getDetails().size());

        final ComponentDetail detail = details.getDetails().iterator().next();
        assertEquals("true",
                detail.getProperties().stream().filter(p -> p.getPath().equals("connection.connection.password")).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No credential found")).getMetadata()
                        .get("ui::credential"));
    }

    private void assertValidation(final String path, final ComponentDetail aggregate,
            final Predicate<PropertyValidation> validator) {
        assertTrue(path, validator.test(findProperty(path, aggregate).getValidation()));
    }

    private SimplePropertyDefinition findProperty(final String path, final ComponentDetail aggregate) {
        return aggregate.getProperties().stream().filter(p -> p.getPath().equals(path)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No path " + path));
    }

    private ComponentIndices fetchIndex() {
        return base.path("component/index").request(APPLICATION_JSON_TYPE).get(ComponentIndices.class);
    }

    private void assertComponent(final String plugin, final String family, final String name, final String displayName,
            final Iterator<ComponentIndex> component, final int version) {
        assertTrue(component.hasNext());
        final ComponentIndex data = component.next();
        assertEquals(family, data.getId().getFamily());
        assertEquals(name, data.getId().getName());
        assertEquals(plugin, data.getId().getPlugin());
        assertEquals(displayName, data.getDisplayName());
        assertEquals(version, data.getVersion());
        assertEquals(singletonList("Misc"), data.getCategories());
        assertEquals(1, data.getLinks().size());
        final Link link = data.getLinks().iterator().next();
        assertEquals("Detail", link.getName());
        assertEquals("/component/details?identifiers=" + Base64.getUrlEncoder().withoutPadding()
                .encodeToString((plugin + "#" + family + "#" + name).getBytes(StandardCharsets.UTF_8)), link.getPath());
        assertEquals(MediaType.APPLICATION_JSON, link.getContentType());

        if ("jdbc".equals(data.getId().getFamily()) && "input".equals(data.getId().getName())) {
            assertEquals("db-input", data.getIcon());
            assertNotNull(data.getCustomIcon());
            assertEquals("application/svg+xml", data.getCustomIconType());
        } else {
            assertEquals("default", data.getIcon());
        }
    }

    private void assertIndex(final ComponentIndices index) {
        assertEquals(6, index.getComponents().size());

        final List<ComponentIndex> list = new ArrayList<>(index.getComponents());
        list.sort(Comparator.comparing(o -> o.getId().getFamily() + "#" + o.getId().getName()));

        final Iterator<ComponentIndex> component = list.iterator();
        assertComponent("the-test-component", "chain", "count", "count", component, 1);
        assertComponent("the-test-component", "chain", "file", "file", component, 1);
        assertComponent("the-test-component", "chain", "list", "The List Component", component, 1);
        assertComponent("another-test-component", "comp", "proc", "proc", component, 1);
        assertComponent("file-component", "file", "output", "output", component, 1);
        assertComponent("jdbc-component", "jdbc", "input", "input", component, 2);
    }
}
