/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult.ValidationError;
import org.apache.johnzon.jsonschema.spi.ValidationContext;
import org.apache.johnzon.jsonschema.spi.ValidationExtension;

public class RequiredValidation implements ValidationExtension {

    @Override
    public Optional<Function<JsonValue, Stream<ValidationError>>> create(final ValidationContext model) {
        return ofNullable(model.getSchema().get("required"))
                .filter(it -> it.getValueType() == JsonValue.ValueType.ARRAY)
                .map(JsonValue::asJsonArray)
                .filter(arr -> arr.stream().allMatch(it -> it.getValueType() == JsonValue.ValueType.STRING))
                .map(arr -> arr.stream().map(it -> JsonString.class.cast(it).getString()).collect(toSet()))
                .map(required -> new Impl(required, model.getValueProvider(), model.toPointer()));
    }

    private static class Impl extends BaseValidation {

        private final Collection<String> required;

        private Impl(final Collection<String> required, final Function<JsonValue, JsonValue> extractor,
                final String pointer) {
            super(pointer, extractor, JsonValue.ValueType.OBJECT);
            this.required = required;
        }

        @Override
        public Stream<ValidationError> onObject(final JsonObject obj) {
            if (obj == null || obj == JsonValue.NULL) {
                return toErrors(required.stream());
            }
            // workaround for requiredIf (@Required combined w/ @ActiveIf) when required option is hidden
            return toErrors(required
                    .stream()
                    .filter(name -> isPresent(obj.get(name)))
                    .filter(name -> isUnvalued(obj.get(name))));
        }

        protected boolean isPresent(final JsonValue obj) {
            return obj != null;
        }

        protected boolean isUnvalued(final JsonValue obj) {
            // maybe check for empty string if needed.
            return obj.getValueType() == JsonValue.ValueType.NULL;
        }

        private Stream<ValidationError> toErrors(final Stream<String> fields) {
            return fields.map(name -> new ValidationError(pointer, name + " is required and is not valued"));
        }

        @Override
        public String toString() {
            return "Required{" + "required=" + required + ", pointer='" + pointer + '\'' + '}';
        }
    }
}
