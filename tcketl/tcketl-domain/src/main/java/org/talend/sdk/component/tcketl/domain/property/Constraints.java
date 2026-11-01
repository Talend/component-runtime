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
package org.talend.sdk.component.tcketl.domain.property;

import java.util.List;
import java.util.OptionalInt;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Declarative validation rules of a property. {@code min}/{@code max} bound a number, {@code minLength}/
 * {@code maxLength} a string length and {@code minItems}/{@code maxItems} an array size. {@code pattern} is a
 * JavaScript regular expression, empty when absent.
 */
public record Constraints(
        boolean required,
        OptionalInt min,
        OptionalInt max,
        OptionalInt minLength,
        OptionalInt maxLength,
        OptionalInt minItems,
        OptionalInt maxItems,
        boolean uniqueItems,
        String pattern,
        List<String> enumValues
) {

    public static final Constraints NONE = new Constraints(false, OptionalInt.empty(), OptionalInt.empty(),
            OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), false, "", List.of());

    public Constraints {
        Preconditions.requireNonNull(min, "min");
        Preconditions.requireNonNull(max, "max");
        Preconditions.requireNonNull(minLength, "minLength");
        Preconditions.requireNonNull(maxLength, "maxLength");
        Preconditions.requireNonNull(minItems, "minItems");
        Preconditions.requireNonNull(maxItems, "maxItems");
        pattern = Preconditions.nullToEmpty(pattern);
        enumValues = Preconditions.distinctNonBlank(enumValues, "enumValues");
    }

    public Constraints withRequired(final boolean value) {
        return new Constraints(value, min, max, minLength, maxLength, minItems, maxItems, uniqueItems, pattern,
                enumValues);
    }

    public Constraints withMin(final int value) {
        return new Constraints(required, OptionalInt.of(value), max, minLength, maxLength, minItems, maxItems,
                uniqueItems, pattern, enumValues);
    }

    public Constraints withMax(final int value) {
        return new Constraints(required, min, OptionalInt.of(value), minLength, maxLength, minItems, maxItems,
                uniqueItems, pattern, enumValues);
    }

    public Constraints withMinLength(final int value) {
        return new Constraints(required, min, max, OptionalInt.of(value), maxLength, minItems, maxItems, uniqueItems,
                pattern, enumValues);
    }

    public Constraints withMaxLength(final int value) {
        return new Constraints(required, min, max, minLength, OptionalInt.of(value), minItems, maxItems, uniqueItems,
                pattern, enumValues);
    }

    public Constraints withMinItems(final int value) {
        return new Constraints(required, min, max, minLength, maxLength, OptionalInt.of(value), maxItems, uniqueItems,
                pattern, enumValues);
    }

    public Constraints withMaxItems(final int value) {
        return new Constraints(required, min, max, minLength, maxLength, minItems, OptionalInt.of(value), uniqueItems,
                pattern, enumValues);
    }

    public Constraints withUniqueItems(final boolean value) {
        return new Constraints(required, min, max, minLength, maxLength, minItems, maxItems, value, pattern,
                enumValues);
    }

    public Constraints withPattern(final String value) {
        return new Constraints(required, min, max, minLength, maxLength, minItems, maxItems, uniqueItems, value,
                enumValues);
    }

    public Constraints withEnumValues(final List<String> values) {
        return new Constraints(required, min, max, minLength, maxLength, minItems, maxItems, uniqueItems, pattern,
                values);
    }
}
