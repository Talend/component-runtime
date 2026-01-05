package org.talend.sdk.component.server.front.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.talend.sdk.component.api.record.Schema;

public final class Entry implements Schema.Entry {
    private final String name;
    private final String rawName;
    private final String originalFieldName;
    private final Schema.Type type;
    private final boolean isNullable;
    private final boolean isMetadata;
    private final boolean isErrorCapable;
    private final boolean isValid;
    private final Schema elementSchema;
    private final String comment;
    private final Map<String, String> props;
    private final Object internalDefaultValue;
    private final Map<String, String> properties;

    private Entry(
            String name,
            String rawName,
            String originalFieldName,
            Schema.Type type,
            boolean isNullable,
            boolean isMetadata,
            boolean isErrorCapable,
            boolean isValid,
            Schema elementSchema,
            String comment,
            Map<String, String> props,
            Object internalDefaultValue,
            Map<String, String> properties) {
        this.name = name;
        this.rawName = rawName;
        this.originalFieldName = originalFieldName;
        this.type = type;
        this.isNullable = isNullable;
        this.isMetadata = isMetadata;
        this.isErrorCapable = isErrorCapable;
        this.isValid = isValid;
        this.elementSchema = elementSchema;
        this.comment = comment;
        this.props = props;
        this.internalDefaultValue = internalDefaultValue;
        this.properties = properties;
    }

    @Override
    public <T> T getDefaultValue(){
        return (T) this.getInternalDefaultValue();
    }

    @Override
    public String getProp(String key) {
        return this.getProperties().get(key);
    }

    /**
     * @return The value of the {@code name} attribute
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return The value of the {@code rawName} attribute
     */
    @Override
    public String getRawName() {
        return rawName;
    }

    /**
     * @return The value of the {@code originalFieldName} attribute
     */
    @Override
    public String getOriginalFieldName() {
        return originalFieldName;
    }

    /**
     * @return The value of the {@code type} attribute
     */
    @Override
    public Schema.Type getType() {
        return type;
    }

    /**
     * @return The value of the {@code isNullable} attribute
     */
    @Override
    public boolean isNullable() {
        return isNullable;
    }

    /**
     * @return The value of the {@code isMetadata} attribute
     */
    @Override
    public boolean isMetadata() {
        return isMetadata;
    }

    /**
     * @return The value of the {@code isErrorCapable} attribute
     */
    @Override
    public boolean isErrorCapable() {
        return isErrorCapable;
    }

    /**
     * @return The value of the {@code isValid} attribute
     */
    @Override
    public boolean isValid() {
        return isValid;
    }

    /**
     * @return The value of the {@code elementSchema} attribute
     */
    @Override
    public Schema getElementSchema() {
        return elementSchema;
    }

    /**
     * @return The value of the {@code comment} attribute
     */
    @Override
    public String getComment() {
        return comment;
    }

    /**
     * @return The value of the {@code props} attribute
     */
    @Override
    public Map<String, String> getProps() {
        return props;
    }

    /**
     * @return The value of the {@code internalDefaultValue} attribute
     */
    public Object getInternalDefaultValue() {
        return internalDefaultValue;
    }

    /**
     * @return The value of the {@code properties} attribute
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#getName() name} attribute.
     * An equals check used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for name
     * @return A modified copy of the {@code this} object
     */
    public final Entry withName(String value) {
        String newValue = Objects.requireNonNull(value, "name");
        if (this.name.equals(newValue)) return this;
        return new Entry(
                newValue,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#getRawName() rawName} attribute.
     * An equals check used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for rawName
     * @return A modified copy of the {@code this} object
     */
    public final Entry withRawName(String value) {
        String newValue = Objects.requireNonNull(value, "rawName");
        if (this.rawName.equals(newValue)) return this;
        return new Entry(
                this.name,
                newValue,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#getOriginalFieldName() originalFieldName} attribute.
     * An equals check used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for originalFieldName
     * @return A modified copy of the {@code this} object
     */
    public final Entry withOriginalFieldName(String value) {
        String newValue = Objects.requireNonNull(value, "originalFieldName");
        if (this.originalFieldName.equals(newValue)) return this;
        return new Entry(
                this.name,
                this.rawName,
                newValue,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#getType() type} attribute.
     * A value equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for type
     * @return A modified copy of the {@code this} object
     */
    public final Entry withType(Schema.Type value) {
        Schema.Type newValue = Objects.requireNonNull(value, "type");
        if (this.type == newValue) return this;
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                newValue,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#isNullable() isNullable} attribute.
     * A value equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for isNullable
     * @return A modified copy of the {@code this} object
     */
    public final Entry withIsNullable(boolean value) {
        if (this.isNullable == value) return this;
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                value,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#isMetadata() isMetadata} attribute.
     * A value equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for isMetadata
     * @return A modified copy of the {@code this} object
     */
    public final Entry withIsMetadata(boolean value) {
        if (this.isMetadata == value) return this;
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                value,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#isErrorCapable() isErrorCapable} attribute.
     * A value equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for isErrorCapable
     * @return A modified copy of the {@code this} object
     */
    public final Entry withIsErrorCapable(boolean value) {
        if (this.isErrorCapable == value) return this;
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                value,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#isValid() isValid} attribute.
     * A value equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for isValid
     * @return A modified copy of the {@code this} object
     */
    public final Entry withIsValid(boolean value) {
        if (this.isValid == value) return this;
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                value,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#getElementSchema() elementSchema} attribute.
     * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for elementSchema
     * @return A modified copy of the {@code this} object
     */
    public final Entry withElementSchema(Schema value) {
        if (this.elementSchema == value) return this;
        Schema newValue = Objects.requireNonNull(value, "elementSchema");
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                newValue,
                this.comment,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#getComment() comment} attribute.
     * An equals check used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for comment
     * @return A modified copy of the {@code this} object
     */
    public final Entry withComment(String value) {
        String newValue = Objects.requireNonNull(value, "comment");
        if (this.comment.equals(newValue)) return this;
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                newValue,
                this.props,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by replacing the {@link Entry#getProps() props} map with the specified map.
     * Nulls are not permitted as keys or values.
     * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param entries The entries to be added to the props map
     * @return A modified copy of {@code this} object
     */
    public final Entry withProps(Map<String, ? extends String> entries) {
        if (this.props == entries) return this;
        Map<String, String> newValue = createUnmodifiableMap(true, false, entries);
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                newValue,
                this.internalDefaultValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by setting a value for the {@link Entry#getInternalDefaultValue() internalDefaultValue} attribute.
     * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param value A new value for internalDefaultValue
     * @return A modified copy of the {@code this} object
     */
    public final Entry withInternalDefaultValue(Object value) {
        if (this.internalDefaultValue == value) return this;
        Object newValue = Objects.requireNonNull(value, "internalDefaultValue");
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                newValue,
                this.properties);
    }

    /**
     * Copy the current immutable object by replacing the {@link Entry#getProperties() properties} map with the specified map.
     * Nulls are not permitted as keys or values.
     * A shallow reference equality check is used to prevent copying of the same value by returning {@code this}.
     *
     * @param entries The entries to be added to the properties map
     * @return A modified copy of {@code this} object
     */
    public final Entry withProperties(Map<String, ? extends String> entries) {
        if (this.properties == entries) return this;
        Map<String, String> newValue = createUnmodifiableMap(true, false, entries);
        return new Entry(
                this.name,
                this.rawName,
                this.originalFieldName,
                this.type,
                this.isNullable,
                this.isMetadata,
                this.isErrorCapable,
                this.isValid,
                this.elementSchema,
                this.comment,
                this.props,
                this.internalDefaultValue,
                newValue);
    }

    /**
     * This instance is equal to all instances of {@code EntryDetail} that have equal attribute values.
     *
     * @return {@code true} if {@code this} is equal to {@code another} instance
     */
    @Override
    public boolean equals(Object another) {
        if (this == another) return true;
        return another instanceof Entry
                && equalTo(0, (Entry) another);
    }

    private boolean equalTo(int synthetic, Entry another) {
        return name.equals(another.name)
                && rawName.equals(another.rawName)
                && originalFieldName.equals(another.originalFieldName)
                && type.equals(another.type)
                && isNullable == another.isNullable
                && isMetadata == another.isMetadata
                && isErrorCapable == another.isErrorCapable
                && isValid == another.isValid
                && elementSchema.equals(another.elementSchema)
                && comment.equals(another.comment)
                && props.equals(another.props)
                && internalDefaultValue.equals(another.internalDefaultValue)
                && properties.equals(another.properties);
    }

    /**
     * Computes a hash code from attributes: {@code name}, {@code rawName}, {@code originalFieldName}, {@code type}, {@code isNullable}, {@code isMetadata}, {@code isErrorCapable}, {@code isValid}, {@code elementSchema}, {@code comment}, {@code props}, {@code internalDefaultValue}, {@code properties}.
     *
     * @return hashCode value
     */
    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + name.hashCode();
        h += (h << 5) + rawName.hashCode();
        h += (h << 5) + originalFieldName.hashCode();
        h += (h << 5) + type.hashCode();
        h += (h << 5) + Boolean.hashCode(isNullable);
        h += (h << 5) + Boolean.hashCode(isMetadata);
        h += (h << 5) + Boolean.hashCode(isErrorCapable);
        h += (h << 5) + Boolean.hashCode(isValid);
        h += (h << 5) + elementSchema.hashCode();
        h += (h << 5) + comment.hashCode();
        h += (h << 5) + props.hashCode();
        h += (h << 5) + internalDefaultValue.hashCode();
        h += (h << 5) + properties.hashCode();
        return h;
    }

    /**
     * Prints the immutable value {@code Entry} with attribute values.
     *
     * @return A string representation of the value
     */
    @Override
    public String toString() {
        return "Entry{"
                + "name=" + name
                + ", rawName=" + rawName
                + ", originalFieldName=" + originalFieldName
                + ", type=" + type
                + ", isNullable=" + isNullable
                + ", isMetadata=" + isMetadata
                + ", isErrorCapable=" + isErrorCapable
                + ", isValid=" + isValid
                + ", elementSchema=" + elementSchema
                + ", comment=" + comment
                + ", props=" + props
                + ", internalDefaultValue=" + internalDefaultValue
                + ", properties=" + properties
                + "}";
    }

    /**
     * Creates an immutable copy of a {@link Entry} value.
     * Uses accessors to get values to initialize the new immutable instance.
     * If an instance is already immutable, it is returned as is.
     *
     * @param instance The instance to copy
     * @return A copied immutable Entry instance
     */
    public static Entry copyOf(Entry instance) {
        if (instance instanceof Entry) {
            return (Entry) instance;
        }
        return Entry.builder()
                .from(instance)
                .build();
    }

    /**
     * Creates a builder for {@link Entry EntryDetail}.
     * <pre>
     * EntryDetail.builder()
     *    .name(String) // required {@link Entry#getName() name}
     *    .rawName(String) // required {@link Entry#getRawName() rawName}
     *    .originalFieldName(String) // required {@link Entry#getOriginalFieldName() originalFieldName}
     *    .type(org.talend.sdk.component.api.record.Schema.Type) // required {@link Entry#getType() type}
     *    .isNullable(boolean) // required {@link Entry#isNullable() isNullable}
     *    .isMetadata(boolean) // required {@link Entry#isMetadata() isMetadata}
     *    .isErrorCapable(boolean) // required {@link Entry#isErrorCapable() isErrorCapable}
     *    .isValid(boolean) // required {@link Entry#isValid() isValid}
     *    .elementSchema(org.talend.sdk.component.api.record.Schema) // required {@link Entry#getElementSchema() elementSchema}
     *    .comment(String) // required {@link Entry#getComment() comment}
     *    .putProps|putAllProps(String =&gt; String) // {@link Entry#getProps() props} mappings
     *    .internalDefaultValue(Object) // required {@link Entry#getInternalDefaultValue() internalDefaultValue}
     *    .putProperties|putAllProperties(String =&gt; String) // {@link Entry#getProperties() properties} mappings
     *    .build();
     * </pre>
     *
     * @return A new EntryDetail builder
     */
    public static Entry.Builder builder() {
        return new Entry.Builder();
    }

    /**
     * Builds instances of type {@link Entry EntryDetail}.
     * Initialize attributes and then invoke the {@link #build()} method to create an
     * immutable instance.
     * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
     * but instead used immediately to create instances.</em>
     */
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
        private boolean isNullable;
        private boolean isMetadata;
        private boolean isErrorCapable;
        private boolean isValid;
        private Schema elementSchema;
        private String comment;
        private Map<String, String> props = new LinkedHashMap<String, String>();
        private Object internalDefaultValue;
        private Map<String, String> properties = new LinkedHashMap<String, String>();

        private Builder() {
        }

        /**
         * Fill a builder with attribute values from the provided {@code org.talend.sdk.component.server.front.model.Entry} instance.
         *
         * @param instance The instance from which to copy values
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder from(Entry instance) {
            Objects.requireNonNull(instance, "instance");
            from((short) 0, (Object) instance);
            return this;
        }

        /**
         * Fill a builder with attribute values from the provided {@code org.talend.sdk.component.api.record.Schema.Entry} instance.
         *
         * @param instance The instance from which to copy values
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder from(Schema.Entry instance) {
            Objects.requireNonNull(instance, "instance");
            from((short) 0, (Object) instance);
            return this;
        }

        private void from(short _unused, Object object) {
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
                putAllProperties(instance.getProperties());
            }
            if (object instanceof Schema.Entry) {
                Schema.Entry instance = (Schema.Entry) object;
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

        /**
         * Initializes the value for the {@link Entry#getName() name} attribute.
         *
         * @param name The value for name
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder name(String name) {
            this.name = Objects.requireNonNull(name, "name");
            initBits &= ~INIT_BIT_NAME;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#getRawName() rawName} attribute.
         *
         * @param rawName The value for rawName
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder rawName(String rawName) {
            this.rawName = Objects.requireNonNull(rawName, "rawName");
            initBits &= ~INIT_BIT_RAW_NAME;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#getOriginalFieldName() originalFieldName} attribute.
         *
         * @param originalFieldName The value for originalFieldName
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder originalFieldName(String originalFieldName) {
            this.originalFieldName = Objects.requireNonNull(originalFieldName, "originalFieldName");
            initBits &= ~INIT_BIT_ORIGINAL_FIELD_NAME;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#getType() type} attribute.
         *
         * @param type The value for type
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder type(Schema.Type type) {
            this.type = Objects.requireNonNull(type, "type");
            initBits &= ~INIT_BIT_TYPE;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#isNullable() isNullable} attribute.
         *
         * @param isNullable The value for isNullable
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder isNullable(boolean isNullable) {
            this.isNullable = isNullable;
            initBits &= ~INIT_BIT_IS_NULLABLE;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#isMetadata() isMetadata} attribute.
         *
         * @param isMetadata The value for isMetadata
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder isMetadata(boolean isMetadata) {
            this.isMetadata = isMetadata;
            initBits &= ~INIT_BIT_IS_METADATA;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#isErrorCapable() isErrorCapable} attribute.
         *
         * @param isErrorCapable The value for isErrorCapable
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder isErrorCapable(boolean isErrorCapable) {
            this.isErrorCapable = isErrorCapable;
            initBits &= ~INIT_BIT_IS_ERROR_CAPABLE;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#isValid() isValid} attribute.
         *
         * @param isValid The value for isValid
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder isValid(boolean isValid) {
            this.isValid = isValid;
            initBits &= ~INIT_BIT_IS_VALID;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#getElementSchema() elementSchema} attribute.
         *
         * @param elementSchema The value for elementSchema
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder elementSchema(Schema elementSchema) {
            this.elementSchema = Objects.requireNonNull(elementSchema, "elementSchema");
            initBits &= ~INIT_BIT_ELEMENT_SCHEMA;
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#getComment() comment} attribute.
         *
         * @param comment The value for comment
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder comment(String comment) {
            this.comment = Objects.requireNonNull(comment, "comment");
            initBits &= ~INIT_BIT_COMMENT;
            return this;
        }

        /**
         * Put one entry to the {@link Entry#getProps() props} map.
         *
         * @param key   The key in the props map
         * @param value The associated value in the props map
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder putProps(String key, String value) {
            this.props.put(
                    Objects.requireNonNull(key, "props key"),
                    Objects.requireNonNull(value, value == null ? "props value for key: " + key : null));
            return this;
        }

        /**
         * Put one entry to the {@link Entry#getProps() props} map. Nulls are not permitted
         *
         * @param entry The key and value entry
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder putProps(Map.Entry<String, ? extends String> entry) {
            String k = entry.getKey();
            String v = entry.getValue();
            this.props.put(
                    Objects.requireNonNull(k, "props key"),
                    Objects.requireNonNull(v, v == null ? "props value for key: " + k : null));
            return this;
        }

        /**
         * Sets or replaces all mappings from the specified map as entries for the {@link Entry#getProps() props} map. Nulls are not permitted
         *
         * @param entries The entries that will be added to the props map
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder props(Map<String, ? extends String> entries) {
            this.props.clear();
            return putAllProps(entries);
        }

        /**
         * Put all mappings from the specified map as entries to {@link Entry#getProps() props} map. Nulls are not permitted
         *
         * @param entries The entries that will be added to the props map
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder putAllProps(Map<String, ? extends String> entries) {
            for (Map.Entry<String, ? extends String> e : entries.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();
                this.props.put(
                        Objects.requireNonNull(k, "props key"),
                        Objects.requireNonNull(v, v == null ? "props value for key: " + k : null));
            }
            return this;
        }

        /**
         * Initializes the value for the {@link Entry#getInternalDefaultValue() internalDefaultValue} attribute.
         *
         * @param internalDefaultValue The value for internalDefaultValue
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder internalDefaultValue(Object internalDefaultValue) {
            this.internalDefaultValue = Objects.requireNonNull(internalDefaultValue, "internalDefaultValue");
            initBits &= ~INIT_BIT_INTERNAL_DEFAULT_VALUE;
            return this;
        }

        /**
         * Put one entry to the {@link Entry#getProperties() properties} map.
         *
         * @param key   The key in the properties map
         * @param value The associated value in the properties map
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder putProperties(String key, String value) {
            this.properties.put(
                    Objects.requireNonNull(key, "properties key"),
                    Objects.requireNonNull(value, value == null ? "properties value for key: " + key : null));
            return this;
        }

        /**
         * Put one entry to the {@link Entry#getProperties() properties} map. Nulls are not permitted
         *
         * @param entry The key and value entry
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder putProperties(Map.Entry<String, ? extends String> entry) {
            String k = entry.getKey();
            String v = entry.getValue();
            this.properties.put(
                    Objects.requireNonNull(k, "properties key"),
                    Objects.requireNonNull(v, v == null ? "properties value for key: " + k : null));
            return this;
        }

        /**
         * Sets or replaces all mappings from the specified map as entries for the {@link Entry#getProperties() properties} map. Nulls are not permitted
         *
         * @param entries The entries that will be added to the properties map
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder properties(Map<String, ? extends String> entries) {
            this.properties.clear();
            return putAllProperties(entries);
        }

        /**
         * Put all mappings from the specified map as entries to {@link Entry#getProperties() properties} map. Nulls are not permitted
         *
         * @param entries The entries that will be added to the properties map
         * @return {@code this} builder for use in a chained invocation
         */
        public final Builder putAllProperties(Map<String, ? extends String> entries) {
            for (Map.Entry<String, ? extends String> e : entries.entrySet()) {
                String k = e.getKey();
                String v = e.getValue();
                this.properties.put(
                        Objects.requireNonNull(k, "properties key"),
                        Objects.requireNonNull(v, v == null ? "properties value for key: " + k : null));
            }
            return this;
        }

        /**
         * Builds a new {@link Entry EntryDetail}.
         *
         * @return An immutable instance of Entry
         * @throws IllegalStateException if any required attributes are missing
         */
        public Entry build() {
            if (initBits != 0) {
                throw new IllegalStateException(formatRequiredAttributesMessage());
            }
            return new Entry(
                    name,
                    rawName,
                    originalFieldName,
                    type,
                    isNullable,
                    isMetadata,
                    isErrorCapable,
                    isValid,
                    elementSchema,
                    comment,
                    createUnmodifiableMap(false, false, props),
                    internalDefaultValue,
                    createUnmodifiableMap(false, false, properties));
        }

        private String formatRequiredAttributesMessage() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_NAME) != 0) attributes.add("name");
            if ((initBits & INIT_BIT_RAW_NAME) != 0) attributes.add("rawName");
            if ((initBits & INIT_BIT_ORIGINAL_FIELD_NAME) != 0) attributes.add("originalFieldName");
            if ((initBits & INIT_BIT_TYPE) != 0) attributes.add("type");
            if ((initBits & INIT_BIT_IS_NULLABLE) != 0) attributes.add("isNullable");
            if ((initBits & INIT_BIT_IS_METADATA) != 0) attributes.add("isMetadata");
            if ((initBits & INIT_BIT_IS_ERROR_CAPABLE) != 0) attributes.add("isErrorCapable");
            if ((initBits & INIT_BIT_IS_VALID) != 0) attributes.add("isValid");
            if ((initBits & INIT_BIT_ELEMENT_SCHEMA) != 0) attributes.add("elementSchema");
            if ((initBits & INIT_BIT_COMMENT) != 0) attributes.add("comment");
            if ((initBits & INIT_BIT_INTERNAL_DEFAULT_VALUE) != 0) attributes.add("internalDefaultValue");
            return "Cannot build Entry, some of required attributes are not set " + attributes;
        }
    }

    private static <K, V> Map<K, V> createUnmodifiableMap(boolean checkNulls, boolean skipNulls, Map<? extends K, ? extends V> map) {
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
                            if (k == null || v == null) continue;
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