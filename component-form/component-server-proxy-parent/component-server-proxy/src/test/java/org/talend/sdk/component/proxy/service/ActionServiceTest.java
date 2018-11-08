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
package org.talend.sdk.component.proxy.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.proxy.api.persistence.OnPersist;
import org.talend.sdk.component.proxy.service.client.UiSpecContext;
import org.talend.sdk.component.proxy.service.qualifier.UiSpecProxy;
import org.talend.sdk.component.proxy.test.CdiInject;
import org.talend.sdk.component.proxy.test.WithServer;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

@CdiInject
@WithServer
class ActionServiceTest {

    @Inject
    private ActionService service;

    @Inject
    @UiSpecProxy
    private Jsonb jsonb;

    @Inject
    private Event<OnPersist> persistEvent;

    @Test
    void http() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/foo").setHandler(httpExchange -> {
            final Headers headers = httpExchange.getRequestHeaders();
            assertEquals("identity", headers.getFirst("Cookie"));
            final byte[] bytes = ("[{\"id\":\"5b164fa72fdfdsfds6564\",\"name\": \"1-Doc\"},"
                    + "{\"id\":\"541564d564sd5scddf\",\"name\":\"2-Beam\"}]").getBytes(StandardCharsets.UTF_8);
            httpExchange.sendResponseHeaders(200, bytes.length);
            httpExchange.getResponseBody().write(bytes);
            httpExchange.close();
        });
        server.start();
        try {
            final Map<String, Object> result = service
                    .findBuiltInAction("builtin::http::dynamic_values(url=${remoteHttpService}/foo,headers=cookie)",
                            new UiSpecContext("en", key -> {
                                if (key.equalsIgnoreCase("remoteHttpService")) {
                                    return "http://localhost:" + server.getAddress().getPort();
                                }
                                if (key.equalsIgnoreCase("cookie")) {
                                    return "identity";
                                }
                                throw new IllegalStateException("Unexpected key: " + key);
                            }), emptyMap())
                    .toCompletableFuture()
                    .get();
            assertEquals(singletonMap("items", asList(new HashMap<String, Object>() {

                {
                    put("id", "5b164fa72fdfdsfds6564");
                    put("label", "1-Doc");
                }
            }, new HashMap<String, Object>() {

                {
                    put("id", "541564d564sd5scddf");
                    put("label", "2-Beam");
                }
            })), result);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void references() throws Exception {
        final Map<String, Object> result = service
                .findBuiltInAction("builtin::references(family=TheTestFamily,type=thetype,name=thename)",
                        new UiSpecContext("en", null), emptyMap())
                .toCompletableFuture()
                .get();
        assertEquals(singletonMap("items", asList(new HashMap<String, Object>() {

            {
                put("id", "thetype1");
                put("label", "thename1");
            }
        }, new HashMap<String, Object>() {

            {
                put("id", "thetype2");
                put("label", "thename2");
            }
        })), result);
    }

    @Test
    void reloadFromParentId() throws Exception {
        final Map<String, Object> result = service
                .findBuiltInAction("builtin::root::reloadFromParentEntityId", new UiSpecContext("en", k -> null),
                        singletonMap("id", "actionServices.reloadFromParentId"))
                .toCompletableFuture()
                .get();
        final ActionService.NewForm form = jsonb.fromJson(jsonb.toJson(result), ActionService.NewForm.class);
        assertNotNull(form);
        assertNotNull(form.getJsonSchema());
        assertNotNull(form.getUiSchema());
        assertNotNull(form.getProperties());
        assertNotNull(form.getMetadata());
        assertEquals("dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE", form.getMetadata().getId());
        assertEquals("dataset-1", form.getJsonSchema().getTitle());
        assertEquals(3, form.getJsonSchema().getProperties().size()); // testConfig, config, $datasetMetadata
        assertEquals(3, form.getUiSchema().size());
        assertJson(
                "{\"testConfig\":{},\"$datasetMetadata\":{},"
                        + "\"$formId\":\"dGVzdC1jb21wb25lbnQjVGhlVGVzdEZhbWlseTIjZGF0YXNldCNkYXRhc2V0LTE\","
                        + "\"configuration\":{\"limit\":0.0,\"connection\":{\"$selfReference\":\"actionServices"
                        + ".reloadFromParentId\",\"url\":\"http://foo\",\"$selfReferenceType\":\"dataset\"}}}",
                form.getProperties().toString());
    }

    @Test
    void createStage() throws Exception {
        Map<String, Object> result = service
                .createStage("TheTestFamily2", "suggestions", "suggestions-values", new UiSpecContext("en", k -> null),
                        new HashMap<String, Object>() {

                            {
                                put("connection.$selfReference", "connectionIdFromPersistence");
                                put("$formId",
                                        Base64
                                                .getUrlEncoder()
                                                .withoutPadding()
                                                .encodeToString("test-component#TheTestFamily2#datastore#Connection-1"
                                                        .getBytes(StandardCharsets.UTF_8)));
                            }
                        })
                .toCompletableFuture()
                .get();
        result.remove("cacheable");
        assertEquals(singletonMap("items", asList(new HashMap<String, Object>() {

            {
                put("id", "1");
                put("label", "value1");
            }
        }, new HashMap<String, Object>() {

            {
                put("id", "2");
                put("label", "value2");
            }
        })), result);
    }

    @Test
    void multiDataset() throws Exception {
        final Map<String, Object> result = service
                .findBuiltInAction("builtin::root::reloadFromParentEntityId", new UiSpecContext("en", k -> null),
                        singletonMap("id", "actionServices.multiDataset"))
                .toCompletableFuture()
                .get();
        final ActionService.NewForm form = jsonb.fromJson(jsonb.toJson(result), ActionService.NewForm.class);
        assertNotNull(form);
        assertNotNull(form.getJsonSchema());
        assertNotNull(form.getUiSchema());
        assertNotNull(form.getProperties());
        assertNotNull(form.getMetadata());
        assertNull(form.getMetadata().getId());
        assertEquals("MultiDataset-One", form.getJsonSchema().getTitle());
        assertEquals(3, form.getJsonSchema().getProperties().size()); // restConfig, $datasetMetadata, configuration
        assertEquals(3, form.getUiSchema().size());

        final UiSchema childrenType = form
                .getUiSchema()
                .stream()
                .filter(it -> "$datasetMetadata".equals(it.getKey()))
                .flatMap(it -> it.getItems().stream())
                .filter(uiSchema -> uiSchema.getKey().equals("$datasetMetadata.childrenType"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No childrenType UI schema found"));
        assertEquals("datalist", childrenType.getWidget());
        assertEquals(2, childrenType.getTitleMap().size());
        List<UiSchema.NameValue> values = new ArrayList<>(childrenType.getTitleMap());
        assertEquals("MultiDataset-One", values.get(0).getName());
        assertEquals("dGVzdC1jb21wb25lbnQjTXVsdGlEYXRhc2V0RmFtaWx5I2RhdGFzZXQjTXVsdGlEYXRhc2V0LU9uZQ",
                values.get(0).getValue());
        assertEquals("MultiDataset-Two", values.get(1).getName());
        assertEquals("dGVzdC1jb21wb25lbnQjTXVsdGlEYXRhc2V0RmFtaWx5I2RhdGFzZXQjTXVsdGlEYXRhc2V0LVR3bw",
                values.get(1).getValue());
    }

    @Test
    void reloadFromParentIdAndType() throws Exception {
        final Map<String, Object> parameters = new HashMap<>();
        parameters
                .put("$datasetMetadata.childrenType",
                        "dGVzdC1jb21wb25lbnQjTXVsdGlEYXRhc2V0RmFtaWx5I2RhdGFzZXQjTXVsdGlEYXRhc2V0LVR3bw");
        // we select MultiDataset-Two (see unit test above)
        final Map<String, Object> result = service
                .findBuiltInAction(
                        "builtin::root::reloadFromParentEntityIdAndType(" + "type=dataset,"
                                + "parentId=actionServices.multiDataset)",
                        new UiSpecContext("en", k -> null), parameters)
                .toCompletableFuture()
                .get();
        final ActionService.NewForm form = jsonb.fromJson(jsonb.toJson(result), ActionService.NewForm.class);
        assertNotNull(form);
        assertNotNull(form.getJsonSchema());
        assertNotNull(form.getUiSchema());
        assertNotNull(form.getProperties());
        assertNotNull(form.getMetadata());
        assertEquals("dGVzdC1jb21wb25lbnQjTXVsdGlEYXRhc2V0RmFtaWx5I2RhdGFzZXQjTXVsdGlEYXRhc2V0LVR3bw",
                form.getMetadata().getId());
        assertEquals("MultiDataset-Two", form.getJsonSchema().getTitle());
        assertEquals(3, form.getJsonSchema().getProperties().size()); // testConfig, config, $datasetMetadata
        assertEquals(3, form.getUiSchema().size());

        // it is important to ensure we have 1. the selfReference 2. the enrichment
        assertJson("{\n" + "  \"testConfig\":{\n" + "\n" + "  },\n" + "  \"$datasetMetadata\":{\n"
                + "    \"childrenType\":\"dGVzdC1jb21wb25lbnQjTXVsdGlEYXRhc2V0RmFtaWx5I2RhdGFzZXQjTXVsdGlEYXRhc2V0LVR3bw\"\n"
                + "  },\n"
                + "  \"$formId\":\"dGVzdC1jb21wb25lbnQjTXVsdGlEYXRhc2V0RmFtaWx5I2RhdGFzZXQjTXVsdGlEYXRhc2V0LVR3bw\",\n"
                + "  \"configuration\":{\n" + "    \"connection\":{\n"
                + "      \"$selfReference\":\"actionServices.multiDataset\",\n"
                + "      \"$selfReferenceType\":\"dataset\"\n" + "    }\n" + "  }\n" + "}",
                form.getProperties().toString());
        // this is really the form for MultiDataset-Two
        assertEquals("MultiDataset-Two", form.getMetadata().getName());
    }

    private static void assertJson(final String oldValue, final String newValue) {
        try (final Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true))) {
            final JsonObject oldJson = jsonb.fromJson(oldValue, JsonObject.class);
            final JsonObject newJson = jsonb.fromJson(newValue, JsonObject.class);
            final boolean condition = areEquals(oldJson, newJson);
            if (!condition) { // to have a nice debug view in the IDE in case of failure
                assertEquals(jsonb.toJson(jsonb.fromJson(oldValue, Map.class)),
                        jsonb.toJson(jsonb.fromJson(newValue, Map.class)));
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    private static boolean areEquals(final JsonValue oldValue, final JsonValue newValue) {
        if (!oldValue.getValueType().equals(newValue.getValueType())) {
            return false;
        }
        switch (oldValue.getValueType()) {
        case STRING:
            return JsonString.class.cast(oldValue).getString().equals(JsonString.class.cast(newValue).getString());
        case NUMBER:
            return JsonNumber.class.cast(oldValue).doubleValue() == JsonNumber.class.cast(newValue).doubleValue();
        case OBJECT:
            final JsonObject oldObject = oldValue.asJsonObject();
            final JsonObject newObject = newValue.asJsonObject();
            if (!oldObject.keySet().equals(newObject.keySet())) {
                return false;
            }
            return oldObject
                    .keySet()
                    .stream()
                    .map(key -> areEquals(oldObject.get(key), newObject.get(key)))
                    .reduce(true, (a, b) -> a && b);
        case ARRAY:
            final JsonArray oldArray = oldValue.asJsonArray();
            final JsonArray newArray = newValue.asJsonArray();
            if (oldArray.size() != newArray.size()) {
                return false;
            }
            if (oldArray.isEmpty()) {
                return true;
            }
            final Iterator<JsonValue> oldIt = oldArray.iterator();
            final Iterator<JsonValue> newIt = newArray.iterator();
            while (oldIt.hasNext()) {
                final JsonValue oldItem = oldIt.next();
                final JsonValue newItem = newIt.next();
                if (!areEquals(oldItem, newItem)) {
                    return false;
                }
            }
            return true;
        default:
            // value type check was enough
            return true;
        }
    }
}
