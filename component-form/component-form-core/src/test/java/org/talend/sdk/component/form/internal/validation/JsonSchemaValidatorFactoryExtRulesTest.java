/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.form.internal.validation.jsonschema.JsonSchemaValidator;
import org.talend.sdk.component.form.internal.validation.jsonschema.ValidationResult;

class JsonSchemaValidatorFactoryExtRulesTest {

    private static JsonObject object(final String json) {
        try (final var reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    private static String field(final String fieldSchema) {
        return "{\"type\":\"object\",\"properties\":{\"a\":" + fieldSchema + "}}";
    }

    private static Arguments of(final String name, final String schema, final String value, final int errors) {
        return Arguments.of(name, schema, value, errors);
    }

    static Stream<Arguments> cases() {
        return Stream
                .of(of("enum valid", field("{\"type\":\"string\",\"enum\":[\"x\",\"y\"]}"), "{\"a\":\"x\"}", 0),
                        of("enum other value is tolerated", field("{\"type\":\"string\",\"enum\":[\"x\",\"y\"]}"), "{\"a\":\"z\"}", 0),
                        of("enum null value", field("{\"type\":\"string\",\"enum\":[\"x\",\"y\"]}"), "{\"a\":null}", 1),
                        of("enum missing value", field("{\"type\":\"string\",\"enum\":[\"x\",\"y\"]}"), "{}", 1),
                        of("enum default is valid", field("{\"type\":\"string\",\"enum\":[\"x\"],\"default\":\"x\"}"), "{}", 0),
                        of("enum not an array", field("{\"type\":\"string\",\"enum\":\"x\"}"), "{}", 0),
                        of("type string ok", field("{\"type\":\"string\"}"), "{\"a\":\"s\"}", 0),
                        of("type string ko", field("{\"type\":\"string\"}"), "{\"a\":1}", 1),
                        of("type number", field("{\"type\":\"number\"}"), "{\"a\":1}", 0),
                        of("type boolean", field("{\"type\":\"boolean\"}"), "{\"a\":true}", 0),
                        of("type array", field("{\"type\":\"array\"}"), "{\"a\":[]}", 0),
                        of("type null", field("{\"type\":\"null\"}"), "{\"a\":null}", 0),
                        of("type union ok", field("{\"type\":[\"string\",\"number\"]}"), "{\"a\":1}", 0),
                        of("type union ko", field("{\"type\":[\"string\",\"number\"]}"), "{\"a\":true}", 1),
                        of("type object ko", field("{\"type\":\"object\"}"), "{\"a\":\"s\"}", 1),
                        of("required valued", "{\"type\":\"object\",\"required\":[\"a\"]}", "{\"a\":1}", 0),
                        of("required unvalued", "{\"type\":\"object\",\"required\":[\"a\"]}", "{\"a\":null}", 1),
                        of("required hidden", "{\"type\":\"object\",\"required\":[\"a\"]}", "{}", 0),
                        of("required not strings", "{\"type\":\"object\",\"required\":[1]}", "{}", 0),
                        of("minimum ko", field("{\"type\":\"number\",\"minimum\":5}"), "{\"a\":1}", 1),
                        of("minimum ok", field("{\"type\":\"number\",\"minimum\":5}"), "{\"a\":6}", 0),
                        of("maximum ko", field("{\"type\":\"number\",\"maximum\":5}"), "{\"a\":10}", 1),
                        of("maximum ok", field("{\"type\":\"number\",\"maximum\":5}"), "{\"a\":3}", 0),
                        of("pattern ko", field("{\"type\":\"string\",\"pattern\":\"^a.*\"}"), "{\"a\":\"b\"}", 1),
                        of("pattern ok", field("{\"type\":\"string\",\"pattern\":\"^a.*\"}"), "{\"a\":\"ab\"}", 0),
                        of("pattern not valid in javascript", field("{\"type\":\"string\",\"pattern\":\"(?<=a)b\\\\p{Alpha}\"}"),
                                "{\"a\":\"b\"}", 1));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void validate(final String name, final String schema, final String value, final int expectedErrors) {
        try (final JsonSchemaValidatorFactoryExt factory = new JsonSchemaValidatorFactoryExt();
                final JsonSchemaValidator validator = factory.newInstance(object(schema))) {
            final ValidationResult result = validator.apply(object(value));
            assertEquals(expectedErrors, result.getErrors().size(), () -> name + ": " + result);
        }
    }

    @Test
    void nullRootIsAlwaysValid() {
        try (final JsonSchemaValidatorFactoryExt factory = new JsonSchemaValidatorFactoryExt();
                final JsonSchemaValidator validator = factory
                        .newInstance(object(field("{\"type\":\"string\",\"enum\":[\"x\"],\"minimum\":1}")))) {
            assertTrue(validator.apply(JsonValue.NULL).isSuccess());
        }
    }

    @Test
    void validatorsAreDescribed() {
        try (final JsonSchemaValidatorFactoryExt factory = new JsonSchemaValidatorFactoryExt()) {
            final String description = factory
                    .newInstance(object("{\"type\":\"object\",\"required\":[\"a\"],\"properties\":{\"a\":"
                            + "{\"enum\":[\"x\"],\"type\":\"string\"}}}"))
                    .toString();
            assertTrue(description.contains("Required{"), description);
            assertTrue(description.contains("Enum{"), description);
            assertTrue(description.contains("Type{"), description);
        }
    }
}
