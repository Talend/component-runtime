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
package org.talend.sdk.component.server.front;

import static java.util.Collections.singletonList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.ziplock.IO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndex;
import org.talend.sdk.component.server.front.model.ComponentIndices;
import org.talend.sdk.component.server.front.model.Dependencies;
import org.talend.sdk.component.server.front.model.DependencyDefinition;
import org.talend.sdk.component.server.front.model.Link;
import org.talend.sdk.component.server.front.model.PropertyValidation;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.server.test.ComponentClient;
import org.talend.sdk.component.server.test.websocket.WebsocketClient;

@MonoMeecrowaveConfig
@WithTemporaryFolder
class ComponentResourceTest {

    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    @Inject
    private WebsocketClient ws;

    @Test
    void webSocketGetIndex() {
        assertIndex(ws.read(ComponentIndices.class, "get", "/component/index?includeIconContent=true", ""));
    }

    @Test
    void getDependencies() {
        final String compId = client.getJdbcId();
        final Dependencies dependencies = base
                .path("component/dependencies")
                .queryParam("identifier", compId)
                .request(APPLICATION_JSON_TYPE)
                .get(Dependencies.class);
        assertEquals(1, dependencies.getDependencies().size());
        final DependencyDefinition definition = dependencies.getDependencies().get(compId);
        assertNotNull(definition);
        assertEquals(1, definition.getDependencies().size());
        assertEquals("org.apache.tomee:ziplock:jar:7.0.5", definition.getDependencies().iterator().next());
    }

    @Test
    void getDependency(final TestInfo info, final TemporaryFolder folder) {
        final Function<String, File> download = id -> {
            final InputStream stream = base
                    .path("component/dependency/{id}")
                    .resolveTemplate("id", id)
                    .request(APPLICATION_OCTET_STREAM_TYPE)
                    .get(InputStream.class);
            final File file = new File(folder.getRoot(), info.getTestMethod().get().getName() + ".jar");
            try (final OutputStream outputStream = new FileOutputStream(file)) {
                IO.copy(stream, outputStream);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            return file;
        };

        final Consumer<File> jarValidator = file -> {
            assertTrue(file.exists());
            try (final JarFile jar = new JarFile(file)) {
                assertTrue(jar.entries().hasMoreElements());
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        };

        final File zipLock = download.apply("org.apache.tomee:ziplock:jar:7.0.5");
        jarValidator.accept(zipLock);

        final File component = download.apply(client.getJdbcId());
        jarValidator.accept(component);
    }

    @Test
    void getIndex() {
        assertIndex(client.fetchIndex());
    }

    @Test
    void migrate() {
        final Map<String, String> migrated = base
                .path("component/migrate/{id}/{version}")
                .resolveTemplate("id", client.getJdbcId())
                .resolveTemplate("version", 1)
                .request(APPLICATION_JSON_TYPE)
                .post(entity(new HashMap<String, String>() {

                    {
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        assertEquals(1, migrated.size());
        assertEquals("true", migrated.get("migrated"));
    }

    @Test
    void getDetails() {
        final ComponentDetailList details = base
                .path("component/details")
                .queryParam("identifiers", client
                        .fetchIndex()
                        .getComponents()
                        .stream()
                        .filter(c -> c.getId().getFamily().equals("chain") && c.getId().getName().equals("list"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("no chain#list component"))
                        .getId()
                        .getId())
                .request(APPLICATION_JSON_TYPE)
                .get(ComponentDetailList.class);
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
        assertValidation("remote.urls", detail,
                validation -> validation.getUniqueItems() != null && validation.getUniqueItems());
        assertValidation("remote.user.user", detail,
                validation -> validation.getMinLength() != null && validation.getMinLength() == 2);
        assertValidation("remote.user.password", detail,
                validation -> validation.getMaxLength() != null && validation.getMaxLength() == 8);
        assertValidation("remote.user.password", detail,
                validation -> validation.getRequired() != null && validation.getRequired());

        assertEquals(0, detail.getLinks().size()); // for now
        /*
         * final Link link = detail.getLinks().iterator().next(); assertEquals("Detail", link.getName());
         * assertEquals("/component/...", link.getPath());
         */
    }

    @Test
    void enumDisplayName() {
        final ComponentDetailList details = base
                .path("component/details")
                .queryParam("identifiers", client.getComponentId("jdbc", "output"))
                .request(APPLICATION_JSON_TYPE)
                .get(ComponentDetailList.class);
        assertEquals(1, details.getDetails().size());

        final SimplePropertyDefinition next = details
                .getDetails()
                .iterator()
                .next()
                .getProperties()
                .stream()
                .filter(p -> p.getPath().equals("configuration.type"))
                .findFirst()
                .get();
        assertNotNull(next.getProposalDisplayNames());
        assertEquals(new HashMap<String, String>() {

            {
                put("FAST", "FAST"); // default
                put("PRECISE", "Furious"); // configured
            }
        }, next.getProposalDisplayNames());
    }

    @Test
    void getDetailsMeta() {
        final ComponentDetailList details = base
                .path("component/details")
                .queryParam("identifiers", client.getJdbcId())
                .request(APPLICATION_JSON_TYPE)
                .get(ComponentDetailList.class);
        assertEquals(1, details.getDetails().size());

        final ComponentDetail detail = details.getDetails().iterator().next();
        assertEquals("true",
                detail
                        .getProperties()
                        .stream()
                        .filter(p -> p.getPath().equals("configuration.connection.password"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No credential found"))
                        .getMetadata()
                        .get("ui::credential"));
        assertEquals("0",
                detail
                        .getProperties()
                        .stream()
                        .filter(p -> p.getPath().equals("configuration.timeout"))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No timeout found"))
                        .getDefaultValue());
        assertNull(detail
                .getProperties()
                .stream()
                .filter(p -> p.getPath().equals("configuration.connection.url"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No url found"))
                .getDefaultValue());
    }

    private void assertValidation(final String path, final ComponentDetail aggregate,
            final Predicate<PropertyValidation> validator) {
        assertTrue(validator.test(findProperty(path, aggregate).getValidation()), path);
    }

    private SimplePropertyDefinition findProperty(final String path, final ComponentDetail aggregate) {
        return aggregate
                .getProperties()
                .stream()
                .filter(p -> p.getPath().equals(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No path " + path));
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
        assertEquals(1, data.getLinks().size());
        final Link link = data.getLinks().iterator().next();
        assertEquals("Detail", link.getName());
        assertEquals(
                "/component/details?identifiers=" + Base64
                        .getUrlEncoder()
                        .withoutPadding()
                        .encodeToString((plugin + "#" + family + "#" + name).getBytes(StandardCharsets.UTF_8)),
                link.getPath());
        assertEquals(MediaType.APPLICATION_JSON, link.getContentType());

        if ("jdbc".equals(data.getId().getFamily()) && "input".equals(data.getId().getName())) {
            assertEquals("db-input", data.getIcon().getIcon());
            assertNotNull(data.getIcon().getCustomIcon());
            assertEquals("image/png", data.getIcon().getCustomIconType());
            assertEquals(singletonList("DB/Std/Yes"), data.getCategories());
        } else {
            assertEquals(singletonList("Misc/" + data.getFamilyDisplayName()), data.getCategories());
            assertEquals("default", data.getIcon().getIcon());
        }
    }

    private void assertIndex(final ComponentIndices index) {
        assertEquals(8, index.getComponents().size());

        final List<ComponentIndex> list = new ArrayList<>(index.getComponents());
        list.sort(Comparator.comparing(o -> o.getId().getFamily() + "#" + o.getId().getName()));

        final Iterator<ComponentIndex> component = list.iterator();
        assertComponent("the-test-component", "chain", "count", "count", component, 1);
        assertComponent("the-test-component", "chain", "file", "file", component, 1);
        assertComponent("the-test-component", "chain", "list", "The List Component", component, 1);
        assertComponent("another-test-component", "comp", "proc", "proc", component, 1);
        assertComponent("collection-of-object", "config", "configurationWithArrayOfObject",
                "configurationWithArrayOfObject", component, 1);
        assertComponent("file-component", "file", "output", "output", component, 1);
        assertComponent("jdbc-component", "jdbc", "input", "input", component, 2);
        assertTrue(list.stream().anyMatch(c -> c.getId().getPluginLocation().startsWith("org.talend.test2:")));
    }
}
