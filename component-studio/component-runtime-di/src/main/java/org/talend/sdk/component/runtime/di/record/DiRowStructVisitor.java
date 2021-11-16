/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
import static org.talend.sdk.component.api.record.Schema.Type.ARRAY;
import static org.talend.sdk.component.api.record.Schema.Type.BOOLEAN;
import static org.talend.sdk.component.api.record.Schema.Type.BYTES;
import static org.talend.sdk.component.api.record.Schema.Type.DATETIME;
import static org.talend.sdk.component.api.record.Schema.Type.DOUBLE;
import static org.talend.sdk.component.api.record.Schema.Type.FLOAT;
import static org.talend.sdk.component.api.record.Schema.Type.INT;
import static org.talend.sdk.component.api.record.Schema.Type.LONG;
import static org.talend.sdk.component.api.record.Schema.Type.RECORD;
import static org.talend.sdk.component.api.record.Schema.Type.STRING;
import static org.talend.sdk.component.api.record.Schema.sanitizeConnectionName;

import routines.system.Dynamic;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
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
import org.talend.sdk.component.runtime.record.MappingUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiRowStructVisitor {

    private RecordBuilderFactory factory;

    private Builder recordBuilder;

    private final Jsonb jsonb = JsonbProvider.provider().create().build();

    private Set<String> allowedFields;

    public void visit(final Object data) {
        log.debug("[visit] Class: {} ==> {}.", data.getClass().getName(), data);
        Arrays.stream(data.getClass().getFields()).forEach(field -> {
            try {
                final Class<?> type = field.getType();
                final String name = field.getName();
                final Object raw = field.get(data);
                log.debug("[visit] Field {} ({}) ==> {}.", name, type.getName(), raw);
                if (raw == null) {
                    log.debug("[visit] Skipping field {} with null value.", name);
                    return;
                }
                if (!allowedFields.contains(name)) {
                    log.debug("[visit] Skipping technical field {}.", name);
                    return;
                }
                switch (type.getName()) {
                case "java.lang.Object":
                    onObject(name, raw);
                    break;
                case "java.lang.String":
                    onString(name, raw);
                    break;
                case "java.math.BigDecimal":
                    onString(name, BigDecimal.class.cast(raw).toString());
                    break;
                case "java.lang.Integer":
                case "int":
                case "java.lang.Short":
                case "short":
                    onInt(name, raw);
                    break;
                case "java.lang.Long":
                case "long":
                    onLong(name, raw);
                    break;
                case "java.lang.Float":
                case "float":
                    onFloat(name, raw);
                    break;
                case "java.lang.Double":
                case "double":
                    onDouble(name, raw);
                    break;
                case "java.lang.Boolean":
                case "boolean":
                    onBoolean(name, raw);
                    break;
                case "java.util.Date":
                    onDatetime(name, Date.class.cast(raw).toInstant().atZone(UTC));
                    break;
                case "routines.system.Dynamic":
                    final Dynamic dynamic = Dynamic.class.cast(raw);
                    dynamic.metadatas.forEach(meta -> {
                        final Object value = dynamic.getColumnValue(meta.getName());
                        final String metaName = sanitizeConnectionName(meta.getName());
                        final String metaOriginalName = meta.getDbName();
                        final String metaComment = meta.getDescription();
                        final boolean metaIsNullable = meta.isNullable();
                        log.debug("[visit] Dynamic {}\t({})\t ==> {}.", meta.getName(), meta.getType(), value);
                        if (value == null) {
                            return;
                        }
                        switch (meta.getType()) {
                        case "id_Object":
                            onObject(metaName, value);
                            break;
                        case "id_List":
                            onArray(toCollectionEntry(metaName, metaOriginalName, value), Collection.class.cast(value));
                            break;
                        case "id_String":
                        case "id_Character":
                            onString(metaName, value);
                            break;
                        case "id_byte[]":
                            final byte[] bytes;
                            if (byte[].class.isInstance(value)) {
                                bytes = byte[].class.cast(value);
                            } else if (ByteBuffer.class.isInstance(value)) {
                                bytes = ByteBuffer.class.cast(value).array();
                            } else {
                                log
                                        .warn("[visit] '{}' of type `id_byte[]` and content is contained in `{}`:"
                                                + " This should not happen! "
                                                + " Wrapping `byte[]` from `String.valueOf()`: result may be inaccurate.",
                                                metaName, value.getClass().getSimpleName());
                                bytes = ByteBuffer.wrap(String.valueOf(value).getBytes()).array();
                            }
                            onBytes(metaName, bytes);
                            break;
                        case "id_Byte":
                        case "id_Short":
                        case "id_Integer":
                            onInt(metaName, value);
                            break;
                        case "id_Long":
                            onLong(metaName, value);
                            break;
                        case "id_Float":
                            onFloat(metaName, value);
                            break;
                        case "id_Double":
                            onDouble(metaName, value);
                            break;
                        case "id_BigDecimal":
                            onString(metaName, BigDecimal.class.cast(value).toString());
                            break;
                        case "id_Boolean":
                            onBoolean(metaName, value);
                            break;
                        case "id_Date":
                            final ZonedDateTime dateTime;
                            if (Long.class.isInstance(value)) {
                                dateTime = ZonedDateTime.ofInstant(ofEpochMilli(Long.class.cast(value)), UTC);
                            } else {
                                dateTime = ZonedDateTime.ofInstant(Date.class.cast(value).toInstant(), UTC);
                            }
                            onDatetime(metaName, dateTime);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + meta.getType());
                        }
                    });
                    break;
                default:
                    if (byte[].class.isInstance(raw)) {
                        onBytes(name, byte[].class.cast(raw));
                    } else if (byte.class.isInstance(raw) || Byte.class.isInstance(raw)) {
                        onInt(name, Byte.class.cast(raw).intValue());
                    } else if (Collection.class.isInstance(raw)) {
                        final Collection collection = Collection.class.cast(raw);
                        onArray(toCollectionEntry(name, "", collection), Collection.class.cast(collection));
                    } else if (char.class.isInstance(raw) || Character.class.isInstance(raw)) {
                        onString(name, String.valueOf(raw));
                    } else {
                        throw new IllegalAccessException(String.format("Invalid type: %s with value: %s.", type, raw));
                    }
                    break;
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public Record get(final Object data, final RecordBuilderFactory factory) {
        if (recordBuilder == null) {
            this.factory = factory;
            recordBuilder = factory.newRecordBuilder(inferSchema(data, factory));
        }
        visit(data);
        return recordBuilder.build();
    }

    private Schema inferSchema(final Object data, final RecordBuilderFactory factory) {
        // all standard rowStruct fields have accessors, not technical fields.
        allowedFields = Arrays
                .stream(data.getClass().getDeclaredMethods())
                .map(method -> method.getName())
                .filter(m -> m.matches("^(get|is).*"))
                .map(n -> n.replaceAll("^(get|is)", ""))
                .map(n -> n.substring(0, 1).toLowerCase(Locale.ROOT) + n.substring(1))
                .collect(Collectors.toSet());
        final Schema.Builder schema = factory.newSchemaBuilder(RECORD);
        Arrays.stream(data.getClass().getFields()).forEach(field -> {
            try {
                final Class<?> type = field.getType();
                final String name = field.getName();
                final Object raw = field.get(data);
                if (!allowedFields.contains(name)) {
                    log.debug("[inferSchema] Skipping technical field {}.", name);
                    return;
                }
                switch (type.getName()) {
                case "java.util.List":
                    schema.withEntry(toCollectionEntry(name, "", raw));
                    break;
                case "java.lang.Object":
                case "java.lang.String":
                case "java.lang.Character":
                case "char":
                case "java.math.BigDecimal":
                    schema.withEntry(toEntry(name, STRING));
                    break;
                case "java.lang.Integer":
                case "int":
                case "java.lang.Short":
                case "short":
                case "java.lang.Byte":
                case "byte":
                    schema.withEntry(toEntry(name, INT));
                    break;
                case "java.lang.Long":
                case "long":
                    schema.withEntry(toEntry(name, LONG));
                    break;
                case "java.lang.Float":
                case "float":
                    schema.withEntry(toEntry(name, FLOAT));
                    break;
                case "java.lang.Double":
                case "double":
                    schema.withEntry(toEntry(name, DOUBLE));
                    break;
                case "java.lang.Boolean":
                case "boolean":
                    schema.withEntry(toEntry(name, BOOLEAN));
                    break;
                case "java.util.Date":
                    schema.withEntry(toEntry(name, DATETIME));
                    break;
                case "byte[]":
                case "[B":
                    schema.withEntry(toEntry(name, BYTES));
                    break;
                case "routines.system.Dynamic":
                    final Dynamic dynamic = Dynamic.class.cast(raw);
                    dynamic.metadatas.forEach(meta -> {
                        final Object value = dynamic.getColumnValue(meta.getName());
                        final String metaName = sanitizeConnectionName(meta.getName());
                        final String metaOriginalName = meta.getDbName();
                        final String metaComment = meta.getDescription();
                        final boolean metaIsNullable = meta.isNullable();
                        log.debug("[inferSchema] Dynamic {}\t({})\t ==> {}.", meta.getName(), meta.getType(), value);
                        switch (meta.getType()) {
                        case "id_List":
                            schema.withEntry(toCollectionEntry(metaName, metaOriginalName, value));
                            break;
                        case "id_Object":
                        case "id_String":
                        case "id_Character":
                        case "id_BigDecimal":
                            schema.withEntry(toEntry(metaName, STRING, metaOriginalName, metaIsNullable, metaComment));
                            break;
                        case "id_byte[]":
                            schema.withEntry(toEntry(metaName, BYTES, metaOriginalName, metaIsNullable, metaComment));
                            break;
                        case "id_Byte":
                        case "id_Short":
                        case "id_Integer":
                            schema.withEntry(toEntry(metaName, INT, metaOriginalName, metaIsNullable, metaComment));
                            break;
                        case "id_Long":
                            schema.withEntry(toEntry(metaName, LONG, metaOriginalName, metaIsNullable, metaComment));
                            break;
                        case "id_Float":
                            schema.withEntry(toEntry(metaName, FLOAT, metaOriginalName, metaIsNullable, metaComment));
                            break;
                        case "id_Double":
                            schema.withEntry(toEntry(metaName, DOUBLE, metaOriginalName, metaIsNullable, metaComment));
                            break;
                        case "id_Boolean":
                            schema.withEntry(toEntry(metaName, BOOLEAN, metaOriginalName, metaIsNullable, metaComment));
                            break;
                        case "id_Date":
                            schema
                                    .withEntry(
                                            toEntry(metaName, DATETIME, metaOriginalName, metaIsNullable, metaComment));
                            break;
                        default:
                            schema.withEntry(toEntry(metaName, STRING, metaOriginalName, metaIsNullable, metaComment));
                        }
                    });
                    break;
                default:
                    log.warn("Unmanaged type: {} for {}.", type, name);
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
        return schema.build();
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

    private void onDatetime(final String name, final ZonedDateTime value) {
        recordBuilder.withDateTime(name, value);
    }

    private void onBytes(final String name, final byte[] value) {
        recordBuilder.withBytes(name, value);
    }

    private void onArray(final Entry entry, final Collection value) {
        recordBuilder.withArray(entry, value);
    }

    private void onObject(final String name, final Object value) {
        recordBuilder.withString(name, jsonb.toJson(value));
    }

    private Entry toEntry(final String name, final Schema.Type type) {
        return factory.newEntryBuilder().withName(name).withNullable(true).withType(type).build();
    }

    private Entry toEntry(final String name, final Schema.Type type, final String originalName,
            final boolean isNullable, final String comment) {
        return factory
                .newEntryBuilder()
                .withName(name)
                .withRawName(originalName)
                .withNullable(isNullable)
                .withType(type)
                .withComment(comment)
                .build();
    }

    private Entry toCollectionEntry(final String name, final String originalName, final Object value) {
        Type elementType = STRING;
        if (value != null && !Collection.class.cast(value).isEmpty()) {
            final Object coll = Collection.class.cast(value).iterator().next();
            elementType = getTypeFromValue(coll);
        }
        return factory
                .newEntryBuilder()
                .withName(name)
                .withRawName(originalName)
                .withNullable(true)
                .withType(ARRAY)
                .withElementSchema(factory.newSchemaBuilder(elementType).build())
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
            return STRING;
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
