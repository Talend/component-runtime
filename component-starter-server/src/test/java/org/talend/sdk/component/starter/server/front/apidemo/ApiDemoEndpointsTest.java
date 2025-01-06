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
package org.talend.sdk.component.starter.server.front.apidemo;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.starter.server.test.Client;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@MonoMeecrowaveConfig
@Client.Active
class ApiDemoEndpointsTest {

    @Test
    void realEnvironment(final WebTarget target) throws Exception {
        final JsonObject result = target
                .path("environment")
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
        log.warn("[environment] {}", result);
        assertNotNull(result.get("version"));
        assertNotNull(result.get("release"));
        assertNotNull(result.get("branch"));
        assertNotNull(result.get("commit"));
    }

    @Test
    void environment(final WebTarget target) throws Exception {
        final JsonObject result = target
                .path("demo/{version}/api/v1/environment")
                .resolveTemplate("version", "1.29.0")
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
        log.warn("[environment] {}", result);
        assertNotNull(result.get("commit"));
        assertEquals("1.29.0", result.getString("version"));
        assertEquals(1, result.getInt("latestApiVersion"));
    }

    @Test
    void actionIndex(final WebTarget target) {
        final JsonObject result = target
                .path("demo/{version}/api/v1/action/index")
                .resolveTemplate("version", "1.29.0")
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
        log.debug("[actionIndex] {}", result);
        JsonArray items = result.getJsonArray("items");
        assertEquals(4, items.size());
        JsonObject urlValidation = items.getJsonObject(0);
        assertEquals("Mock", urlValidation.getString("component"));
        assertEquals("urlValidation", urlValidation.getString("name"));
        JsonObject property = urlValidation.getJsonArray("properties").get(0).asJsonObject();
        JsonObject metadata = property.getJsonObject("metadata");
        assertEquals("0", metadata.getString("definition::parameter::index"));
    }

    @Test
    void actionExecute(final WebTarget target) {
        final Map<String, String> result = target
                .path("demo/{version}/api/v1/action/execute")
                .resolveTemplate("version", "1.29.0")
                .queryParam("family", "testf")
                .queryParam("type", "testt")
                .queryParam("action", "testa")
                .queryParam("lang", "testl")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(entity(new HashMap<String, String>() {

                    {
                        put("configuration.username", "simple");
                        put("configuration.password", "clearValue");
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        log.debug("[actionExecute] {}", result);
        assertEquals("KO", result.get("status"));
        assertEquals("Connection failed!\n"
                + "Cause: java.net.MalformedURLException: no protocol: nullapi/now/v2/table/incident?SYSPARM_OFFSET=0&SYSPARM_LIMIT=1&SYSPARM_EXCLUDE_REFERENCE_LINK=true&SYSPARM_SUPPRESS_PAGINATION_HEADER=true.",
                result.get("comment"));
    }

    @Test
    void componentIndex(final WebTarget target) {
        final JsonObject result = target
                .path("demo/{version}/api/v1/component/index")
                .resolveTemplate("version", "1.29.0")
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
        log.debug("[componentIndex] {}", result);
        assertEquals(2, result.getJsonArray("components").size());
        JsonObject component = result.getJsonArray("components").get(0).asJsonObject();
        assertEquals("MockInput", component.getString("displayName"));
        assertEquals("Mock", component.getString("familyDisplayName"));
        JsonObject id = component.getJsonObject("id");
        assertEquals("Y29tcG9uZW50cyNNb2NrI01vY2tJbnB1dA", id.getString("id"));
        assertEquals("MockInput", id.getString("name"));
        assertEquals("components", id.getString("plugin"));
        assertEquals("org.talend.demo:components:1.0.0", id.getString("pluginLocation"));
    }

    @Test
    void componentDetails(final WebTarget target) {
        final JsonObject result = target
                .path("demo/{version}/api/v1/component/details")
                .resolveTemplate("version", "1.29.0")
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
        log.debug("[componentDetails] {}", result);
        JsonObject details = result.getJsonArray("details").get(0).asJsonObject();
        assertEquals(11, details.size());
    }

    @Test
    void componentDependencies(final WebTarget target) {
        final JsonObject result = target
                .path("demo/{version}/api/v1/component/dependencies")
                .resolveTemplate("version", "1.29.0")
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
        log.debug("[componentDependencies] {}", result);
        assertTrue(JsonObject.class.isInstance(result.getJsonObject("dependencies")));
        assertEquals(0, result.getJsonObject("dependencies").size());
    }

    @Test
    void componentDependency(final WebTarget target) {
        final Object result = target
                .path("demo/{version}/api/v1/component/dependency/{id}")
                .resolveTemplate("version", "1.29.0")
                .resolveTemplate("id", "component")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .get(Object.class);
        log.debug("[componentDependency] {}", result);
        assertTrue(InputStream.class.isInstance(result));
    }

    @Test
    void componentIcon(final WebTarget target) {
        final Object result = target
                .path("demo/{version}/api/v1/component/icon/{id}")
                .resolveTemplate("version", "1.29.0")
                .resolveTemplate("id", "iddid")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .get(Object.class);
        log.debug("[componentIcon] {}", result);
        assertTrue(InputStream.class.isInstance(result));
    }

    @Test
    void componentIconFamily(final WebTarget target) {
        final Object result = target
                .path("demo/{version}/api/v1/component/icon/family/{id}")
                .resolveTemplate("version", "1.29.0")
                .resolveTemplate("id", "iddid")
                .request(APPLICATION_OCTET_STREAM_TYPE)
                .get(Object.class);
        log.debug("[componentIconFamily] {}", result);
        assertTrue(InputStream.class.isInstance(result));
    }

    @Test
    void componentMigrate(final WebTarget target) {
        final Map<String, String> result = target
                .path("demo/{version}/api/v1/component/migrate/{id}/{configurationVersion}")
                .resolveTemplate("version", "1.29.0")
                .resolveTemplate("id", "test")
                .resolveTemplate("configurationVersion", "1")
                .queryParam("action", "testa")
                .queryParam("lang", "testl")
                .request(APPLICATION_JSON_TYPE)
                .header("x-talend-tenant-id", "test-tenant")
                .post(entity(new HashMap<String, String>() {

                    {
                        put("configuration.username", "simple");
                        put("configuration.password", "clearValue");
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        log.debug("[componentMigrate] {}", result);
        assertEquals(0, result.size());
    }

    @Test
    void configurationtypeIndex(final WebTarget target) {
        final JsonObject result = target
                .path("demo/{version}/api/v1/configurationtype/index")
                .resolveTemplate("version", "1.29.0")
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
        log.debug("[configurationtypeIndex] {}", result);
        assertEquals(3, result.getJsonObject("nodes").size());
        JsonObject component = result.getJsonObject("nodes").getJsonObject("Y29tcG9uZW50cyNNb2Nr");
        assertEquals("Mock", component.getString("displayName"));
        assertEquals("Mock", component.getString("name"));
        assertEquals("Y29tcG9uZW50cyNNb2Nr", component.getString("id"));
        assertEquals(0, component.getInt("version"));
    }

    @Test
    void configurationtypeDetails(final WebTarget target) {
        final JsonObject result = target
                .path("demo/{version}/api/v1/configurationtype/details")
                .resolveTemplate("version", "1.29.0")
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
        log.debug("[configurationtypeDetails] {}", result);
        assertEquals(1, result.getJsonObject("nodes").size());
        JsonObject config = result.getJsonObject("nodes").getJsonObject("Y29tcG9uZW50cyNNb2NrI2RhdGFzZXQjdGFibGU");
        assertEquals("Table", config.getString("displayName"));
        assertEquals("table", config.getString("name"));
        assertEquals("Y29tcG9uZW50cyNNb2NrI2RhdGFzZXQjdGFibGU", config.getString("id"));
        assertEquals(-1, config.getInt("version"));
    }

    @Test
    void configurationtypeMigrate(final WebTarget target) {
        final Map<String, String> result = target
                .path("demo/{version}/api/v1/configurationtype/migrate/{id}/{configurationVersion}")
                .resolveTemplate("version", "1.29.0")
                .resolveTemplate("id", "test")
                .resolveTemplate("configurationVersion", "1")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(new HashMap<String, String>() {

                    {
                        put("configuration.username", "simple");
                        put("configuration.password", "clearValue");
                    }
                }, APPLICATION_JSON_TYPE), new GenericType<Map<String, String>>() {
                });
        log.debug("[configurationtypeMigrate] {}", result);
        assertEquals(0, result.size());
    }
}