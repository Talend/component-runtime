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
package org.talend.sdk.component.form.internal.validation.jsonschema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class JsonSchemaValidatorFactoryTest {

    private static JsonObject object(final String json) {
        try (final var reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    // each keyword is validated on the "a" property of the root object
    private static String field(final String fieldSchema) {
        return "{\"type\":\"object\",\"properties\":{\"a\":" + fieldSchema + "}}";
    }

    private static Arguments of(final String name, final String schema, final String value, final int errors) {
        return Arguments.of(name, schema, value, errors);
    }

    static Stream<Arguments> cases() {
        return Stream
                .of(of("required missing", "{\"type\":\"object\",\"required\":[\"a\"]}", "{}", 1),
                        of("required present", "{\"type\":\"object\",\"required\":[\"a\"]}", "{\"a\":1}", 0),
                        of("minProperties ko", "{\"type\":\"object\",\"minProperties\":1}", "{}", 1),
                        of("minProperties ok", "{\"type\":\"object\",\"minProperties\":1}", "{\"a\":1}", 0),
                        of("maxProperties ko", "{\"type\":\"object\",\"maxProperties\":1}", "{\"a\":1,\"b\":2}", 1),
                        of("maxProperties ok", "{\"type\":\"object\",\"maxProperties\":1}", "{\"a\":1}", 0),
                        of("type string ko", field("{\"type\":\"string\"}"), "{\"a\":1}", 1),
                        of("type string ok", field("{\"type\":\"string\"}"), "{\"a\":\"x\"}", 0),
                        of("type number ko", field("{\"type\":\"number\"}"), "{\"a\":\"x\"}", 1),
                        of("type boolean ko", field("{\"type\":\"boolean\"}"), "{\"a\":\"x\"}", 1),
                        of("type boolean ok", field("{\"type\":\"boolean\"}"), "{\"a\":true}", 0),
                        of("type array ko", field("{\"type\":\"array\"}"), "{\"a\":1}", 1),
                        of("type array ok", field("{\"type\":\"array\"}"), "{\"a\":[]}", 0),
                        of("type object ko", field("{\"type\":\"object\"}"), "{\"a\":1}", 1),
                        of("type object ok", field("{\"type\":\"object\"}"), "{\"a\":{}}", 0),
                        of("type null ko", field("{\"type\":\"null\"}"), "{\"a\":1}", 1),
                        of("type union ko", field("{\"type\":[\"string\",\"null\"]}"), "{\"a\":1}", 1),
                        of("type union ok", field("{\"type\":[\"string\",\"null\"]}"), "{\"a\":\"x\"}", 0),
                        of("type union null", field("{\"type\":[\"string\",\"null\"]}"), "{\"a\":null}", 0),
                        of("integer ko", field("{\"type\":\"integer\"}"), "{\"a\":1.5}", 1),
                        of("integer ok", field("{\"type\":\"integer\"}"), "{\"a\":2}", 0),
                        of("enum ko", field("{\"type\":\"string\",\"enum\":[\"x\",\"y\"]}"), "{\"a\":\"z\"}", 1),
                        of("enum ok", field("{\"type\":\"string\",\"enum\":[\"x\",\"y\"]}"), "{\"a\":\"x\"}", 0),
                        of("enum nullable", field("{\"type\":\"string\",\"enum\":[\"x\"],\"nullable\":true}"),
                                "{\"a\":null}", 0),
                        of("multipleOf ko", field("{\"type\":\"number\",\"multipleOf\":3}"), "{\"a\":4}", 1),
                        of("multipleOf ok", field("{\"type\":\"number\",\"multipleOf\":3}"), "{\"a\":6}", 0),
                        of("maximum ko", field("{\"type\":\"number\",\"maximum\":5}"), "{\"a\":6}", 1),
                        of("maximum ok", field("{\"type\":\"number\",\"maximum\":5}"), "{\"a\":5}", 0),
                        of("minimum ko", field("{\"type\":\"number\",\"minimum\":5}"), "{\"a\":4}", 1),
                        of("minimum ok", field("{\"type\":\"number\",\"minimum\":5}"), "{\"a\":5}", 0),
                        of("exclusiveMaximum ko", field("{\"type\":\"number\",\"exclusiveMaximum\":5}"), "{\"a\":5}",
                                1),
                        of("exclusiveMaximum ok", field("{\"type\":\"number\",\"exclusiveMaximum\":5}"), "{\"a\":4}",
                                0),
                        of("exclusiveMinimum ko", field("{\"type\":\"number\",\"exclusiveMinimum\":5}"), "{\"a\":5}",
                                1),
                        of("exclusiveMinimum ok", field("{\"type\":\"number\",\"exclusiveMinimum\":5}"), "{\"a\":6}",
                                0),
                        of("maxLength ko", field("{\"type\":\"string\",\"maxLength\":2}"), "{\"a\":\"abc\"}", 1),
                        of("maxLength ok", field("{\"type\":\"string\",\"maxLength\":2}"), "{\"a\":\"ab\"}", 0),
                        of("minLength ko", field("{\"type\":\"string\",\"minLength\":2}"), "{\"a\":\"a\"}", 1),
                        of("minLength ok", field("{\"type\":\"string\",\"minLength\":2}"), "{\"a\":\"ab\"}", 0),
                        of("pattern ko", field("{\"type\":\"string\",\"pattern\":\"^a+$\"}"), "{\"a\":\"b\"}", 1),
                        of("pattern ok", field("{\"type\":\"string\",\"pattern\":\"^a+$\"}"), "{\"a\":\"aa\"}", 0),
                        of("maxItems ko", field("{\"type\":\"array\",\"maxItems\":1}"), "{\"a\":[1,2]}", 1),
                        of("maxItems ok", field("{\"type\":\"array\",\"maxItems\":1}"), "{\"a\":[1]}", 0),
                        of("minItems ko", field("{\"type\":\"array\",\"minItems\":2}"), "{\"a\":[1]}", 1),
                        of("minItems ok", field("{\"type\":\"array\",\"minItems\":2}"), "{\"a\":[1,2]}", 0),
                        of("uniqueItems ko", field("{\"type\":\"array\",\"uniqueItems\":true}"), "{\"a\":[1,1]}", 1),
                        of("uniqueItems ok", field("{\"type\":\"array\",\"uniqueItems\":true}"), "{\"a\":[1,2]}", 0),
                        of("contains ko", field("{\"type\":\"array\",\"contains\":{\"type\":\"string\"}}"),
                                "{\"a\":[1,2]}", 1),
                        of("contains ok", field("{\"type\":\"array\",\"contains\":{\"type\":\"string\"}}"),
                                "{\"a\":[1,\"x\"]}", 0),
                        of("items schema ko",
                                field("{\"type\":\"array\",\"items\":{\"type\":\"number\",\"minimum\":1}}"),
                                "{\"a\":[1,0]}", 1),
                        of("items schema ok",
                                field("{\"type\":\"array\",\"items\":{\"type\":\"number\",\"minimum\":1}}"),
                                "{\"a\":[1,2]}", 0),
                        of("items array of schemas ko",
                                field("{\"type\":\"array\",\"items\":[{\"type\":\"object\",\"required\":[\"k\"]}]}"),
                                "{\"a\":[{\"k\":1},{}]}", 1),
                        of("items unsupported schema kind", field("{\"type\":\"array\",\"items\":true}"),
                                "{\"a\":[1]}", 0),
                        of("patternProperties ko",
                                "{\"type\":\"object\",\"patternProperties\":{\"^x.*\":{\"type\":\"number\"}}}",
                                "{\"xa\":\"s\",\"b\":\"s\"}", 1),
                        of("patternProperties ok",
                                "{\"type\":\"object\",\"patternProperties\":{\"^x.*\":{\"type\":\"number\"}}}",
                                "{\"xa\":1}", 0),
                        of("nested properties",
                                field("{\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"}}}"),
                                "{\"a\":{\"b\":1}}", 1),
                        of("missing optional nested value",
                                field("{\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"}}}"), "{}", 0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void validate(final String name, final String schema, final String value, final int expectedErrors) {
        try (final JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();
                final JsonSchemaValidator validator = factory.newInstance(object(schema))) {
            final ValidationResult result = validator.apply(object(value));
            assertEquals(expectedErrors, result.getErrors().size(), () -> name + ": " + result);
            assertEquals(expectedErrors == 0, result.isSuccess());
        }
    }

    @Test
    void errorMessage() {
        try (final JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory();
                final JsonSchemaValidator validator = factory.newInstance(object(field("{\"type\":\"string\"}")))) {
            final ValidationResult result = validator.apply(object("{\"a\":1}"));
            final ValidationResult.ValidationError error = result.getErrors().iterator().next();
            assertTrue(error.getMessage().startsWith("Expected"), error.getMessage());
        }
    }

    @Test
    void resultAccessors() {
        final ValidationResult result = new ValidationResult();
        assertTrue(result.isSuccess());
        final ValidationResult.ValidationError error = new ValidationResult.ValidationError();
        error.setField("/f");
        error.setMessage("m");
        result.setErrors(List.of(error));
        assertFalse(result.isSuccess());
        assertEquals("/f", error.getField());
        assertEquals("m", error.getMessage());
        assertTrue(result.toString().contains("/f"));
        assertTrue(error.toString().contains("m"));
    }

    @Test
    void invalidTypeIsRejected() {
        try (final JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory()) {
            assertThrows(RuntimeException.class, () -> factory.newInstance(object("{\"type\":1}")));
        }
    }

    @Test
    void extensionsAndRegexFactoryCanBeCustomized() {
        try (final JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory()) {
            factory.setExtensions().appendExtensions(model -> Optional.of(value -> Stream.empty()));
            factory.setRegexFactory(regex -> input -> true);
            final JsonSchemaValidator validator = factory.newInstance(object("{\"type\":\"string\"}"));
            assertTrue(validator.apply(JsonValue.NULL).isSuccess());
            assertTrue(validator.toString().startsWith("JsonSchemaValidator{"));
            assertTrue(factory.createDefaultValidations().size() > 10);
        }
    }

    @Test
    void validatorsAreDescribed() {
        try (final JsonSchemaValidatorFactory factory = new JsonSchemaValidatorFactory()) {
            final String description = factory
                    .newInstance(object(field("{\"type\":\"number\",\"minimum\":1,\"maximum\":2}")))
                    .toString();
            assertTrue(description.contains("Minimum{bound=1.0"), description);
            assertTrue(description.contains("Maximum{bound=2.0"), description);
        }
    }
}
