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

import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;

import org.talend.sdk.component.form.internal.validation.jsonschema.ValidationResult.ValidationError;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationContext;
import org.talend.sdk.component.form.internal.validation.jsonschema.spi.ValidationExtension;

public class IntegerValidation implements ValidationExtension {

    @Override
    public Optional<Function<JsonValue, Stream<ValidationError>>> create(final ValidationContext model) {
        final JsonValue type = model.getSchema().get("type");
        if (type.getValueType().equals(ValueType.STRING) && "integer".equals(JsonString.class.cast(type).getString())) {
            return Optional.of(new MultipleOfValidation.Impl(model.toPointer(), model.getValueProvider(), 1));
        }
        return Optional.empty();
    }

}
