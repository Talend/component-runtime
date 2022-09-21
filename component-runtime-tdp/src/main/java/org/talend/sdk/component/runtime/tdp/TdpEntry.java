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
package org.talend.sdk.component.runtime.tdp;

import javax.json.JsonValue;
import java.util.Map;
import java.util.Objects;
import org.talend.sdk.component.api.record.Schema;

public class TdpEntry implements Schema.Entry {

    private String name;

    private String rawName;

    private String originalFieldName;

    private Schema.Type type;

    private Object defaultValue;

    private boolean isNullable;

    private boolean isMetadata;

    private Map<String, String> props;

    private String comment;

    private TdpEntry() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRawName() {
        return rawName;
    }

    @Override
    public String getOriginalFieldName() {
        return originalFieldName;
    }

    @Override
    public Schema.Type getType() {
        return type;
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    @Override
    public boolean isMetadata() {
        return isMetadata;
    }

    @Override
    public <T> T getDefaultValue() {
        // noinspection unchecked trust me...
        return (T) defaultValue;
    }

    @Override
    public Schema getElementSchema() {
        throw new UnsupportedOperationException("#getElementSchema is not supported");
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public Map<String, String> getProps() {
        return props;
    }

    @Override
    public String getProp(final String property) {
        return props.get(property);
    }

    @Override
    public JsonValue getJsonProp(final String name) {
        throw new UnsupportedOperationException("#getJsonProp is not supported");
    }

    /**
     * Warning: will mutate existing instance
     */
    @Override
    public Schema.Entry.Builder toBuilder() {
        return new Builder(this);
    }

    public static Schema.Entry.Builder builder() {
        return new Builder();
    }

    /**
     * Setters are only here to allow mutation from builder
     */
    private TdpEntry setName(final String name) {
        this.name = name;
        return this;
    }

    private TdpEntry setRawName(final String rawName) {
        this.rawName = rawName;
        return this;
    }

    private TdpEntry setOriginalFieldName(final String originalFieldName) {
        this.originalFieldName = originalFieldName;
        return this;
    }

    private TdpEntry setType(final Schema.Type type) {
        this.type = type;
        return this;
    }

    private TdpEntry setDefaultValue(final Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    private TdpEntry setNullable(final boolean nullable) {
        isNullable = nullable;
        return this;
    }

    private TdpEntry setMetadata(final boolean metadata) {
        isMetadata = metadata;
        return this;
    }

    private TdpEntry setProps(final Map<String, String> props) {
        this.props = props;
        return this;
    }

    private TdpEntry setComment(final String comment) {
        this.comment = comment;
        return this;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final TdpEntry tdpEntry = (TdpEntry) o;
        return name.equals(tdpEntry.name) && type == tdpEntry.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "TdpEntry{" +
                "name='" + name + '\'' +
                ", rawName='" + rawName + '\'' +
                ", type=" + type +
                '}';
    }

    /**
     * An awful builder which can either build a fresh {@link org.talend.sdk.component.api.record.Schema.Entry}
     * instance, or mutate an existing one.
     */
    static class Builder implements Schema.Entry.Builder {

        private final TdpEntry tdpEntry;

        Builder(final TdpEntry tdpEntry) {
            this.tdpEntry = tdpEntry;
        }

        Builder() {
            this.tdpEntry = new TdpEntry();
        }

        @Override
        public Schema.Entry.Builder withName(final String name) {
            tdpEntry.setName(name);
            return this;
        }

        @Override
        public Schema.Entry.Builder withRawName(final String rawName) {
            tdpEntry.setRawName(rawName);
            return this;
        }

        @Override
        public Schema.Entry.Builder withType(final Schema.Type type) {
            tdpEntry.setType(type);
            return this;
        }

        @Override
        public Schema.Entry.Builder withNullable(final boolean nullable) {
            tdpEntry.setNullable(nullable);
            return this;
        }

        @Override
        public Schema.Entry.Builder withMetadata(final boolean metadata) {
            tdpEntry.setMetadata(metadata);
            return this;
        }

        @Override
        public <T> Schema.Entry.Builder withDefaultValue(final T value) {
            tdpEntry.setDefaultValue(value);
            return this;
        }

        @Override
        public Schema.Entry.Builder withElementSchema(final Schema schema) {
            throw new UnsupportedOperationException("#withElementSchema is not supported");
        }

        @Override
        public Schema.Entry.Builder withComment(final String comment) {
            tdpEntry.setComment(comment);
            return this;
        }

        @Override
        public Schema.Entry.Builder withProps(final Map<String, String> props) {
            tdpEntry.setProps(props);
            return this;
        }

        @Override
        public Schema.Entry.Builder withProp(final String key, final String value) {
            return null;
        }

        @Override
        public Schema.Entry build() {
            return tdpEntry;
        }
    }
}
