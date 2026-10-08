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

import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import org.talend.sdk.component.form.internal.validation.jsonschema.ValidationResult;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationContext;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationExtension;

public class MinLengthValidation implements ValidationExtension {

    @Override
    public Optional<Function<JsonValue, Stream<ValidationResult.ValidationError>>>
            create(final ValidationContext model) {
        return BaseSizeValidation.create(model, "minLength", "string", Impl::new);
    }

    private static class Impl extends BaseSizeValidation {

        private Impl(final String pointer, final UnaryOperator<JsonValue> valueProvider, final int bound) {
            super("MinLength", pointer, valueProvider, JsonValue.ValueType.STRING, bound);
        }

        @Override
        protected Stream<ValidationResult.ValidationError> onString(final JsonString val) {
            return val.getString().length() < bound ? error(val + " length is less than " + bound) : Stream.empty();
        }
    }
}
