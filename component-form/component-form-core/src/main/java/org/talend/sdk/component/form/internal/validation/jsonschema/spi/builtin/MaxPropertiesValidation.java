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
import java.util.stream.Stream;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import org.talend.sdk.component.form.internal.validation.jsonschema.ValidationResult;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationContext;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationExtension;

public class MaxPropertiesValidation implements ValidationExtension {

    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>>
            create(final ValidationContext model) {
        return Optional.ofNullable(model.getSchema().get("maxProperties"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.NUMBER)
                .map(it -> JsonNumber.class.cast(it).intValue())
                .filter(it -> it >= 0)
                .map(max -> new Impl(model.toPointer(), model.getValueProvider(), max));
    }

    private static class Impl extends BaseValidation {

        private final int bound;

        private Impl(final String pointer,
                final Function<JsonValue, JsonValue> extractor,
                final int bound) {
            super(pointer, extractor, JsonValue.ValueType.OBJECT);
            this.bound = bound;
        }

        @Override
        protected Stream<ValidationResult.ValidationError> onObject(final JsonObject object) {
            if (object.size() > bound) {
                return Stream
                        .of(new ValidationResult.ValidationError(pointer, "Too much properties (> " + bound + ")"));
            }
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "MaxProperties{" +
                    "max=" + bound +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
