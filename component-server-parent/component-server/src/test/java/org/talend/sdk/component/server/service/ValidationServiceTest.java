/*
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package org.talend.sdk.component.server.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.johnzon.jsonschema.JsonSchemaValidatorFactory;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.internal.converter.PropertyContext.Configuration;
import org.talend.sdk.component.form.internal.validation.JsonSchemaValidatorFactoryExt;
import org.talend.sdk.component.form.model.Ui;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.form.model.uischema.UiSchema;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.test.ComponentClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@MonoMeecrowaveConfig
public class ValidationServiceTest {
    @Inject
    private WebTarget base;

    @Inject
    private ComponentClient client;

    @Inject
    private PropertiesService propertiesService;

    private Jsonb jsonb = JsonbBuilder.create();

    final String lang = "en";
    final String family = "custom";
    final String component = "noop";
    ConfigTypeNodes details;
    ConfigTypeNode connection;
    JsonSchema jsonSchema;
    private Collection<UiSchema> uiSchemas;

    private final UiSpecService<Object> uiSpecService = new UiSpecService<>(new Client() {

        @Override // for dynamic_values, just return an empty schema
        public CompletionStage<Map<String, Object>> action(final String family, final String type,
                                                           final String action, final String lang, final Map params, final Object context) {
            final Map<String, Object> result = new HashMap<>();
            result.put("items", emptyList());
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public void close() {
            // no-op
        }
    });

    private final JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactoryExt();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class Result {

        private Collection<ValidationError> errors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ValidationError {

        private String field;

        private String message;

    }

    @BeforeEach
    void setup() throws Exception {
        details = base
                .path("configurationtype/details")
                .queryParam("identifiers", "Y29tcG9uZW50LXdpdGgtdXNlci1qYXJzI2N1c3RvbSNkYXRhc2V0I2RhdGFzZXQ")
                .request(APPLICATION_JSON_TYPE)
                .header("Accept-Encoding", "gzip")
                .get(ConfigTypeNodes.class);
        connection = details.getNodes().values().iterator().next();
        uiSpecService.setConfiguration(new Configuration(true));
        Ui ui = uiSpecService.convert(family, lang, connection, null).toCompletableFuture().get();
        jsonSchema = ui.getJsonSchema();
        uiSchemas = ui.getUiSchema();
        int i = 0;
    }

    private void checkAsserts(JsonObject payload, List<String> expected) throws Exception {
        try {
            propertiesService.validate(connection, payload);
            if (expected != null && expected.size() > 0) {
                fail("There should be errors: " + expected);
            }
        } catch (Exception errors) {
            StringBuffer expectedBuffer = new StringBuffer();
            expected.stream().forEach(e -> {
                expectedBuffer.append(e).append("\n");
            });
            expectedBuffer.delete(expectedBuffer.lastIndexOf("\n"), expectedBuffer.length());
            assertEquals(expectedBuffer.toString(), errors.getMessage());
        }
    }

    @Test
    void testValidation() throws Exception {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());
        //
        final List<String> uniques = Arrays.asList("one", "two", "three");
        final List<String> notUniques = Arrays.asList("one", "two", "three", "one");
        //
        String activeIfsError = "- Property 'configuration.connection.activedIfs' is required.";
        String connectionError = "- Property 'configuration.connection' is required.";
        String url1Required = "- Property 'configuration.connection.url1' is required.";
        String url0Error = "- 'configuration.connection.url0' does not match '^https?://.*'.";
        String url1Error = "- 'configuration.connection.url1' does not match '^https?://.*'.";
        String usernameError = "- Property 'configuration.connection.username' is required.";
        String minError = "- Property 'configuration.limit' should be > 100, got 1.";
        String maxError = "- Property 'configuration.limit' should be < 150, got 1,000.";
        String ValueEvalError = "- Invalid value for Property 'configuration.connection.valueEval' expected: '[VALUE_1, VALUE_2, VALUE_3]', got null.";

        JsonObject payload;
        /**
         * connection is required!
         **/
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connectn", "").build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Required, usernameError));
        /**
         * min/max
         **/
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", JsonValue.NULL)
                        .add("limit", 1))
                .build();
        checkAsserts(payload, Arrays.asList(connectionError, minError, url1Required, usernameError));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", JsonValue.NULL)
                        .add("limit", 1000))
                .build();
        checkAsserts(payload, Arrays.asList(connectionError, maxError, url1Required, usernameError));
        /*
         * url0 pattern
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", "mailto://toto@titi.org").build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url0Error, url1Required, usernameError));
        /*
         * url1
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url1", "mailto://toto@titi.org").build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Error, usernameError));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url101", "mailto://toto@titi.org").build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Required, usernameError));

        /*
         * uniques
         */
        final JsonObject try3 = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url", "http://t")
                                .add("username", JsonValue.NULL)
                                .add("uniqVals", factory.createArrayBuilder(notUniques).build())
                                .build())
                        .add("limit", 100))
                .build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Required, usernameError));

        /*
         * uniqVals
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", JsonValue.NULL)
                                .add("url1", JsonValue.NULL)
                                .add("username", JsonValue.NULL)
                                .add("password", JsonValue.NULL)
                                .add("uniqVals", factory.createArrayBuilder(notUniques).build())
                                .add("checkbox1", JsonValue.TRUE)
                                .add("checkbox2", JsonValue.TRUE)
                                .build())
                        .add("limit", 100))
                .build();

        checkAsserts(payload, Arrays.asList(activeIfsError, connectionError, url1Required, usernameError, ValueEvalError));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", JsonValue.NULL)
                                .add("url1", JsonValue.NULL)
                                .add("username", JsonValue.NULL)
                                .add("password", JsonValue.NULL)
                                .add("uniqVals", factory.createArrayBuilder(uniques).build())
                                .add("checkbox1", JsonValue.TRUE)
                                .add("checkbox2", JsonValue.TRUE)
                                .build())
                        .add("limit", 100))
                .build();

        checkAsserts(payload, Arrays.asList(activeIfsError, connectionError, url1Required, usernameError, ValueEvalError));
    }

    @Test
    void testValidation_activeIf() throws Exception {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());
        //
        String activeIfsError = "- Property 'configuration.connection.activedIfs' is required.";
        String connectionError = "- Property 'configuration.connection' is required.";
        String passwordError = "- Property 'configuration.connection.password' is required.";
        String url1Required = "- Property 'configuration.connection.url1' is required.";

        JsonObject payload;

        /*
         * password : @ActiveIf(target = "username", evaluationStrategy = ActiveIf.EvaluationStrategy.CONTAINS, value = "undx")
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "undx")
                                .add("password", JsonValue.NULL).build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, passwordError, url1Required));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abcd")
                                .add("password", JsonValue.NULL).build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Required));


        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "undx")
                                .add("password", "abc").build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Required));
    }

    @Test
    void testValidation_activeIf_2() throws Exception {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());
        //
        String activeIfsError = "- Property 'configuration.connection.activedIfs' is required.";
        String connectionError = "- Property 'configuration.connection' is required.";
        String usernameError = "- Property 'configuration.connection.username' is required.";
        String url1Required = "- Property 'configuration.connection.url1' is required.";
        String ValueEvalError = "- Invalid value for Property 'configuration.connection.valueEval' expected: '[VALUE_1, VALUE_2, VALUE_3]', got null.";

        JsonObject payload;
        /*
         *   valueEval     @ActiveIf(target = "checkbox1", value = "true")
         *
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.TRUE)
                                .add("valueEval", JsonValue.NULL).build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Required, usernameError, ValueEvalError));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.FALSE)
                                .add("valueEval", JsonValue.NULL).build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Required, usernameError));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.TRUE)
                                .add("valueEval", "VALUE_2").build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(connectionError, url1Required, usernameError));

    }


}
