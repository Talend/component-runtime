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
package org.talend.sdk.component.form.internal.validation.jsonschema.spi;

import static java.util.stream.Collectors.joining;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class ValidationContext {

    private final String[] path;

    private final JsonObject schema;

    private final UnaryOperator<JsonValue> valueProvider;

    public ValidationContext(final String[] path, final JsonObject schema,
            final UnaryOperator<JsonValue> valueProvider) {
        this.path = path;
        this.schema = schema;
        this.valueProvider = valueProvider;
    }

    public UnaryOperator<JsonValue> getValueProvider() {
        return valueProvider;
    }

    public String[] getPath() {
        return path;
    }

    public JsonObject getSchema() {
        return schema;
    }

    public String toPointer() {
        return Stream.of(path).collect(joining("/", "/", ""));
    }
}
