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
package org.talend.sdk.component.server.front.model;

import java.beans.ConstructorProperties;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;

@Data
public final class Entry {

    private final String name;

    private final String rawName;

    private final Schema.Type type;

    private final boolean nullable;

    private final boolean metadata;

    private final boolean errorCapable;

    private final boolean valid;

    private final Schema elementSchema;

    private final String comment;

    private final Map<String, String> props = new LinkedHashMap<>(0);

    private final Object defaultValue;

    @ConstructorProperties({ "name", "rawName", "type", "nullable", "metadata", "errorCapable",
            "valid", "elementSchema", "comment", "props", "defaultValue" })
    // Checkstyle off to let have 11 parameters to this constructor (normally 10 max)
    // CHECKSTYLE:OFF
    public Entry(
            final String name,
            final String rawName,
            final Schema.Type type,
            final boolean nullable,
            final boolean metadata,
            final boolean errorCapable,
            final boolean valid,
            final Schema elementSchema,
            final String comment,
            final Map<String, String> props,
            final Object defaultValue) {
        // CHECKSTYLE:ON
        this.name = name;
        this.rawName = rawName;
        this.type = type;
        this.nullable = nullable;
        this.metadata = metadata;
        this.errorCapable = errorCapable;
        this.valid = valid;
        this.elementSchema = elementSchema;
        this.comment = comment;
        this.props.putAll(props);
        this.defaultValue = defaultValue;
    }

    private Object getInternalDefaultValue() {
        return defaultValue;
    }

    public <T> T getDefaultValue() {
        return (T) this.getInternalDefaultValue();
    }

    public String getOriginalFieldName() {
        return rawName != null ? rawName : name;
    }

    public String getProp(final String key) {
        return this.props.get(key);
    }

}