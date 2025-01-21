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
package org.talend.sdk.component.server.front;

import static java.util.Collections.singletonList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_SVG_XML_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.talend.sdk.component.server.front.ComponentResourceImpl.COMPONENT_TYPE_INPUT;
import static org.talend.sdk.component.server.front.ComponentResourceImpl.COMPONENT_TYPE_PROCESSOR;
import static org.talend.sdk.component.server.front.ComponentResourceImpl.COMPONENT_TYPE_STANDALONE;
import static org.talend.sdk.component.server.front.ComponentResourceImpl.IMAGE_SVG_XML_TYPE;

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
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.apache.ziplock.IO;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.runtime.manager.extension.ComponentSchemaEnricher;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.ComponentDetail;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentId;
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
class ComponentResourceImplTest {

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

    @RepeatedTest(2) // this also checks the cache and queries usage
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
        assertEquals("org.apache.tomee:ziplock:jar:8.0.14", definition.getDependencies().iterator().next());
    }

    @RepeatedTest(2) // this also checks the cache and queries usage
    void getDependency(final TestInfo info, @TempDir final File folder) {
        final Function<String, File> download = id -> {
            final InputStream stream = base
                    .path("component/dependency/{id}")
                    .resolveTemplate("id", id)
                    .request(APPLICATION_OCTET_STREAM_TYPE)
                    .get(InputStream.class);
            final File file = new File(folder, info.getTestMethod().get().getName() + ".jar");
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

        final File zipLock = download.apply("org.apache.tomee:ziplock:jar:8.0.14");
        jarValidator.accept(zipLock);

        final File component = download.apply(client.getJdbcId());
        jarValidator.accept(component);
    }

    @RepeatedTest(2) // this also checks the cache and queries usage
    void getIndex() {
        assertIndex(client.fetchIndex());
    }

    @RepeatedTest(2) // this also checks the cache and queries usage
    void getIndexWithQuery() {
        final List<ComponentIndex> components = base
                .path("component/index")
                .queryParam("includeIconContent", false)
                .queryParam("q",
                        "(id = " + client.getJdbcId() + ") AND " + "(metadata[configurationtype::type] = dataset) AND "
                                + "(plugin = jdbc-component) AND (name = input)")
                .request(APPLICATION_JSON_TYPE)
                .header("Accept-Encoding", "gzip")
                .get(ComponentIndices.class)
                .getComponents();
        assertEquals(1, components.size());

        final ComponentId id = components.iterator().next().getId();
        assertEquals("jdbc#input", id.getFamily() + "#" + id.getName());
    }

    @Test()
    void getFixedSchemaMetadata() {
        final List<ComponentIndex> components = base
                .path("component/index")
                .queryParam("includeIconContent", false)
                .queryParam("q",
                        "(id = " + client.getJdbcId() + ") AND " + "(metadata[configurationtype::type] = dataset) AND "
                                + "(plugin = jdbc-component) AND (name = input)")
                .request(APPLICATION_JSON_TYPE)
                .header("Accept-Encoding", "gzip")
                .get(ComponentIndices.class)
                .getComponents();
        assertEquals(1, components.size());
        final ComponentIndex index = components.iterator().next();
        assertEquals("jdbc#input", index.getId().getFamily() + "#" + index.getId().getName());
        assertEquals("jdbc_discover_schema", index.getMetadata().get(ComponentSchemaEnricher.FIXED_SCHEMA_META_PREFIX));
        assertEquals(Branches.DEFAULT_BRANCH,
                index.getMetadata().get(ComponentSchemaEnricher.FIXED_SCHEMA_FLOWS_META_PREFIX));
    }

    @Test()
    void getFixedSchemaMetadataWithRejectFlow() {
        final List<ComponentIndex> components = base
                .path("component/index")
                .queryParam("includeIconContent", false)
                .queryParam("q", "(id = " + client.getJdbcOutputId() + ") AND (plugin = jdbc-component)")
                .request(APPLICATION_JSON_TYPE)
                .header("Accept-Encoding", "gzip")
                .get(ComponentIndices.class)
                .getComponents();
        assertEquals(1, components.size());
        final ComponentIndex index = components.iterator().next();
        assertEquals("jdbc#output", index.getId().getFamily() + "#" + index.getId().getName());
        assertEquals("jdbc_discover_schema", index.getMetadata().get(ComponentSchemaEnricher.FIXED_SCHEMA_META_PREFIX));
        assertEquals("reject", index.getMetadata().get(ComponentSchemaEnricher.FIXED_SCHEMA_FLOWS_META_PREFIX));
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
    void noMigrateWithUpperVersion() {
        final Map<String, String> migrated = base
                .path("component/migrate/{id}/{version}")
                .resolveTemplate("id", client.getJdbcId())
                .resolveTemplate("version", 3)
                .request(APPLICATION_JSON_TYPE)
                .post(entity(new HashMap<String, String>() {

                    {
                        put("going", "nowhere");
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        assertEquals(1, migrated.size());
        assertEquals(null, migrated.get("migrated"));
    }

    @Test
    void searchIcon() {
        assertNotNull(base.path("component/icon/custom/{familyId}/{iconKey}")
                .resolveTemplate("familyId", client.getFamilyId("jdbc"))
                .resolveTemplate("iconKey", "logo")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(String.class));
    }

    @Test
    void themedIcon() {
        final String id = client.getJdbcId();
        //
        Response icon = base.path("component/icon/{id}")
                .resolveTemplate("id", id)
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(Response.class);
        assertNotNull(icon);
        assertEquals("image/png", icon.getMediaType().toString());
        //
        icon = base.path("component/icon/{id}")
                .resolveTemplate("id", id)
                .queryParam("theme", "dark")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(Response.class);
        assertNotNull(icon);
        assertEquals(IMAGE_SVG_XML_TYPE, icon.getMediaType().toString());
    }

    @Test
    void themedFamilyIcon() {
        final String family = client.getFamilyId("chain");

        Response icon = base.path("component/icon/family/{id}")
                .resolveTemplate("id", family)
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(Response.class);
        assertNotNull(icon);
        assertEquals("image/png", icon.getMediaType().toString());

        icon = base.path("component/icon/family/{id}")
                .resolveTemplate("id", family)
                .queryParam("theme", "dark")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(Response.class);
        assertNotNull(icon);
        assertEquals(IMAGE_SVG_XML_TYPE, icon.getMediaType().toString());

        assertThrows(NotFoundException.class, () -> base.path("component/icon/family/{id}")
                .resolveTemplate("id", family)
                .queryParam("theme", "dak")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(String.class));
    }

    @Test
    void customThemedIcon() {
        final String family = client.getFamilyId("jdbc");
        String icon = base.path("component/icon/custom/{familyId}/{iconKey}")
                .resolveTemplate("familyId", family)
                .resolveTemplate("iconKey", "logo")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(String.class);
        assertNotNull(icon);
        assertTrue(icon.contains("light"));
        //
        icon = base.path("component/icon/custom/{familyId}/{iconKey}")
                .resolveTemplate("familyId", family)
                .resolveTemplate("iconKey", "logo")
                .queryParam("theme", "dark")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(String.class);
        assertNotNull(icon);
        assertTrue(icon.contains("dark"));
        // even if theme does not exist, the legacy lookup should find the top level logo.svg
        icon = base.path("component/icon/custom/{familyId}/{iconKey}")
                .resolveTemplate("familyId", family)
                .resolveTemplate("iconKey", "logo")
                .queryParam("theme", "dak")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(String.class);
        assertNotNull(icon);
        // invalid iconKey
        assertThrows(NotFoundException.class, () -> base.path("component/icon/custom/{familyId}/{iconKey}")
                .resolveTemplate("familyId", family)
                .resolveTemplate("iconKey", "log")
                .queryParam("theme", "dak")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .accept(APPLICATION_OCTET_STREAM_TYPE)
                .get(String.class));
    }

    @Test
    void getIconIndex() {
        // wrong content type
        Response icons = base.path("component/icon/index")
                .request(APPLICATION_SVG_XML_TYPE)
                .accept(APPLICATION_SVG_XML_TYPE)
                .get();
        assertNotNull(icons);
        assertEquals(406, icons.getStatus());
        // inexistant theme (no fallback)
        icons = base.path("component/icon/index")
                .queryParam("theme", "dak")
                .request(IMAGE_SVG_XML_TYPE)
                .accept(IMAGE_SVG_XML_TYPE)
                .get();
        assertEquals(404, icons.getStatus());
        assertEquals(APPLICATION_JSON_TYPE, icons.getMediaType());

        // content type
        icons = base.path("component/icon/index")
                .request(IMAGE_SVG_XML_TYPE)
                .accept(IMAGE_SVG_XML_TYPE, APPLICATION_JSON_TYPE.toString())
                .get();
        assertNotNull(icons);
        assertEquals(IMAGE_SVG_XML_TYPE, icons.getMediaType().toString());
        // default: light theme
        String content = base.path("component/icon/index")
                .request(IMAGE_SVG_XML_TYPE)
                .accept(IMAGE_SVG_XML_TYPE)
                .get(String.class);
        assertNotNull(content);
        assertTrue(
                content.startsWith(
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"sr-only\" data-theme=\"light\" focusable=\"false\">"));
        assertTrue(content.contains(
                "data-connector=\"standalone\" data-family=\"chain\" data-theme=\"light\" data-type=\"connector\" id=\"myicon-light\""));
        assertTrue(
                content.contains(
                        "data-family=\"file\" data-theme=\"light\" data-type=\"family\" id=\"file-family-light\""));
        // light theme
        content = base.path("component/icon/index")
                .queryParam("theme", "light")
                .request(IMAGE_SVG_XML_TYPE)
                .accept(IMAGE_SVG_XML_TYPE)
                .get(String.class);
        assertNotNull(content);
        assertTrue(
                content.startsWith(
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"sr-only\" data-theme=\"light\" focusable=\"false\">"));
        assertTrue(content.contains(
                "data-connector=\"standalone\" data-family=\"chain\" data-theme=\"light\" data-type=\"connector\" id=\"myicon-light\""));
        assertTrue(
                content.contains(
                        "data-family=\"file\" data-theme=\"light\" data-type=\"family\" id=\"file-family-light\""));

        // dark theme
        content = base.path("component/icon/index")
                .queryParam("theme", "dark")
                .request(IMAGE_SVG_XML_TYPE)
                .accept(IMAGE_SVG_XML_TYPE)
                .get(String.class);
        assertNotNull(content);
        assertTrue(
                content.startsWith(
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"sr-only\" data-theme=\"dark\" focusable=\"false\">"));
        assertTrue(content
                .contains(
                        "data-connector=\"input\" data-family=\"jdbc\" data-theme=\"dark\" data-type=\"connector\" id=\"db-input-dark\""));
        assertTrue(content
                .contains(
                        "data-connector=\"output\" data-family=\"jdbc\" data-theme=\"dark\" data-type=\"connector\" id=\"db-input-dark\""));
        // theme = all
        content = base.path("component/icon/index")
                .queryParam("theme", "all")
                .request(IMAGE_SVG_XML_TYPE)
                .accept(IMAGE_SVG_XML_TYPE)
                .get(String.class);
        assertNotNull(content);
        System.out.println(content);
        assertTrue(
                content.startsWith(
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"sr-only\" data-theme=\"all\" focusable=\"false\">"));
        assertTrue(content
                .contains(
                        "data-connector=\"input\" data-family=\"jdbc\" data-theme=\"dark\" data-type=\"connector\" id=\"db-input-dark\""));
        assertTrue(content
                .contains(
                        "data-connector=\"output\" data-family=\"jdbc\" data-theme=\"dark\" data-type=\"connector\" id=\"db-input-dark\""));
        assertTrue(content.contains(
                "data-connector=\"standalone\" data-family=\"chain\" data-theme=\"light\" data-type=\"connector\" id=\"myicon-light\""));
        assertTrue(
                content.contains(
                        "data-family=\"file\" data-theme=\"dark\" data-type=\"family\" id=\"file-family-dark\""));
    }

    @Test
    void migrateFromStudio() {
        final Map<String, String> migrated = base
                .path("component/migrate/{id}/{version}")
                .resolveTemplate("id", client.getJdbcId())
                .resolveTemplate("version", 1)
                .request(APPLICATION_JSON_TYPE)
                .post(entity(new HashMap<String, String>() {

                    {
                        put("going", "nowhere");
                        put("configuration.dataSet.connection.authMethod", "base64://QWN0aXZlRGlyZWN0b3J5");
                        put("configuration.dataSet.blobPath",
                                "base64://KFN0cmluZylnbG9iYWxNYXAuZ2V0KCJTWVNURU1aVCIpKyIvIitjb250ZXh0LmN0eE5vbVRhYmxlU291cmNlKyIvIitjb250ZXh0LmN0eE5vbVRhYmxlU291cmNlKyJfIitTdHJpbmdIYW5kbGluZy5DSEFOR0UoY29udGV4dC5jdHhEYXRlRGVidXRUcmFpdGVtZW50LCAiW15cXGRdIiwgIiIpKyIvIg==");
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        assertEquals(4, migrated.size());
        assertEquals(
                "(String)globalMap.get(\"SYSTEMZT\")+\"/\"+context.ctxNomTableSource+\"/\"+context.ctxNomTableSource+\"_\"+StringHandling.CHANGE(context.ctxDateDebutTraitement, \"[^\\\\d]\", \"\")+\"/\"",
                migrated.get("configuration.dataSet.blobPath"));
        assertEquals("ActiveDirectory", migrated.get("configuration.dataSet.connection.authMethod"));
        assertEquals("nowhere", migrated.get("going"));
        assertEquals("true", migrated.get("migrated"));
    }

    @Test
    void migrateFromStudioWs() {
        final Map<String, String> migrated = ws
                .read(Map.class, "post", String.format("/component/migrate/%s/1", client.getJdbcId()),
                        "{\"going\":\"nowhere\",\"configuration.dataSet.connection.authMethod\":\"base64://QWN0aXZlRGlyZWN0b3J5\",\"configuration.dataSet.blobPath\":\"base64://KFN0cmluZylnbG9iYWxNYXAuZ2V0KCJTWVNURU1aVCIpKyIvIitjb250ZXh0LmN0eE5vbVRhYmxlU291cmNlKyIvIitjb250ZXh0LmN0eE5vbVRhYmxlU291cmNlKyJfIitTdHJpbmdIYW5kbGluZy5DSEFOR0UoY29udGV4dC5jdHhEYXRlRGVidXRUcmFpdGVtZW50LCAiW15cXGRdIiwgIiIpKyIvIg==\"}");
        assertEquals(4, migrated.size());
        assertEquals(
                "(String)globalMap.get(\"SYSTEMZT\")+\"/\"+context.ctxNomTableSource+\"/\"+context.ctxNomTableSource+\"_\"+StringHandling.CHANGE(context.ctxDateDebutTraitement, \"[^\\\\d]\", \"\")+\"/\"",
                migrated.get("configuration.dataSet.blobPath"));
        assertEquals("ActiveDirectory", migrated.get("configuration.dataSet.connection.authMethod"));
        assertEquals("nowhere", migrated.get("going"));
        assertEquals("true", migrated.get("migrated"));
    }

    @Test
    void migrateWithEncrypted() {
        final Map<String, String> migrated = base
                .path("component/migrate/{id}/{version}")
                .resolveTemplate("id", client.getJdbcId())
                .resolveTemplate("version", 1)
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(entity(new HashMap<String, String>() {

                    {
                        put("configuration.url", "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                        put("configuration.username", "username0");
                        put("configuration.connection.password",
                                "vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==");
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        assertEquals(4, migrated.size());
        assertEquals("true", migrated.get("migrated"));
        assertEquals("vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==", migrated.get("configuration.url"));
        assertEquals("username0", migrated.get("configuration.username"));
        // should not be deciphered
        assertEquals("vault:v1:hcccVPODe9oZpcr/sKam8GUrbacji8VkuDRGfuDt7bg7VA==",
                migrated.get("configuration.connection.password"));
    }

    @RepeatedTest(2) // this also checks the cache and queries usage
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
        assertEquals("false", detail.getMetadata().get("mapper::infinite"));
        assertEquals("false", detail.getMetadata().get("mapper::optionalRow"));
        IntStream.of(0, 5).forEach(i -> assertEquals(String.valueOf(i), detail.getMetadata().get("testing::v" + i)));

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

    @RepeatedTest(2) // this also checks the cache and queries usage
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

    @RepeatedTest(2) // this also checks the cache and queries usage
    void getDetailsStandaloneType() {
        final ComponentDetailList details = base
                .path("component/details")
                .queryParam("identifiers", client.getStandaloneId())
                .request(APPLICATION_JSON_TYPE)
                .get(ComponentDetailList.class);
        assertEquals(1, details.getDetails().size());

        final ComponentDetail detail = details.getDetails().iterator().next();
        assertEquals("standalone", detail.getType());
    }

    @RepeatedTest(2) // this also checks the cache and queries usage
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

    @Test
    void getStreamingDetails() {
        final ComponentDetailList details = base
                .path("component/details")
                .queryParam("identifiers", client.getStreamingId())
                .request(APPLICATION_JSON_TYPE)
                .get(ComponentDetailList.class);
        assertEquals(1, details.getDetails().size());
        final ComponentDetail detail = details.getDetails().iterator().next();
        assertEquals(1, detail.getProperties().size());
        assertThrows(IllegalArgumentException.class, () -> detail
                .getProperties()
                .stream()
                .filter(p -> p.getPath().equals("configuration.$maxDurationMs"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No max duration found")));
        assertThrows(IllegalArgumentException.class, () -> detail
                .getProperties()
                .stream()
                .filter(p -> p.getPath().equals("configuration.$maxRecords"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No max records found")));
    }

    @Test
    void getStreamingStoppableDetails() {
        final ComponentDetailList details = base
                .path("component/details")
                .queryParam("identifiers", client.getStreamingStoppableId())
                .request(APPLICATION_JSON_TYPE)
                .get(ComponentDetailList.class);
        assertEquals(1, details.getDetails().size());
        final ComponentDetail detail = details.getDetails().iterator().next();
        assertEquals(3, detail.getProperties().size());
        org.talend.sdk.component.server.front.model.SimplePropertyDefinition maxDuration = detail
                .getProperties()
                .stream()
                .filter(p -> p.getPath().equals("configuration.$maxDurationMs"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No max duration found"));
        assertEquals("-1", maxDuration.getMetadata().get("ui::defaultvalue::value"));
        assertEquals(-1, maxDuration.getValidation().getMin());
        org.talend.sdk.component.server.front.model.SimplePropertyDefinition maxRecords = detail
                .getProperties()
                .stream()
                .filter(p -> p.getPath().equals("configuration.$maxRecords"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No max records found"));
        assertEquals("-1", maxRecords.getMetadata().get("ui::defaultvalue::value"));
        assertEquals(-1, maxRecords.getValidation().getMin());
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
            final Iterator<ComponentIndex> component, final String type, final int version) {
        assertTrue(component.hasNext());
        final ComponentIndex data = component.next();
        assertEquals(family, data.getId().getFamily());
        assertEquals(name, data.getId().getName());
        assertEquals(plugin, data.getId().getPlugin());
        assertEquals(displayName, data.getDisplayName());
        assertEquals(type, data.getType());
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
        } else if ("chain".equals(data.getId().getFamily())
                && ("file".equals(data.getId().getName()) || "standalone".equals(data.getId().getName()))) {
            assertEquals("myicon", data.getIcon().getIcon());
            assertEquals("light", data.getIcon().getTheme());
            assertTrue(new String(data.getIcon().getCustomIcon(), StandardCharsets.UTF_8)
                    .startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\""));
            assertEquals(IMAGE_SVG_XML_TYPE, data.getIcon().getCustomIconType());
            assertEquals(singletonList("Misc/" + data.getFamilyDisplayName()), data.getCategories());
        } else {
            assertEquals(singletonList("Misc/" + data.getFamilyDisplayName()), data.getCategories());
            assertEquals("default", data.getIcon().getIcon());
        }
    }

    private void assertIndex(final ComponentIndices index) {
        assertEquals(13, index.getComponents().size());

        final List<ComponentIndex> list = new ArrayList<>(index.getComponents());
        list.sort(Comparator.comparing(o -> o.getId().getFamily() + "#" + o.getId().getName()));

        final Iterator<ComponentIndex> component = list.iterator();
        assertComponent("the-test-component", "chain", "count", "count", component, COMPONENT_TYPE_PROCESSOR, 1);
        assertComponent("the-test-component", "chain", "file", "file", component, COMPONENT_TYPE_PROCESSOR, 1);
        assertComponent("the-test-component", "chain", "list", "The List Component", component, COMPONENT_TYPE_INPUT,
                1);
        assertComponent("the-test-component", "chain", "standalone", "standalone", component, COMPONENT_TYPE_STANDALONE,
                1);
        assertComponent("another-test-component", "comp", "proc", "proc", component, COMPONENT_TYPE_PROCESSOR, 1);
        assertComponent("collection-of-object", "config", "configurationWithArrayOfObject",
                "configurationWithArrayOfObject", component, COMPONENT_TYPE_PROCESSOR, 1);
        assertComponent("component-with-user-jars", "custom", "noop", "noop", component, COMPONENT_TYPE_PROCESSOR, 1);
        assertComponent("file-component", "file", "output", "output", component, COMPONENT_TYPE_PROCESSOR, 1);
        assertComponent("jdbc-component", "jdbc", "input", "input", component, COMPONENT_TYPE_INPUT, 2);
        assertTrue(list.stream().anyMatch(c -> c.getId().getPluginLocation().startsWith("org.talend.test2:")));
    }
}
