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
package org.talend.sdk.component.form.internal.validation.jsonschema.spi.builtin;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import jakarta.json.JsonArray;
import jakarta.json.JsonValue;

import org.talend.sdk.component.form.internal.validation.jsonschema.JsonSchemaValidator;
import org.talend.sdk.component.form.internal.validation.jsonschema.JsonSchemaValidatorFactory;
import org.talend.sdk.component.form.internal.validation.jsonschema.ValidationResult;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationContext;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationExtension;

public class ContainsValidation implements ValidationExtension {

    private final JsonSchemaValidatorFactory factory;

    public ContainsValidation(final JsonSchemaValidatorFactory factory) {
        this.factory = factory;
    }

    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>>
            create(final ValidationContext model) {
        return Optional.ofNullable(model.getSchema().get("contains"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.OBJECT)
                .map(it -> new ItemsValidator(model.toPointer(), model.getValueProvider(),
                        factory.newInstance(it.asJsonObject())));
    }

    private static class ItemsValidator extends BaseValidation {

        private final JsonSchemaValidator validator;

        private ItemsValidator(final String pointer,
                final UnaryOperator<JsonValue> extractor,
                final JsonSchemaValidator validator) {
            super(pointer, extractor, JsonValue.ValueType.ARRAY);
            this.validator = validator;
        }

        @Override
        protected Stream<ValidationResult.ValidationError> onArray(final JsonArray array) {
            for (final JsonValue value : array) {
                final Collection<ValidationResult.ValidationError> itemErrors = validator.apply(value).getErrors();
                if (itemErrors.isEmpty()) {
                    return Stream.empty();
                }
            }
            return Stream.of(new ValidationResult.ValidationError(pointer, "No item matching the expected schema"));
        }

        @Override
        public String toString() {
            return "Contains{" +
                    "validator=" + validator +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
