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
import static java.util.Optional.ofNullable;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
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
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
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
        findActiveIf(config, properties);

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

    private void findActiveIf(final ConfigTypeNode config, JsonObject properties) throws NoSuchMethodException {
        extracted(config.getProperties().get(0));
        extracted(config.getProperties().get(1));
        //TCOMP-2260: find out all activeIf's target , and its value in jsonObject; if it is not active, clear its validation
        config
                .getProperties()
                .stream()
                .filter(p -> p.getMetadata().get("condition::if::target") != null)
                .forEach(simplePropertyDefinition -> {
                    if (isNotActive(simplePropertyDefinition, properties))
                        simplePropertyDefinition.setValidation(null);
                });
    }

    private static void extracted(final SimplePropertyDefinition properties) throws NoSuchMethodException {
        final ParameterModelService service = new ParameterModelService(new PropertyEditorRegistry());
//        final List<ParameterMeta> metas = service
//                .buildParameterMetas(SimplePropertyDefinition.class.getMethod("getMetadata"),
//                        "", new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "server")));
        final List<ParameterMeta> metas2 = service.buildParameterMetas(Stream.of(new ParameterModelService.Param(SimplePropertyDefinition.class, SimplePropertyDefinition.class.getAnnotations(), properties.getName())),
                        SimplePropertyDefinition.class,
                    ofNullable(SimplePropertyDefinition.class.getPackage()).map(Package::getName).orElse(""), true,
                        new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "server-model")));
        System.err.println("size: " + metas2.size());
    }

    //should own the same first parent.
    // 1) A.B.C depends on A.B.D, A.B -> A.C :support
    // 2) A.B.C depends on A.D.E : TO  check
    private boolean isNotActive(final SimplePropertyDefinition simplePropertyDefinition, final JsonObject properties) {
        String[] targetPath = simplePropertyDefinition.getMetadata().get("condition::if::target").split("\\.");
        String[] selfPath = simplePropertyDefinition.getPath().split("\\.");
        String targetValue = simplePropertyDefinition.getMetadata().get("condition::if::value");

        AtomicBoolean isNotActive = new AtomicBoolean(false);
        AtomicReference<JsonObject> object = new AtomicReference<>(properties);
        Arrays.stream(selfPath).forEach( onePath -> {
            Arrays.stream(targetPath).forEach( targetP -> {
                if (object.get().get(onePath) != null) {
                    if(object.get().get(onePath).getValueType().equals(JsonValue.ValueType.OBJECT)) {
                        object.set(object.get().get(onePath).asJsonObject());
                    } else {
                        if (object.get().get(targetP) != null) {
                            String str = object.get().get(targetP).toString();
                            isNotActive.set(str.contains(targetValue));
                        }
                    }
                }
            });
        });

        return !isNotActive.get();
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

    @Test
    public void test1(){
        final ParameterModelService service = new ParameterModelService(new PropertyEditorRegistry());
            final List<ParameterMeta> metas2 = service.buildParameterMetas(Stream.of(new ParameterModelService.Param(JsonSchema.class,
                        JsonSchema.class.getAnnotations(), jsonSchema.getTitle())),
                JsonSchema.class,
                ofNullable(JsonSchema.class.getPackage()).map(Package::getName).orElse(""), true,
                new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "server-model")));
            System.err.println("size: " + metas2.size());
    }
    private void checkAsserts(JsonObject payload, List<ValidationError> expected) throws Exception {
        Result errors = validate(connection, payload).toCompletableFuture().get();

        System.err.println(" ====== "+ expected.size());
        long ecnt = errors.getErrors().stream()
                .peek(e -> log.warn("[checkAsserts] {}", e))
                .filter(e -> !expected.contains(e))
                .peek(e -> log.error("[checkAsserts]Validation uncaught: {}", e))
                //   .map(e-> fail(e))
                .count();
//        System.out.println("Actual errors: "+ errors.getErrors().size() + ", uncaught errors: " + ecnt);
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
            checkAsserts(payload, Arrays.asList(conn));
            /**
             * min/max
             **/
            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", JsonValue.NULL)
                            .add("limit", 1))
                    .build();
            checkAsserts(payload, Arrays.asList(min, conn));

            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", JsonValue.NULL)
                            .add("limit", 1000))
                    .build();
            checkAsserts(payload, Arrays.asList(max, conn));
            /*
             * url0 pattern
             */
            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", factory.createObjectBuilder()
                                    .add("url0", "mailto://toto@titi.org").build())
                            .build()).build();
            checkAsserts(payload, Arrays.asList(url0, username, url1Required, activedIfs));
            /*
             * url1
             */
            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", factory.createObjectBuilder()
                                    .add("url1", "mailto://toto@titi.org").build())
                            .build()).build();
            checkAsserts(payload, Arrays.asList(username, url1Patten, activedIfs));

            payload = factory.createObjectBuilder()
                    .add("configuration", factory.createObjectBuilder()
                            .add("connection", factory.createObjectBuilder()
                                    .add("url101", "mailto://toto@titi.org").build())
                            .build()).build();
            checkAsserts(payload, Arrays.asList(username, url1Required, activedIfs));

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
            checkAsserts(payload, Arrays.asList(username, url1Required, activedIfs));

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

            checkAsserts(payload, Arrays.asList(uniqVals, username, activedIfs, url1Required));

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

            checkAsserts(payload, Arrays.asList(username, activedIfs, url1Required));
        }

    @Test
    void testValidation_activeIf () throws Exception {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());
        //
        ValidationError url1Required = new ValidationError("/configuration/connection", "url1 is required and is not present");
        ValidationError valueEval = new ValidationError("/configuration/connection/valueEval", "Invalid value, got null, expected: [\"VALUE_1\",\"VALUE_2\",\"VALUE_3\"]");
        ValidationError password = new ValidationError("/configuration/connection", "password is required and is not present");
        ValidationError activedIfs = new ValidationError("/configuration/connection", "activedIfs is required and is not present");

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
        checkAsserts(payload, Arrays.asList(url1Required, activedIfs, password));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "abcd")
                                .add("password", JsonValue.NULL).build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(url1Required, activedIfs));


        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("username", "undx")
                                .add("password", "abc").build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(url1Required, activedIfs));
    }

    @Test
    void testValidation_activeIf_2 () throws Exception {
        final JsonBuilderFactory factory = Json.createBuilderFactory(emptyMap());
        //
        ValidationError url1Required = new ValidationError("/configuration/connection", "url1 is required and is not present");
        ValidationError valueEval = new ValidationError("/configuration/connection/valueEval", "Invalid value, got null, expected: [\"VALUE_1\",\"VALUE_2\",\"VALUE_3\"]");
        ValidationError password = new ValidationError("/configuration/connection", "password is required and is not present");
        ValidationError activedIfs = new ValidationError("/configuration/connection", "activedIfs is required and is not present");

        JsonObject payload;


        /*
         *   valueEval     @ActiveIf(target = "checkbox1", value = "true")
         *
         *  TODO Fails
         *
         */
        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.TRUE)
                                .add("valueEval", JsonValue.NULL).build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(url1Required, activedIfs, valueEval));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.FALSE)
                                .add("valueEval", JsonValue.NULL).build())
                        .build()).build();
        checkAsserts(payload, Arrays.asList(url1Required, activedIfs));

        payload = factory.createObjectBuilder()
                .add("configuration", factory.createObjectBuilder()
                        .add("connection", factory.createObjectBuilder()
                                .add("checkbox1", JsonValue.TRUE)
                                .add("valueEval", "VALUE_2").build())
                       .build()).build();
        checkAsserts(payload, Arrays.asList(url1Required, activedIfs));

    }


    }
