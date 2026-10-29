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

abstract class BaseNumberValidation extends BaseValidation {

    protected final double bound;

    private final String name;

    BaseNumberValidation(final String name, final String pointer, final UnaryOperator<JsonValue> extractor,
            final double bound) {
        super(pointer, extractor, JsonValue.ValueType.NUMBER);
        this.name = name;
        this.bound = bound;
    }

    @FunctionalInterface
    interface Factory {

        BaseNumberValidation create(String pointer, UnaryOperator<JsonValue> extractor, double bound);
    }

    static Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>> create(
            final ValidationContext model, final String keyword, final Factory factory) {
        if (!"number".equals(model.getSchema().getString("type", "object"))) {
            return Optional.empty();
        }
        return Optional.ofNullable(model.getSchema().get(keyword))
                .filter(v -> v
                        .getValueType() == JsonValue.ValueType.NUMBER).<Function<JsonValue, Stream<ValidationResult
                                .ValidationError>>> map(
                                        m -> factory.create(model.toPointer(), model.getValueProvider(),
                                                JsonNumber.class.cast(m).doubleValue()));
    }

    @Override
    public String toString() {
        return name + "{bound=" + bound + ", pointer='" + pointer + "'}";
    }

    @Override
    protected Stream<ValidationResult.ValidationError> onNumber(final JsonNumber number) {
        final double val = number.doubleValue();
        if (isValid(val)) {
            return Stream.empty();
        }
        return toError(val);
    }

    protected abstract boolean isValid(double val);

    protected abstract Stream<ValidationResult.ValidationError> toError(double val);
}
