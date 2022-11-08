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
package org.talend.sdk.component.form.internal.validation.spi.ext;

import static java.util.Optional.ofNullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;
import org.apache.johnzon.jsonschema.ValidationResult.ValidationError;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.builtin.EnumValidation;

public class EnumValidationWithDefaultValue extends EnumValidation {

    @Override
    public Optional<Function<JsonValue, Stream<ValidationError>>> create(final ValidationContext model) {
        return ofNullable(model.getSchema().get("enum"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.ARRAY)
                .map(JsonValue::asJsonArray)
                .map(values -> new Impl(values, model.getValueProvider(), model.toPointer(),
                        model.getSchema().get("default")));
    }

    private class Impl extends BaseValidation {

        private final Collection<JsonValue> valid;

        private final JsonValue defaultValue;

        private Impl(final Collection<JsonValue> valid, final Function<JsonValue, JsonValue> extractor,
                final String pointer, final JsonValue defaultValue) {
            super(pointer, extractor, JsonValue.ValueType.OBJECT);
            this.valid = valid;
            this.defaultValue = defaultValue;
        }

        @Override
        public Stream<ValidationResult.ValidationError> apply(final JsonValue root) {
            if (isNull(root)) {
                return Stream.empty();
            }
            final JsonValue value = extractor.apply(root);
            if (valid.contains(value) || valid.contains(defaultValue)) {
                return Stream.empty();
            }
            if (value != null && !JsonValue.NULL.equals(value)) {
                return Stream.empty();
            }

            return Stream
                    .of(new ValidationResult.ValidationError(pointer,
                            "Invalid value, got " + value + ", expected: " + valid));
        }

        @Override
        public String toString() {
            return "Enum{" + "valid=" + valid + ", pointer='" + pointer + '\'' + '}';
        }
    }

}
