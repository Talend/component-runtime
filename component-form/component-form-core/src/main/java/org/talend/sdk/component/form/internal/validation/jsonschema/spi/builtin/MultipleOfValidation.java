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
import jakarta.json.JsonValue;

import org.talend.sdk.component.form.internal.validation.jsonschema.ValidationResult;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationContext;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationExtension;

public class MultipleOfValidation implements ValidationExtension {

    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>>
            create(final ValidationContext model) {
        if (model.getSchema().getString("type", "object").equals("number")) {
            return Optional.ofNullable(model.getSchema().get("multipleOf"))
                    .filter(v -> v.getValueType() == JsonValue.ValueType.NUMBER)
                    .map(m -> new Impl(model.toPointer(), model.getValueProvider(),
                            JsonNumber.class.cast(m).doubleValue()));
        }
        return Optional.empty();
    }

    static class Impl extends BaseNumberValidation {

        Impl(final String pointer, final Function<JsonValue, JsonValue> valueProvider, final double multipleOf) {
            super(pointer, valueProvider, multipleOf);
        }

        @Override
        protected boolean isValid(final double val) {
            final double divided = val / bound;
            return divided == (long) divided;
        }

        @Override
        protected Stream<ValidationResult.ValidationError> toError(final double val) {
            return Stream.of(new ValidationResult.ValidationError(pointer, val + " is not a multiple of " + bound));
        }

        @Override
        public String toString() {
            return "MultipleOf{" +
                    "factor=" + bound +
                    ", pointer='" + pointer + '\'' +
                    '}';
        }
    }
}
