/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.form.internal.validation;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.spi.JsonProvider;

import org.apache.johnzon.jsonschema.JsonSchemaValidator;
import org.apache.johnzon.jsonschema.ValidationResult.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.form.internal.converter.PropertyContext;
import org.talend.sdk.component.form.internal.converter.PropertyContext.Configuration;
import org.talend.sdk.component.form.internal.converter.impl.JsonSchemaConverter;
import org.talend.sdk.component.form.internal.lang.CompletionStages;
import org.talend.sdk.component.form.model.jsonschema.JsonSchema;
import org.talend.sdk.component.server.front.model.ConfigTypeNode;
import org.talend.sdk.component.server.front.model.ConfigTypeNodes;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;

/**
 * This class mimics ValidationServiceImpl in projects using component-form for validating configurations
 */
public class ValidationServiceTest {

    private Jsonb jsonb = JsonbBuilder.create();

    private final JsonProvider jsonp = JsonProvider.provider();

    private final JsonBuilderFactory jsonFactory = jsonp.createBuilderFactory(emptyMap());


    final JsonObject conf = jsonFactory
            .createObjectBuilder()
            .add("dataStoreOk",
                    jsonFactory
                            .createObjectBuilder()
                            .add("username", "test")
                            .add("password", "test")
                            .add("jdbcUrl", jsonFactory.createObjectBuilder().add("setRawUrl", true).build())
                            .build())
            .add("dataStore",
                    jsonFactory
                            .createObjectBuilder()
                            .add("username", "test")
                            .add("password", "test")
                            .add("jdbcUrl",
                                    jsonFactory
                                            .createObjectBuilder()
                                            .add("setRawUrl", false)
                                            .add("rawUrl", JsonValue.NULL)
                                            .build())
                            .build())

            .build();

    private final JsonSchemaValidatorFactoryExt factory = new JsonSchemaValidatorFactoryExt();

    private ConfigTypeNode node;

    private ConfigTypeNode node2;//ComponentDetail

    public CompletionStage<Collection<ValidationError>> validate(final ConfigTypeNode config,
            final JsonObject properties) {
        return getValidator(config, properties)
                .thenApply(validator -> validator.apply(properties))
                .thenApply(vr -> vr
                        .getErrors()
                        .stream()
                        .map(e -> new ValidationError(e.getField(), e.getMessage()))
                        .collect(toList()));
    }

    public CompletionStage<JsonSchemaValidator> getValidator(final ConfigTypeNode config , final JsonObject jsonObject) {
        final JsonSchema jsonSchema = new JsonSchema();
        final JsonSchemaConverter converter = new JsonSchemaConverter(jsonb, jsonSchema, config.getProperties());

        findActiveIf(config, jsonObject);

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
                .thenApply(r -> jsonSchema)
                .thenApply(schema -> jsonb.fromJson(jsonb.toJson(schema), JsonObject.class))
                .thenApply(factory::newInstance);
    }

    private void findActiveIf(final ConfigTypeNode config, JsonObject properties) {
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

    private boolean isNotActive(SimplePropertyDefinition simplePropertyDefinition, JsonObject properties) {
        String[] path = simplePropertyDefinition.getMetadata().get("condition::if::target").split("\\.");
        AtomicBoolean isNotActive = new AtomicBoolean(false);
        AtomicReference<JsonObject> object = new AtomicReference<>(properties);
        Arrays.stream(path).forEach(onePath -> {
            if (object.get().get(onePath) != null) {
                if(object.get().get(onePath).getValueType().equals(JsonValue.ValueType.OBJECT)) {
                    object.set(object.get().get(onePath).asJsonObject());
                } else {
                    isNotActive.set(object.get().getBoolean(onePath));//getValueType().equals(JsonValue.ValueType.TRUE));
                }
            }
        });
        return !isNotActive.get();
    }

    private ConfigTypeNode getConfigTypeNode(final String source, final String node) throws IOException {
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(source)) {
            return jsonb.fromJson(stream, ConfigTypeNodes.class).getNodes().get(node);
        }

    }

    @BeforeEach
    void setup() throws Exception {
        node = getConfigTypeNode("config.json", "U2VydmljZU5vdyNkYXRhc2V0I3RhYmxl");
    }

    @Test
    void testValidationOk() throws Exception {
        final Collection<ValidationError> errors =
                validate(node, Json.createObjectBuilder().build()).toCompletableFuture().get();
        assertEquals(0, errors.size());
    }

    @Test
    void testTCOMP2260() throws Exception {
        node2 = getConfigTypeNode("config-activeIf.json", "U2VydmljZU5vdyNkYXRhc2V0I3RhYmxl");
        final Collection<ValidationError> errors =
                validate(node2, conf).toCompletableFuture().get();
        assertEquals(0, errors.size());
    }

}
