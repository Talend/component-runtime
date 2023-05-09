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
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.johnzon.jsonschema.JsonSchemaValidator;
import org.apache.johnzon.jsonschema.JsonSchemaValidatorFactory;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.UiSpecService;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyContext.Configuration;
import org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter;
import org.talend.sdk.component.form.internal.lang.CompletionStages;
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

    @Inject
    private PropertiesService propertiesService;

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

    public CompletionStage<Result> validate(final ConfigTypeNode config, final JsonObject payload) throws Exception {
        try {
            propertiesService.validate(connection, payload);
        } catch (Exception errors) {
            return CompletableFuture
                    .completedFuture(errors.getMessage())
                    .thenApply(e -> getError(e));
        }
        return null;
    }

    Result getError(final String error) {
        Result result = new Result(new ArrayList<>());
        Arrays.stream(error.split("\n")).forEach(e -> {
            int index = e.indexOf('\'') + 1;
            String property = '/' + e.substring(index, e.indexOf("\'", index)).replace('.', '/');
            result.getErrors().add(new ValidationError(property, e));
            System.err.println("got error of :" + property + ", error : " + e);
        });
        return result;
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
    }

    private void checkAsserts(JsonObject payload, List<ValidationError> expected) throws Exception {
        Result errors = validate(connection, payload).toCompletableFuture().get();

        System.out.println("===");
        long ecnt = errors.getErrors()
                .stream()
                .peek(e -> log.warn("[checkAsserts] {}", e))
                .filter(e -> !expected.contains(e))
                .peek(e -> log.error("[checkAsserts]Validation uncaught: {}", e))
                // .map(e-> fail(e))
                .count();

        expected.stream().forEach(e -> {
            System.out.println("expected: " + e);
            assertTrue(errors.getErrors().contains(e));
        });
    }

    @Test
    void testValidation() throws Exception {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());

        final List<String> uniques = Arrays.asList("one", "two", "three");
        final List<String> notUniques = Arrays.asList("one", "two", "three", "one");
        
        ValidationError conn =
                new ValidationError("/configuration/connection", "- Property 'configuration.connection' is required.");
        ValidationError min = new ValidationError("/configuration/limit",
                "- Property 'configuration.limit' should be > 100, got 99.");
        ValidationError max = new ValidationError("/configuration/limit",
                "- Property 'configuration.limit' should be < 150, got 200.");
        ValidationError url0 = new ValidationError("/configuration/connection/url0",
                "- 'configuration.connection.url0' does not match '^https?://.*'.");
        ValidationError url1Patten = new ValidationError("/configuration/connection/url1",
                "- 'configuration.connection.url1' does not match '^https?://.*'.");
        ValidationError url1Required = new ValidationError("/configuration/connection/url1",
                "- Property 'configuration.connection.url1' is required.");
        ValidationError username = new ValidationError("/configuration/connection/username",
                "- Property 'configuration.connection.username' is required.");
        ValidationError valueEval = new ValidationError("/configuration/connection/valueEval",
                "- Invalid value for Property 'configuration.connection.valueEval' expected: '[VALUE_1, VALUE_2, VALUE_3]', got null.");
        ValidationError password = new ValidationError("/configuration/connection/password",
                "- Property 'configuration.connection.password' is required.");
        ValidationError activedIfs = new ValidationError("/configuration/connection/activedIfs",
                "- Property 'configuration.connection.activedIfs' is required.");
        ValidationError uniqVals = new ValidationError("/configuration/connection/uniqVals", "duplicated items: []");

        JsonObject payload;
        /**
         * connection is required!
         **/
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", "")
                        .build())
                .build();
        checkAsserts(payload, Arrays.asList(conn));
        /**
         * min/max
         **/
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", "talend")
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", "http://t")
                                .add("username", "abc")
                                .add("password", "aaa")
                                .build())
                        .add("limit", 110))
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Required));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", "https://t")
                                .add("url1", "https://t")
                                .add("username", "abc")
                                .add("password", "aaa")
                                .build())
                        .add("limit", 99))
                .build();
        checkAsserts(payload, Arrays.asList(conn, min));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abc")
                                .add("password", "abc")
                                .add("url0", "https://t")
                                .add("url1", "https://t")
                                .build())
                        .add("limit", 200))
                .build();
        checkAsserts(payload, Arrays.asList(conn, max));
        /*
         * url0 pattern
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abc")
                                .add("password", "abc")
                                .add("url0", "mailto://toto@titi.org")
                                .build()))
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Required));

        /*
         * url1
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abc")
                                .add("password", "abc")
                                .add("url1", "mailto://toto@titi.org")
                                .build()))
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Patten));

        /*
         * uniques
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("url0", JsonValue.NULL)
                                .add("url1", JsonValue.NULL)
                                .add("username", "abc")
                                .add("password", JsonValue.NULL)
                                .add("uniqVals", factory.createArrayBuilder(notUniques).build())
                                .add("checkbox1", JsonValue.TRUE)
                                .add("checkbox2", JsonValue.TRUE)
                                .build())
                        .add("limit", 100))
                .build();
        checkAsserts(payload, Arrays.asList(valueEval, conn, url1Required, activedIfs));

        /*
         * password : @ActiveIf(target = "username", evaluationStrategy = ActiveIf.EvaluationStrategy.CONTAINS, value =
         * "undx")
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "undx")
                                .add("password", JsonValue.NULL)
                                .build())
                        .build())
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Required, password));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abcd")
                                .add("password", JsonValue.NULL)
                                .build())
                        .build())
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Required));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "undx")
                                .add("password", "abc")
                                .build())
                        .build())
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Required));

        /*
         * valueEval @ActiveIf(target = "checkbox1", value = "true")
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abcd")
                                .add("checkbox1", JsonValue.TRUE)
                                .add("valueEval", JsonValue.NULL)
                                .build())
                        .build())
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Required, valueEval));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abcd")
                                .add("checkbox1", JsonValue.FALSE)
                                .add("valueEval", JsonValue.NULL)
                                .build())
                        .build())
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Required));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abcd")
                                .add("checkbox1", JsonValue.TRUE)
                                .add("valueEval", "VALUE_2")
                                .build())
                        .build())
                .build();
        checkAsserts(payload, Arrays.asList(conn, url1Required));

    }

}
