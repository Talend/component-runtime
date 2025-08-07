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
package org.talend.sdk.component.runtime.di.record;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneOffset.UTC;
import static java.util.Optional.ofNullable;
import static org.talend.sdk.component.api.record.Schema.Type.ARRAY;
import static org.talend.sdk.component.api.record.Schema.Type.BOOLEAN;
import static org.talend.sdk.component.api.record.Schema.Type.BYTES;
import static org.talend.sdk.component.api.record.Schema.Type.DATETIME;
import static org.talend.sdk.component.api.record.Schema.Type.DECIMAL;
import static org.talend.sdk.component.api.record.Schema.Type.DOUBLE;
import static org.talend.sdk.component.api.record.Schema.Type.FLOAT;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.LONG;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;
import static org.talend.sdk.component.api.record.SchemaCompanionUtil.sanitizeName;
import static org.talend.sdk.component.api.record.SchemaProperty.IS_KEY;
import static org.talend.sdk.component.api.record.SchemaProperty.PATTERN;
import static org.talend.sdk.component.api.record.SchemaProperty.SCALE;
import static org.talend.sdk.component.api.record.SchemaProperty.SIZE;
import static org.talend.sdk.component.api.record.SchemaProperty.STUDIO_TYPE;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.di.schema.StudioTypes;
import org.talend.sdk.component.runtime.record.MappingUtils;

import routines.system.Document;
import routines.system.DynamicMetadata;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiRowStructVisitor {

    private RecordBuilderFactory factory;

    private Builder recordBuilder;

    private Schema rowStructSchema;

    private final Jsonb jsonb = JsonbProvider.provider().create().build();

    private Set<String> allowedFields;

    private void visit(final Object data) {
        log.trace("[visit] Class: {} ==> {}.", data.getClass().getName(), data);
        for (Field field : data.getClass().getFields()) {
            try {
                final Class<?> fieldType = field.getType();
                final String studioType = StudioTypes.typeFromClass(fieldType.getName());
                final String name = field.getName();
                final Object raw = field.get(data);
                log.trace("[visit] Field {} ({} / {}) ==> {}.", name, fieldType.getName(), studioType, raw);
                if (raw == null) {
                    log.trace("[visit] Skipping field {} with null value.", name);
                    continue;
                }
                if (!allowedFields.contains(name)) {
                    log.trace("[visit] Skipping technical field {}.", name);
                    continue;
                }
                switch (studioType) {
                    case StudioTypes.OBJECT:
                        onObject(name, raw);
                        break;
                    case StudioTypes.LIST:
                        // for studio name == fieldName it's unique, and it allows only latin characters
                        onArray(rowStructSchema.getEntry(name), (Collection) raw);
                        break;
                    case StudioTypes.BYTE_ARRAY:
                        onBytes(name, byte[].class.cast(raw));
                        break;
                    case StudioTypes.CHARACTER:
                        onString(name, String.valueOf(raw));
                        break;
                    case StudioTypes.STRING:
                        onString(name, raw);
                        break;
                    case StudioTypes.BIGDECIMAL:
                        onDecimal(name, BigDecimal.class.cast(raw));
                        break;
                    case StudioTypes.BYTE:
                        onInt(name, Byte.class.cast(raw).intValue());
                        break;
                    case StudioTypes.INTEGER:
                    case StudioTypes.SHORT:
                        onInt(name, raw);
                        break;
                    case StudioTypes.LONG:
                        onLong(name, raw);
                        break;
                    case StudioTypes.FLOAT:
                        onFloat(name, raw);
                        break;
                    case StudioTypes.DOUBLE:
                        onDouble(name, raw);
                        break;
                    case StudioTypes.BOOLEAN:
                        onBoolean(name, raw);
                        break;
                    case StudioTypes.DATE:
                        if (Timestamp.class.isInstance(raw)) {
                            onInstant(name, (Timestamp) raw);
                            break;
                        }
                        onDatetime(name, Date.class.cast(raw).toInstant().atZone(UTC));
                        break;
                    case StudioTypes.DOCUMENT:
                        if (Document.class.cast(raw).getDocument() == null) {
                            log.trace("[visit] Skipping field {} with null value.", name);
                            continue; // loop
                        }
                        onDocument(name, raw);
                        break;
                    case StudioTypes.DYNAMIC:
                        handleDynamic(raw);
                        break;
                    default:
                        throw new IllegalAccessException(String.format("Invalid type: %s (%s) with value: %s. .",
                                fieldType, studioType, raw));
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private void handleDynamic(final Object raw) {
        final DynamicWrapper dynamic = new DynamicWrapper(raw);
        for (DynamicMetadata meta : dynamic.getDynamic().metadatas) {
            final Object value = dynamic.getDynamic().getColumnValue(meta.getName());
            // imo we should use the schema-entry name here instead, and find it by original name
            final String metaName = sanitizeName(meta.getName());
            log.trace("[visit] Dynamic {}\t({})\t ==> {}.", meta.getName(), meta.getType(), value);
            if (value == null) {
                continue;
            }
            switch (meta.getType()) {
                case StudioTypes.OBJECT:
                    onObject(metaName, value);
                    break;
                case StudioTypes.LIST:
                    // we can't add a new entry after we defined the schema
                    // so we can pick the entry from the schema
                    onArray(rowStructSchema.getEntry(metaName), (Collection) value);
                    break;
                case StudioTypes.STRING:
                case StudioTypes.CHARACTER:
                    onString(metaName, value);
                    break;
                case StudioTypes.BYTE_ARRAY:
                    final byte[] bytes;
                    if (byte[].class.isInstance(value)) {
                        bytes = byte[].class.cast(value);
                    } else if (ByteBuffer.class.isInstance(value)) {
                        bytes = ByteBuffer.class.cast(value).array();
                    } else {
                        log.warn("[visit] '{}' of type `id_byte[]` and content is contained in `{}`:"
                                + " This should not happen! "
                                + " Wrapping `byte[]` from `String.valueOf()`: result may be inaccurate.",
                                metaName, value.getClass().getSimpleName());
                        bytes = ByteBuffer.wrap(String.valueOf(value).getBytes()).array();
                    }
                    onBytes(metaName, bytes);
                    break;
                case StudioTypes.BYTE:
                case StudioTypes.SHORT:
                case StudioTypes.INTEGER:
                    onInt(metaName, value);
                    break;
                case StudioTypes.LONG:
                    onLong(metaName, value);
                    break;
                case StudioTypes.FLOAT:
                    onFloat(metaName, value);
                    break;
                case StudioTypes.DOUBLE:
                    onDouble(metaName, value);
                    break;
                case StudioTypes.BIGDECIMAL:
                    onDecimal(metaName, BigDecimal.class.cast(value));
                    break;
                case StudioTypes.BOOLEAN:
                    onBoolean(metaName, value);
                    break;
                case StudioTypes.DATE:
                    final ZonedDateTime dateTime;
                    dateTime = ZonedDateTime.ofInstant(value instanceof Long ? ofEpochMilli(Long.class.cast(value))
                            : Date.class.cast(value).toInstant(), UTC);
                    onDatetime(metaName, dateTime);
                    break;
                case StudioTypes.DOCUMENT:
                    onString(metaName, value);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + meta.getType());
            }
        }
    }

    public Record get(final Object data, final RecordBuilderFactory factory) {
        if (rowStructSchema == null) {
            this.factory = factory;
            rowStructSchema = inferSchema(data, factory);
        }
        recordBuilder = factory.newRecordBuilder(rowStructSchema);
        visit(data);
        return recordBuilder.build();
    }

    private <T> T getMetadata(final String metadata, final Object data, final Class<T> type) {
        try {
            final Method m = data.getClass().getDeclaredMethod(metadata);
            return ofNullable(m.invoke(data)).map(type::cast).orElse(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            return null;
        }
    }

    private Schema inferSchema(final Object data, final RecordBuilderFactory factory) {
        // all standard rowStruct fields have accessors, not technical fields.
        final Set<String> fields = Arrays
                .stream(data.getClass().getFields()) //
                .map(f -> f.getName()) //
                .collect(Collectors.toSet());
        allowedFields = Arrays
                .stream(data.getClass().getDeclaredMethods())
                .map(method -> method.getName())
                .filter(m -> m.matches("^(get|is).*"))
                .map(n -> n.replaceAll("^(get|is)", ""))
                .map(n -> {
                    if (fields.contains(n)) {
                        return n;
                    }
                    // use java convention for members
                    return n.substring(0, 1).toLowerCase(Locale.ROOT) + n.substring(1);
                })
                .collect(Collectors.toSet());
        final Schema.Builder schema = factory.newSchemaBuilder(RECORD);
        for (Field field : data.getClass().getFields()) {
            try {
                final Class<?> type = field.getType();
                if (!allowedFields.contains(field.getName())) {
                    log.trace("[inferSchema] Skipping technical field {}.", field.getName());
                    continue;
                }
                final String name = sanitizeName(field.getName());
                final Object raw = field.get(data);
                final boolean isNullable =
                        ofNullable(getMetadata(name + "IsNullable", data, Boolean.class)).orElse(true);
                final Boolean isKey = ofNullable(getMetadata(name + "IsKey", data, Boolean.class)).orElse(false);
                final Integer length = ofNullable(getMetadata(name + "Length", data, Integer.class)).orElse(-1);
                final Integer precision = ofNullable(getMetadata(name + "Precision", data, Integer.class)).orElse(-1);
                final String defaultValue = getMetadata(name + "Default", data, String.class);
                final String comment = getMetadata(name + "Comment", data, String.class);
                final String pattern = getMetadata(name + "Pattern", data, String.class);
                final String originalDbColumnName = getMetadata(name + "OriginalDbColumnName", data, String.class);
                final String studioType = StudioTypes.typeFromClass(type.getName());
                switch (studioType) {
                    case StudioTypes.LIST:
                        schema.withEntry(toCollectionEntry(name, originalDbColumnName, raw));
                        break;
                    case StudioTypes.OBJECT:
                    case StudioTypes.STRING:
                    case StudioTypes.CHARACTER:
                        schema.withEntry(toEntry(name, STRING, originalDbColumnName, isNullable, comment, isKey, length,
                                precision, defaultValue, null, studioType));
                        break;
                    case StudioTypes.BIGDECIMAL:
                        schema.withEntry(
                                toEntry(name, DECIMAL, originalDbColumnName, isNullable, comment, isKey, length,
                                        precision, defaultValue, null, studioType));
                        break;
                    case StudioTypes.INTEGER:
                    case StudioTypes.SHORT:
                    case StudioTypes.BYTE:
                        schema.withEntry(
                                toEntry(name, INT, originalDbColumnName, isNullable, comment, isKey, null, null,
                                        defaultValue, null, studioType));
                        break;
                    case StudioTypes.LONG:
                        schema.withEntry(
                                toEntry(name, LONG, originalDbColumnName, isNullable, comment, isKey, null, null,
                                        defaultValue, null, studioType));
                        break;
                    case StudioTypes.FLOAT:
                        schema.withEntry(toEntry(name, FLOAT, originalDbColumnName, isNullable, comment, isKey, length,
                                precision, defaultValue, null, studioType));
                        break;
                    case StudioTypes.DOUBLE:
                        schema.withEntry(toEntry(name, DOUBLE, originalDbColumnName, isNullable, comment, isKey, length,
                                precision, defaultValue, null, studioType));
                        break;
                    case StudioTypes.BOOLEAN:
                        schema.withEntry(toEntry(name, BOOLEAN, originalDbColumnName, isNullable, comment, isKey, null,
                                null, defaultValue, null, studioType));
                        break;
                    case StudioTypes.DATE:
                        schema.withEntry(toEntry(name, DATETIME, originalDbColumnName, isNullable, comment, isKey, null,
                                null, defaultValue, pattern, studioType));
                        break;
                    case StudioTypes.BYTE_ARRAY:
                        schema.withEntry(
                                toEntry(name, BYTES, originalDbColumnName, isNullable, comment, isKey, null, null,
                                        defaultValue, null, studioType));
                        break;
                    case StudioTypes.DOCUMENT:
                        schema.withEntry(toEntry(name, STRING, originalDbColumnName, isNullable, comment, isKey, null,
                                null, defaultValue, pattern, studioType));
                        break;
                    case StudioTypes.DYNAMIC:
                        inferDynamicSchema(raw, length, precision, pattern, schema, comment, defaultValue);
                        break;
                    default:
                        log.warn("Unmanaged type: {} for {}.", type, name);
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return schema.build();
    }

    private void inferDynamicSchema(final Object raw,
            final Integer length,
            final Integer precision,
            final String pattern,
            final Schema.Builder schema,
            final String comment,
            final String defaultValue) {
        final DynamicWrapper dynamic = new DynamicWrapper(raw);
        for (DynamicMetadata meta : dynamic.getDynamic().metadatas) {
            final Object value = dynamic.getDynamic().getColumnValue(meta.getName());
            final String metaName = sanitizeName(meta.getName());
            final String metaOriginalName = meta.getDbName();
            final boolean metaIsNullable = meta.isNullable();
            final boolean metaIsKey = meta.isKey();
            final int metaLength = meta.getLength() != -1 ? meta.getLength() : length;
            final int metaPrecision = meta.getPrecision() != -1 ? meta.getPrecision() : precision;
            final String metaPattern =
                    !meta.getFormat().equals("dd-MM-yyyy HH:mm:ss") ? meta.getFormat() : pattern;
            final String metaStudioType = meta.getType();
            log.trace("[inferSchema] Dynamic {}\t({})\t ==> {}.", meta.getName(), metaStudioType,
                    value);
            switch (metaStudioType) {
                case StudioTypes.LIST:
                    schema.withEntry(toCollectionEntry(metaName, metaOriginalName, value));
                    break;
                case StudioTypes.OBJECT:
                case StudioTypes.STRING:
                case StudioTypes.CHARACTER:
                    schema.withEntry(
                            toEntry(metaName, STRING, metaOriginalName, metaIsNullable, comment,
                                    metaIsKey, null, null, defaultValue, metaPattern, metaStudioType));
                    break;
                case StudioTypes.BIGDECIMAL:
                    schema.withEntry(toEntry(metaName, DECIMAL, metaOriginalName, metaIsNullable,
                            comment,
                            metaIsKey, metaLength, metaPrecision, defaultValue, null, metaStudioType));
                    break;
                case StudioTypes.BYTE_ARRAY:
                    schema.withEntry(toEntry(metaName, BYTES, metaOriginalName, metaIsNullable, comment,
                            metaIsKey, null, null, defaultValue, null, metaStudioType));
                    break;
                case StudioTypes.BYTE:
                case StudioTypes.SHORT:
                case StudioTypes.INTEGER:
                    schema.withEntry(toEntry(metaName, INT, metaOriginalName, metaIsNullable, comment,
                            metaIsKey, null, null, defaultValue, null, metaStudioType));
                    break;
                case StudioTypes.LONG:
                    schema.withEntry(toEntry(metaName, LONG, metaOriginalName, metaIsNullable, comment,
                            metaIsKey, null, null, defaultValue, null, metaStudioType));
                    break;
                case StudioTypes.FLOAT:
                    schema.withEntry(toEntry(metaName, FLOAT, metaOriginalName, metaIsNullable, comment,
                            metaIsKey, metaLength, metaPrecision, defaultValue, null, metaStudioType));
                    break;
                case StudioTypes.DOUBLE:
                    schema.withEntry(toEntry(metaName, DOUBLE, metaOriginalName, metaIsNullable,
                            comment,
                            metaIsKey, metaLength, metaPrecision, defaultValue, null, metaStudioType));
                    break;
                case StudioTypes.BOOLEAN:
                    schema.withEntry(
                            toEntry(metaName, BOOLEAN, metaOriginalName, metaIsNullable, comment,
                                    metaIsKey, null, null, defaultValue, null, metaStudioType));
                    break;
                case StudioTypes.DATE:
                    schema.withEntry(
                            toEntry(metaName, DATETIME, metaOriginalName, metaIsNullable, comment,
                                    metaIsKey, null, null, defaultValue, metaPattern, metaStudioType));
                    break;
                default:
                    schema.withEntry(
                            toEntry(metaName, STRING, metaOriginalName, metaIsNullable, comment,
                                    metaIsKey, metaLength, metaPrecision, defaultValue, metaPattern,
                                    metaStudioType));
            }
        }
    }

    private void onInt(final String name, final Object value) {
        recordBuilder.withInt(name, Integer.class.cast(MappingUtils.coerce(Integer.class, value, name)));
    }

    private void onLong(final String name, final Object value) {
        recordBuilder.withLong(name, Long.class.cast(MappingUtils.coerce(Long.class, value, name)));
    }

    private void onFloat(final String name, final Object value) {
        recordBuilder.withFloat(name, Float.class.cast(MappingUtils.coerce(Float.class, value, name)));
    }

    private void onDouble(final String name, final Object value) {
        recordBuilder.withDouble(name, Double.class.cast(MappingUtils.coerce(Double.class, value, name)));
    }

    private void onBoolean(final String name, final Object value) {
        recordBuilder.withBoolean(name, Boolean.class.cast(MappingUtils.coerce(Boolean.class, value, name)));
    }

    private void onString(final String name, final Object value) {
        recordBuilder.withString(name, String.class.cast(MappingUtils.coerce(String.class, value, name)));
    }

    private void onDocument(final String name, final Object raw) {
        recordBuilder.withString(name, Document.class.cast(raw).getDocument().toString());
    }

    private void onDecimal(final String name, final BigDecimal value) {
        recordBuilder.withDecimal(name, value);
    }

    private void onDatetime(final String name, final ZonedDateTime value) {
        recordBuilder.withDateTime(name, value);
    }

    private void onInstant(final String name, final Timestamp raw) {
        recordBuilder.withInstant(name, raw.toInstant());
    }

    private void onBytes(final String name, final byte[] value) {
        recordBuilder.withBytes(name, value);
    }

    private void onArray(final Entry entry, final Collection value) {
        recordBuilder.withArray(entry, value);
    }

    private void onObject(final String name, final Object value) {
        if (Record.class.isInstance(value)) {// keep old action here
            recordBuilder.withString(name, jsonb.toJson(value));
            return;
        }

        recordBuilder.with(rowStructSchema.getEntry(name), value);
    }

    // CHECKSTYLE:OFF
    private Entry toEntry(final String name, final Schema.Type type, final String originalName,
            final boolean isNullable, final String comment, final Boolean isKey, final Integer length,
            final Integer precision, final String defaultValue, final String pattern, final String studioType) {
        // CHECKSTYLE:ON
        final Map<String, String> props = new HashMap<>();
        if (isKey != null) {
            props.put(IS_KEY, String.valueOf(isKey));
        }
        if (length != null) {
            props.put(SIZE, String.valueOf(length));
        }
        if (precision != null) {
            props.put(SCALE, String.valueOf(precision));
        }
        if (pattern != null) {
            props.put(PATTERN, pattern);
        }
        props.put(STUDIO_TYPE, studioType);

        return factory
                .newEntryBuilder()
                .withName(name)
                .withRawName(originalName)
                .withNullable(isNullable)
                .withType(type)
                .withComment(comment)
                .withDefaultValue(defaultValue)
                .withProps(props)
                .build();
    }

    private Entry toCollectionEntry(final String name, final String originalName, final Object value) {
        Object coll = null;
        Type elementType = STRING;
        if (value != null && !((Collection<?>) value).isEmpty()) {
            coll = ((Collection<?>) value).iterator().next();
            elementType = getTypeFromValue(coll);
        }

        return factory.newEntryBuilder()
                .withName(name)
                .withRawName(originalName)
                .withNullable(true)
                .withType(ARRAY)
                .withElementSchema(elementSchema(elementType, coll))
                .withProp(STUDIO_TYPE, StudioTypes.LIST)
                .build();
    }

    private Schema elementSchema(final Type type, final Object value) {
        if (type != ARRAY || !(value instanceof Collection)) {
            return factory.newSchemaBuilder(type).build();
        }

        Type elementType = null;
        Object columnValue = null;

        // we inherit the logic that we evaluate the type by its first element. (at least it's fast )
        // looks like we support only homogeneous structures
        if (!((Collection<?>) value).isEmpty()) {
            columnValue = ((Collection<?>) value).iterator().next();
            elementType = getTypeFromValue(columnValue);
        }

        // if we can't evaluate the type of element we will return the same as it was, array without type
        return elementType == null
                ? factory.newSchemaBuilder(Type.ARRAY).build()
                : factory.newSchemaBuilder(Type.ARRAY)
                        .withElementSchema(elementSchema(elementType, columnValue))
                        .build();
    }

    private Schema.Type getTypeFromValue(final Object value) {
        if (String.class.isInstance(value)) {
            return STRING;
        }
        if (Integer.class.isInstance(value)) {
            return INT;
        }
        if (Long.class.isInstance(value)) {
            return LONG;
        }
        if (Float.class.isInstance(value)) {
            return FLOAT;
        }
        if (BigDecimal.class.isInstance(value)) {
            return DECIMAL;
        }
        if (Double.class.isInstance(value)) {
            return DOUBLE;
        }
        if (Boolean.class.isInstance(value)) {
            return BOOLEAN;
        }
        if (Date.class.isInstance(value) || ZonedDateTime.class.isInstance(value)) {
            return DATETIME;
        }
        if (byte[].class.isInstance(value)) {
            return BYTES;
        }
        if (Collection.class.isInstance(value)) {
            return ARRAY;
        }
        return STRING;
    }

}
