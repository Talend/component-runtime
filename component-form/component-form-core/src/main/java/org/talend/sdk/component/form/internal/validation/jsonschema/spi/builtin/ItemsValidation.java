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

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.json.JsonArray;
import jakarta.json.JsonValue;

import org.talend.sdk.component.form.internal.validation.jsonschema.JsonSchemaValidator;
import org.talend.sdk.component.form.internal.validation.jsonschema.JsonSchemaValidatorFactory;
import org.talend.sdk.component.form.internal.validation.jsonschema.ValidationResult;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationContext;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationExtension;

public class ItemsValidation implements ValidationExtension {

    private final JsonSchemaValidatorFactory factory;

    public ItemsValidation(final JsonSchemaValidatorFactory factory) {
        this.factory = factory;
    }

    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>>
            create(final ValidationContext model) {
        return Optional.ofNullable(model.getSchema().get("items"))
                .map(items -> {
                    switch (items.getValueType()) {
                        case OBJECT:
                            final JsonSchemaValidator objectValidator = factory.newInstance(items.asJsonObject());
                            return new ItemsValidator(model.toPointer(), model.getValueProvider(),
                                    singleton(objectValidator));
                        case ARRAY:
                            return new ItemsValidator(model.toPointer(), model.getValueProvider(),
                                    items.asJsonArray()
                                            .stream()
                                            .filter(it -> it.getValueType() == JsonValue.ValueType.OBJECT)
                                            .map(it -> factory.newInstance(it.asJsonObject()))
                                            .collect(toList()));
                        default:
                            return null;
                    }
                });
    }

    private static class ItemsValidator extends BaseValidation {

        private final Collection<JsonSchemaValidator> objectValidators;

        private ItemsValidator(final String pointer,
                final Function<JsonValue, JsonValue> extractor,
                final Collection<JsonSchemaValidator> objectValidators) {
            super(pointer, extractor, JsonValue.ValueType.ARRAY);
            this.objectValidators = objectValidators;
        }

        @Override
        protected Stream<ValidationResult.ValidationError> onArray(final JsonArray array) {
            Collection<ValidationResult.ValidationError> errors = null;
            for (int i = 0; i < array.size(); i++) {
                final JsonValue value = array.get(i);
                final Collection<ValidationResult.ValidationError> itemErrors = objectValidators.stream()
                        .flatMap(validator -> validator.apply(value).getErrors().stream())
                        .collect(toList());
                if (itemErrors != null && !itemErrors.isEmpty()) {
                    if (errors == null) {
                        errors = new ArrayList<>();
                    }
                    final String suffix = "[" + i + "]";
                    errors.addAll(itemErrors.stream()
                            .map(e -> new ValidationResult.ValidationError(pointer + e.getField() + suffix,
                                    e.getMessage()))
                            .collect(toList()));
                }
            }
            return errors == null ? Stream.empty() : errors.stream();
        }

        @Override
        public String toString() {
            return "Items{" +
                    "validators=" + objectValidators +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
