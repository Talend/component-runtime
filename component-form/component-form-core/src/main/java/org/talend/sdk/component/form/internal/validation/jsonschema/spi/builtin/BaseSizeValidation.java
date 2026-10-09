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

import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import jakarta.json.JsonNumber;
import jakarta.json.JsonValue;

import org.talend.sdk.component.form.internal.validation.jsonschema.ValidationResult;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationContext;

// shared base of the min/max length, items and properties validations
abstract class BaseSizeValidation extends BaseValidation {

    protected final int bound;

    private final String name;

    BaseSizeValidation(final String name, final String pointer, final UnaryOperator<JsonValue> extractor,
            final JsonValue.ValueType validType, final int bound) {
        super(pointer, extractor, validType);
        this.name = name;
        this.bound = bound;
    }

    @FunctionalInterface
    interface Factory {

        BaseSizeValidation create(String pointer, UnaryOperator<JsonValue> extractor, int bound);
    }

    // requiredSchemaType == null means the keyword applies whatever the schema type is
    static Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(
            final ValidationContext model, final String keyword, final String requiredSchemaType,
            final Factory factory) {
        if (requiredSchemaType != null
                && !requiredSchemaType.equals(model.getSchema().getString("type", "object"))) {
            return Optional.empty();
        }
        return Optional.ofNullable(model.getSchema().get(keyword))
                .filter(it -> it.getValueType() == JsonValue.ValueType.NUMBER)
                .map(it -> JsonNumber.class.cast(it).intValue())
                .filter(it -> it >= 0).<Function<JsonValue, Stream<ValidationResult
                        .ValidationError>>> map(
                                value -> factory.create(model.toPointer(), model.getValueProvider(), value));
    }

    protected Stream<ValidationResult.ValidationError> error(final String message) {
        return Stream.of(new ValidationResult.ValidationError(pointer, message));
    }

    @Override
    public String toString() {
        return name + "{bound=" + bound + ", pointer='" + pointer + "'}";
    }
}
