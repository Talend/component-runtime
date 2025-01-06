/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonNumber;
import javax.json.JsonValue;

import org.apache.johnzon.jsonschema.ValidationResult;

/**
 * Delete this class when next johnzon-jsonschema release is out
 */
abstract class BaseNumberValidation extends BaseValidation {

    protected final double bound;

    BaseNumberValidation(final String pointer, final Function<JsonValue, JsonValue> extractor, final double bound) {
        super(pointer, extractor, JsonValue.ValueType.NUMBER);
        this.bound = bound;
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
