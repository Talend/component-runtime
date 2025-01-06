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
package org.talend.sdk.component.form.internal.validation;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

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

/**
 * This class mimics ValidationServiceImpl in projects using component-form for validating configurations
 */
public class ValidationServiceTest {

    private Jsonb jsonb = JsonbBuilder.create();

    private final JsonSchemaValidatorFactoryExt factory = new JsonSchemaValidatorFactoryExt();

    private ConfigTypeNode node;

    public CompletionStage<Collection<ValidationError>> validate(final ConfigTypeNode config,
            final JsonObject properties) {
        return getValidator(config)
                .thenApply(validator -> validator.apply(properties))
                .thenApply(vr -> vr
                        .getErrors()
                        .stream()
                        .map(e -> new ValidationError(e.getField(), e.getMessage()))
                        .collect(toList()));
    }

    public CompletionStage<JsonSchemaValidator> getValidator(final ConfigTypeNode config) {
        final JsonSchema jsonSchema = new JsonSchema();
        final JsonSchemaConverter converter = new JsonSchemaConverter(jsonb, jsonSchema, config.getProperties());
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

}
