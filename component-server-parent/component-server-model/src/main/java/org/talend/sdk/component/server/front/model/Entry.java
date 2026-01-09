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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;

public final class Entry {
    @Getter
    private final String name;

    @Getter
    private final String rawName;

    @Getter
    private final Schema.Type type;

    @Getter
    private final boolean nullable;

    @Getter
    private final boolean metadata;

    @Getter
    private final boolean errorCapable;

    @Getter
    private final boolean valid;

    @Getter
    private final Schema elementSchema;

    @Getter
    private final String comment;

    @Getter
    private final Map<String, String> props = new LinkedHashMap<>(0);;

    @Getter
    private final Object internalDefaultValue;

    @ConstructorProperties({"name", "rawName", "type", "nullable", "metadata", "errorCapable",
    "valid", "elementSchema", "comment", "props", "internalDefaultValue"})
    private Entry(
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
            final Object internalDefaultValue) {
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
        this.internalDefaultValue = internalDefaultValue;
    }

    public <T> T getDefaultValue(){
        return (T) this.getInternalDefaultValue();
    }

    public String getOriginalFieldName() {
        return rawName != null ? rawName : name;
    }

    public String getProp(final String key) {
        return this.props.get(key);
    }

    public final Entry withName(final String value) {
        String newValue = Objects.requireNonNull(value, "name");
        if (this.name.equals(newValue)) {
            return this;
        }
        return new Entry(
                newValue,
                this.rawName,
                this.type,
                this.nullable,
                this.metadata,
                this.errorCapable,
                this.valid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withRawName(final String value) {
        String newValue = Objects.requireNonNull(value, "rawName");
        if (this.rawName.equals(newValue)) {
            return this;
        }
        return new Entry(
                this.name,
                newValue,
                this.type,
                this.nullable,
                this.metadata,
                this.errorCapable,
                this.valid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withType(final Schema.Type value) {
        Schema.Type newValue = Objects.requireNonNull(value, "type");
        if (this.type == newValue) {
            return this;
        }
        return new Entry(
                this.name,
                this.rawName,
                newValue,
                this.nullable,
                this.metadata,
                this.errorCapable,
                this.valid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withIsNullable(final boolean value) {
        if (this.nullable == value) {
            return this;
        }
        return new Entry(
                this.name,
                this.rawName,
                this.type,
                value,
                this.metadata,
                this.errorCapable,
                this.valid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withIsMetadata(final boolean value) {
        if (this.metadata == value) {
            return this;
        }
        return new Entry(
                this.name,
                this.rawName,
                this.type,
                this.nullable,
                value,
                this.errorCapable,
                this.valid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withIsErrorCapable(final boolean value) {
        if (this.errorCapable == value) {
            return this;
        }
        return new Entry(
                this.name,
                this.rawName,
                this.type,
                this.nullable,
                this.metadata,
                value,
                this.valid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withIsValid(final boolean value) {
        if (this.valid == value) {
            return this;
        }
        return new Entry(
                this.name,
                this.rawName,
                this.type,
                this.nullable,
                this.metadata,
                this.errorCapable,
                value,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withElementSchema(final Schema value) {
        if (this.elementSchema == value) {
            return this;
        }
        Schema newValue = Objects.requireNonNull(value, "elementSchema");
        return new Entry(
                this.name,
                this.rawName,
                this.type,
                this.nullable,
                this.metadata,
                this.errorCapable,
                this.valid,
                newValue,
                this.comment,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withComment(final String value) {
        String newValue = Objects.requireNonNull(value, "comment");
        if (this.comment.equals(newValue)) {
            return this;
        }
        return new Entry(
                this.name,
                this.rawName,
                this.type,
                this.nullable,
                this.metadata,
                this.errorCapable,
                this.valid,
                this.elementSchema,
                newValue,
                this.props,
                this.internalDefaultValue);
    }

    public final Entry withProps(final Map<String, ? extends String> entries) {
        if (this.props == entries) {
            return this;
        }
        Map<String, String> newValue = createUnmodifiableMap(true, false, entries);
        return new Entry(
                this.name,
                this.rawName,
                this.type,
                this.nullable,
                this.metadata,
                this.errorCapable,
                this.valid,
                this.elementSchema,
                this.comment,
                newValue,
                this.internalDefaultValue);
    }

    public final Entry withInternalDefaultValue(final Object value) {
        if (this.internalDefaultValue == value) {
            return this;
        }
        Object newValue = Objects.requireNonNull(value, "internalDefaultValue");
        return new Entry(
                this.name,
                this.rawName,
                this.type,
                this.nullable,
                this.metadata,
                this.errorCapable,
                this.valid,
                this.elementSchema,
                this.comment,
                this.props,
                newValue);
    }

    @Override
    public String toString() {
        return "Entry{"
                + "name=" + name
                + ", rawName=" + rawName
                + ", type=" + type
                + ", nullable=" + nullable
                + ", metadata=" + metadata
                + ", errorCapable=" + errorCapable
                + ", valid=" + valid
                + ", elementSchema=" + elementSchema
                + ", comment=" + comment
                + ", props=" + props
                + ", internalDefaultValue=" + internalDefaultValue
                + "}";
    }

    public static Entry copyOf(final Entry instance) {
        if (instance instanceof Entry) {
            return (Entry) instance;
        }
        return Entry.builder()
                .from(instance)
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final long INIT_BIT_NAME = 0x1L;
        private static final long INIT_BIT_RAW_NAME = 0x2L;
        private static final long INIT_BIT_ORIGINAL_FIELD_NAME = 0x4L;
        private static final long INIT_BIT_TYPE = 0x8L;
        private static final long INIT_BIT_IS_NULLABLE = 0x10L;
        private static final long INIT_BIT_IS_METADATA = 0x20L;
        private static final long INIT_BIT_IS_ERROR_CAPABLE = 0x40L;
        private static final long INIT_BIT_IS_VALID = 0x80L;
        private static final long INIT_BIT_ELEMENT_SCHEMA = 0x100L;
        private static final long INIT_BIT_COMMENT = 0x200L;
        private static final long INIT_BIT_INTERNAL_DEFAULT_VALUE = 0x400L;
        private long initBits = 0x7ffL;

        private String name;
        private String rawName;
        private String originalFieldName;
        private Schema.Type type;
        private boolean nullable;
        private boolean metadata;
        private boolean errorCapable;
        private boolean valid;
        private Schema elementSchema;
        private String comment;
        private Map<String, String> props = new LinkedHashMap<String, String>();
        private Object internalDefaultValue;

        private Builder() {
        }

        public final Builder from(final Entry instance) {
            Objects.requireNonNull(instance, "instance");
            from((Object) instance);
            return this;
        }

        private void from(final Object object) {
            long bits = 0;
            if (object instanceof Entry) {
                Entry instance = (Entry) object;
                if ((bits & 0x1L) == 0) {
                    this.elementSchema(instance.getElementSchema());
                    bits |= 0x1L;
                }
                if ((bits & 0x2L) == 0) {
                    this.isValid(instance.isValid());
                    bits |= 0x2L;
                }
                this.internalDefaultValue(instance.getInternalDefaultValue());
                if ((bits & 0x4L) == 0) {
                    this.type(instance.getType());
                    bits |= 0x4L;
                }
                if ((bits & 0x8L) == 0) {
                    this.rawName(instance.getRawName());
                    bits |= 0x8L;
                }
                if ((bits & 0x10L) == 0) {
                    putAllProps(instance.getProps());
                    bits |= 0x10L;
                }
                if ((bits & 0x20L) == 0) {
                    this.originalFieldName(instance.getOriginalFieldName());
                    bits |= 0x20L;
                }
                if ((bits & 0x40L) == 0) {
                    this.isNullable(instance.isNullable());
                    bits |= 0x40L;
                }
                if ((bits & 0x80L) == 0) {
                    this.name(instance.getName());
                    bits |= 0x80L;
                }
                if ((bits & 0x100L) == 0) {
                    this.comment(instance.getComment());
                    bits |= 0x100L;
                }
                if ((bits & 0x200L) == 0) {
                    this.isMetadata(instance.isMetadata());
                    bits |= 0x200L;
                }
                if ((bits & 0x400L) == 0) {
                    this.isErrorCapable(instance.isErrorCapable());
                    bits |= 0x400L;
                }
            }
            if (object instanceof Entry) {
                Entry instance = (Entry) object;
                if ((bits & 0x1L) == 0) {
                    this.elementSchema(instance.getElementSchema());
                    bits |= 0x1L;
                }
                if ((bits & 0x20L) == 0) {
                    this.originalFieldName(instance.getOriginalFieldName());
                    bits |= 0x20L;
                }
                if ((bits & 0x2L) == 0) {
                    this.isValid(instance.isValid());
                    bits |= 0x2L;
                }
                if ((bits & 0x40L) == 0) {
                    this.isNullable(instance.isNullable());
                    bits |= 0x40L;
                }
                if ((bits & 0x80L) == 0) {
                    this.name(instance.getName());
                    bits |= 0x80L;
                }
                if ((bits & 0x100L) == 0) {
                    this.comment(instance.getComment());
                    bits |= 0x100L;
                }
                if ((bits & 0x4L) == 0) {
                    this.type(instance.getType());
                    bits |= 0x4L;
                }
                if ((bits & 0x200L) == 0) {
                    this.isMetadata(instance.isMetadata());
                    bits |= 0x200L;
                }
                if ((bits & 0x400L) == 0) {
                    this.isErrorCapable(instance.isErrorCapable());
                    bits |= 0x400L;
                }
                if ((bits & 0x8L) == 0) {
                    this.rawName(instance.getRawName());
                    bits |= 0x8L;
                }
                if ((bits & 0x10L) == 0) {
                    putAllProps(instance.getProps());
                    bits |= 0x10L;
                }
            }
        }

        public final Builder name(final String name) {
            this.name = Objects.requireNonNull(name, "name");
            initBits &= ~INIT_BIT_NAME;
            return this;
        }

        public final Builder rawName(final String rawName) {
            this.rawName = Objects.requireNonNull(rawName, "rawName");
            initBits &= ~INIT_BIT_RAW_NAME;
            return this;
        }

        public final Builder originalFieldName(final String originalFieldName) {
            this.originalFieldName = Objects.requireNonNull(originalFieldName, "originalFieldName");
            initBits &= ~INIT_BIT_ORIGINAL_FIELD_NAME;
            return this;
        }

        public final Builder type(final Schema.Type type) {
            this.type = Objects.requireNonNull(type, "type");
            initBits &= ~INIT_BIT_TYPE;
            return this;
        }

        public final Builder isNullable(final boolean isNullable) {
            this.nullable = isNullable;
            initBits &= ~INIT_BIT_IS_NULLABLE;
            return this;
        }

        public final Builder isMetadata(final boolean isMetadata) {
            this.metadata = isMetadata;
            initBits &= ~INIT_BIT_IS_METADATA;
            return this;
        }

        public final Builder isErrorCapable(final boolean isErrorCapable) {
            this.errorCapable = isErrorCapable;
            initBits &= ~INIT_BIT_IS_ERROR_CAPABLE;
            return this;
        }

        public final Builder isValid(final boolean isValid) {
            this.valid = isValid;
            initBits &= ~INIT_BIT_IS_VALID;
            return this;
        }

        public final Builder elementSchema(final Schema elementSchema) {
            this.elementSchema = Objects.requireNonNull(elementSchema, "elementSchema");
            initBits &= ~INIT_BIT_ELEMENT_SCHEMA;
            return this;
        }

        public final Builder comment(final String comment) {
            this.comment = Objects.requireNonNull(comment, "comment");
            initBits &= ~INIT_BIT_COMMENT;
            return this;
        }

        public final Builder putProps(final String key, final String value) {
            this.props.put(
                    Objects.requireNonNull(key, "props key"),
                    Objects.requireNonNull(value, value == null ? "props value for key: " + key : null));
            return this;
        }

        public final Builder putProps(final Map.Entry<String, ? extends String> entry) {
            String k = entry.getKey();
            String v = entry.getValue();
            this.props.put(
                    Objects.requireNonNull(k, "props key"),
                    Objects.requireNonNull(v, v == null ? "props value for key: " + k : null));
            return this;
        }

        public final Builder props(final Map<String, ? extends String> entries) {
            this.props.clear();
            return putAllProps(entries);
        }

        public final Builder putAllProps(final Map<String, ? extends String> entries) {
            for (Map.Entry<String, ? extends String> e : entries.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();
                this.props.put(
                        Objects.requireNonNull(k, "props key"),
                        Objects.requireNonNull(v, v == null ? "props value for key: " + k : null));
            }
            return this;
        }

        public final Builder internalDefaultValue(final Object internalDefaultValue) {
            this.internalDefaultValue = Objects.requireNonNull(internalDefaultValue, "internalDefaultValue");
            initBits &= ~INIT_BIT_INTERNAL_DEFAULT_VALUE;
            return this;
        }

        public Entry build() {
            return new Entry(
                    name,
                    rawName,
                    type,
                    nullable,
                    metadata,
                    errorCapable,
                    valid,
                    elementSchema,
                    comment,
                    createUnmodifiableMap(false, false, props),
                    internalDefaultValue);
        }

    }

    private static <K, V> Map<K, V> createUnmodifiableMap(final boolean checkNulls, final boolean skipNulls,
                                                          final Map<? extends K, ? extends V> map) {
        switch (map.size()) {
            case 0:
                return Collections.emptyMap();
            case 1: {
                Map.Entry<? extends K, ? extends V> e = map.entrySet().iterator().next();
                K k = e.getKey();
                V v = e.getValue();
                if (checkNulls) {
                    Objects.requireNonNull(k, "key");
                    Objects.requireNonNull(v, v == null ? "value for key: " + k : null);
                }
                if (skipNulls && (k == null || v == null)) {
                    return Collections.emptyMap();
                }
                return Collections.singletonMap(k, v);
            }
            default: {
                Map<K, V> linkedMap = new LinkedHashMap<>(map.size() * 4 / 3 + 1);
                if (skipNulls || checkNulls) {
                    for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
                        K k = e.getKey();
                        V v = e.getValue();
                        if (skipNulls) {
                            if (k == null || v == null) {
                                continue;
                            }
                        } else if (checkNulls) {
                            Objects.requireNonNull(k, "key");
                            Objects.requireNonNull(v, v == null ? "value for key: " + k : null);
                        }
                        linkedMap.put(k, v);
                    }
                } else {
                    linkedMap.putAll(map);
                }
                return Collections.unmodifiableMap(linkedMap);
            }
        }
    }
}