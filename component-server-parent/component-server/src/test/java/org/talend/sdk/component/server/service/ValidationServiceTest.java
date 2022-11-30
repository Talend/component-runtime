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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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


    public CompletionStage<Result> validate(final ConfigTypeNode config, final JsonObject properties) throws Exception {
        return getValidator(config)
                .thenApply(validator -> validator.apply(properties))
                .thenApply(vr -> new Result(vr
                        .getErrors()
                        .stream()
                        .map(e -> new ValidationError(e.getField(), e.getMessage()))
                        .collect(toList())));
    }

    public CompletionStage<JsonSchemaValidator> getValidator(final ConfigTypeNode config) throws Exception {

        JsonSchema xschema = new UiSpecService<>(null, jsonb)
                .convert("test", "en", config, null)
                .toCompletableFuture()
                .get().getJsonSchema();
        JsonObject xjson = jsonb.fromJson(jsonb.toJson(xschema), JsonObject.class);
        // xschema should be same as jsonSchema
        final JsonSchema jschema = new JsonSchema();
        final JsonSchemaConverter converter = new JsonSchemaConverter(jsonb, jschema, config.getProperties());
        return CompletableFuture
                .allOf(config
                        .getProperties()
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(p -> p.getName().equals(p.getPath()))
                        .map(it -> new PropertyContext<>(it, this, new Configuration(false)))
                        .map(CompletionStages::toStage)
                        .map(converter::convert)
                        .toArray(CompletableFuture[]::new))
                .thenApply(r -> jschema)
                .thenApply(schema -> jsonb.fromJson(jsonb.toJson(schema), JsonObject.class))
                .thenApply(factory::newInstance);
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
        long ecnt = errors.getErrors().stream()
                .peek(e -> log.warn("[checkAsserts] {}", e))
                .filter(e -> !expected.contains(e))
                .peek(e -> log.error("[checkAsserts]Validation uncaught: {}", e))
                //   .map(e-> fail(e))
                .count();
        expected.stream().forEach(e -> assertTrue(errors.getErrors().contains(e)));
    }

        @Test
        void testValidation () throws Exception {
            final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());
            //
            final List<String> uniques = Arrays.asList("one", "two", "three");
            final List<String> notUniques = Arrays.asList("one", "two", "three", "one");
            //
            ValidationError conn = new ValidationError("/configuration", "connection is required and is not present");
            ValidationError min = new ValidationError("/configuration/limit", "1.0 is less than 100.0");
            ValidationError max = new ValidationError("/configuration/limit", "1000.0 is more than 150.0");
            ValidationError url0 = new ValidationError("/configuration/connection/url0", "\"mailto://toto@titi.org\" doesn't match JavascriptRegex{/^https?://.*/}");
            ValidationError url1Patten = new ValidationError("/configuration/connection/url1", "\"mailto://toto@titi.org\" doesn't match JavascriptRegex{/^https?://.*/}");
            ValidationError url1Required = new ValidationError("/configuration/connection", "url1 is required and is not present");
            ValidationError username = new ValidationError("/configuration/connection", "username is required and is not present");
            ValidationError valueEval = new ValidationError("/configuration/connection/valueEval", "Invalid value, got null, expected: [\"VALUE_1\",\"VALUE_2\",\"VALUE_3\"]");
            ValidationError password = new ValidationError("/configuration/connection", "password is required and is not present");
            ValidationError activedIfs = new ValidationError("/configuration/connection", "activedIfs is required and is not present");
            ValidationError uniqVals = new ValidationError("/configuration/connection/uniqVals", "duplicated items: []");


            JsonObject payload;
            /**
             * connection is required!
             **/
            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connectn", "").build()).build();
            checkAsserts(payload, Arrays.asList(conn, valueEval));
            /**
             * min/max
             **/
            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", JsonValue.NULL)
                            .add("limit", 1))
                    .build();
            checkAsserts(payload, Arrays.asList(min, valueEval, conn));

            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", JsonValue.NULL)
                            .add("limit", 1000))
                    .build();
            checkAsserts(payload, Arrays.asList(max, valueEval, conn));
            /*
             * url0 pattern
             */
            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", factory.createObjectBuilder()
                                    .add("url0", "mailto://toto@titi.org").build())
                            .build()).build();
            checkAsserts(payload, Arrays.asList(url0, valueEval, username, password, url1Required, activedIfs));
            /*
             * url1
             */
            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", factory.createObjectBuilder()
                                    .add("url1", "mailto://toto@titi.org").build())
                            .build()).build();
            checkAsserts(payload, Arrays.asList(valueEval, username, url1Patten, password, activedIfs));
            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", factory.createObjectBuilder()
                                    .add("url101", "mailto://toto@titi.org").build())
                            .build()).build();
            checkAsserts(payload, Arrays.asList(valueEval, username, url1Required, password, activedIfs));

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
            checkAsserts(payload, Arrays.asList(valueEval, username, url1Required, password, activedIfs));

            /*
             * password : @ActiveIf(target = "username", evaluationStrategy = ActiveIf.EvaluationStrategy.CONTAINS, value = "undx")
             */

            /*
             *   valueEval     @ActiveIf(target = "checkbox1", value = "true")
             *
             *  TODO Fails
             *
             */


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

            checkAsserts(payload, Arrays.asList(uniqVals, valueEval, username, password, activedIfs, url1Required));

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

            checkAsserts(payload, Arrays.asList(valueEval, username, password, activedIfs, url1Required));
        }


    }
