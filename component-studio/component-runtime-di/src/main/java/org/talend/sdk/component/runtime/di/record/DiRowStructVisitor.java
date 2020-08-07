/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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

import routines.system.Dynamic;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiRowStructVisitor {

    private RecordBuilderFactory factory;

    private Builder recordBuilder;

    public void visit(final Object data) {
        log.debug("[visit] Class: {} ==> {}.", data.getClass().getName(), data);
        Arrays.stream(data.getClass().getFields()).forEach(field -> {
            try {
                final Class<?> type = field.getType();
                final String name = field.getName();
                final Object raw = field.get(data);
                log.debug("[visit] Field {} ({}) ==> {}.", name, type.getName(), raw);
                if (raw == null) {
                    log.debug("[visit] Skipping Field {} with null value.", name);
                    return;
                }
                switch (type.getName()) {
                case "java.lang.String":
                    onString(toEntry(name, STRING), raw);
                    break;
                case "java.math.BigDecimal":
                    onDouble(toEntry(name, DOUBLE), BigDecimal.class.cast(raw).doubleValue());
                    break;
                case "java.lang.Integer":
                case "int":
                case "java.lang.Short":
                case "short":
                    onInt(toEntry(name, INT), raw);
                    break;
                case "java.lang.Long":
                case "long":
                    onLong(toEntry(name, LONG), raw);
                    break;
                case "java.lang.Float":
                case "float":
                    onFloat(toEntry(name, FLOAT), raw);
                    break;
                case "java.lang.Double":
                case "double":
                    onDouble(toEntry(name, DOUBLE), raw);
                    break;
                case "java.lang.Boolean":
                case "boolean":
                    onBoolean(toEntry(name, BOOLEAN), raw);
                    break;
                case "java.util.Date":
                    onDatetime(toEntry(name, DATETIME), Date.class.cast(raw).toInstant().atZone(UTC));
                    break;
                case "routines.system.Dynamic":
                    Dynamic dynamic = Dynamic.class.cast(raw);
                    dynamic.metadatas.forEach(meta -> {
                        final Object value = dynamic.getColumnValue(meta.getName());
                        final String metaName = meta.getName();
                        final String metaOriginalName = meta.getDbName();
                        final String metaComment = meta.getDescription();
                        final boolean metaIsNullable = meta.isNullable();
                        log.debug("[visit] Dynamic {}\t({})\t ==> {}.", meta.getName(), meta.getType(), value);
                        if (value == null) {
                            return;
                        }
                        switch (meta.getType()) {
                        case "id_Object":
                            onString(toEntry(metaName, metaOriginalName, RECORD, metaIsNullable, metaComment), value);
                            break;
                        case "id_List":
                            onArray(toCollectionEntry(metaName, metaOriginalName, value), Collection.class.cast(value));
                            break;
                        case "id_String":
                        case "id_Character":
                            onString(toEntry(metaName, metaOriginalName, STRING, metaIsNullable, metaComment), value);
                            break;
                        case "id_byte[]":
                            final byte[] bytes =
                                    value != null ? Base64.getDecoder().decode(String.valueOf(value)) : null;
                            onBytes(toEntry(metaName, metaOriginalName, BYTES, metaIsNullable, metaComment), bytes);
                            break;
                        case "id_Byte":
                        case "id_Short":
                        case "id_Integer":
                            onInt(toEntry(metaName, metaOriginalName, INT, metaIsNullable, metaComment), value);
                            break;
                        case "id_Long":
                            onLong(toEntry(metaName, metaOriginalName, LONG, metaIsNullable, metaComment), value);
                            break;
                        case "id_Float":
                            onFloat(toEntry(metaName, metaOriginalName, FLOAT, metaIsNullable, metaComment), value);
                            break;
                        case "id_Double":
                            onDouble(toEntry(metaName, metaOriginalName, DOUBLE, metaIsNullable, metaComment), value);
                            break;
                        case "id_BigDecimal":
                            onDouble(toEntry(metaName, metaOriginalName, DOUBLE, metaIsNullable, metaComment),
                                    BigDecimal.class.cast(value).doubleValue());
                            break;
                        case "id_Boolean":
                            onBoolean(toEntry(metaName, metaOriginalName, BOOLEAN, metaIsNullable, metaComment), value);
                            break;
                        case "id_Date":
                            ZonedDateTime dateTime;
                            if (Long.class.isInstance(value)) {
                                dateTime = ZonedDateTime.ofInstant(ofEpochMilli(Long.class.cast(value)), UTC);
                            } else {
                                dateTime = ZonedDateTime.ofInstant(Date.class.cast(value).toInstant(), UTC);
                            }
                            onDatetime(toEntry(metaName, metaOriginalName, DATETIME, metaIsNullable, metaComment),
                                    dateTime);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + meta.getType());
                        }
                    });
                    break;
                default:
                    if (byte[].class.isInstance(raw)) {
                        onBytes(toEntry(name, BYTES), byte[].class.cast(raw));
                    } else if (byte.class.isInstance(raw) || Byte.class.isInstance(raw)) {
                        onInt(toEntry(name, INT), Byte.class.cast(raw).intValue());
                    } else if (Collection.class.isInstance(raw)) {
                        final Collection collection = Collection.class.cast(raw);
                        onArray(toCollectionEntry(name, "", collection), Collection.class.cast(collection));
                    } else if (char.class.isInstance(raw) || Character.class.isInstance(raw)) {
                        onString(toEntry(name, STRING), String.valueOf(raw));
                    } else {
                        throw new IllegalAccessException(String.format("Invalid type: % with value: %s.", type, raw));
                    }
                    break;
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public Record get(final Object data, final RecordBuilderFactory factory) {
        this.factory = factory;
        recordBuilder = factory.newRecordBuilder(inferSchema(data, factory));
        visit(data);
        return recordBuilder.build();
    }

    private Schema inferSchema(final Object data, final RecordBuilderFactory factory) {
        final Schema.Builder schema = factory.newSchemaBuilder(RECORD);
        Arrays.stream(data.getClass().getFields()).forEach(field -> {
            try {
                final Class<?> type = field.getType();
                final String name = field.getName();
                final Object raw = field.get(data);
                switch (type.getName()) {
                case "java.lang.String":
                case "java.lang.Character":
                case "char":
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
                case "java.math.BigDecimal":
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
                    Dynamic dynamic = Dynamic.class.cast(raw);
                    dynamic.metadatas.forEach(meta -> {
                        final Object value = dynamic.getColumnValue(meta.getName());
                        final String metaName = meta.getName();
                        log.debug("[visit] Dynamic {}\t({})\t ==> {}.", meta.getName(), meta.getType(), value);
                        switch (meta.getType()) {
                        case "id_Object":
                            schema.withEntry(toEntry(metaName, RECORD));
                            break;
                        case "id_List":
                            schema.withEntry(toCollectionEntry(metaName, "", value));
                            break;
                        case "id_String":
                        case "id_Character":
                            schema.withEntry(toEntry(metaName, STRING));
                            break;
                        case "id_byte[]":
                            schema.withEntry(toEntry(metaName, BYTES));
                            break;
                        case "id_Byte":
                        case "id_Short":
                        case "id_Integer":
                            schema.withEntry(toEntry(metaName, INT));
                            break;
                        case "id_Long":
                            schema.withEntry(toEntry(metaName, LONG));
                            break;
                        case "id_Float":
                            schema.withEntry(toEntry(metaName, FLOAT));
                            break;
                        case "id_Double":
                        case "id_BigDecimal":
                            schema.withEntry(toEntry(metaName, DOUBLE));
                            break;
                        case "id_Boolean":
                            schema.withEntry(toEntry(metaName, BOOLEAN));
                            break;
                        case "id_Date":
                            schema.withEntry(toEntry(metaName, DATETIME));
                            break;
                        default:
                            schema.withEntry(toEntry(metaName, STRING));
                        }
                    });
                    break;
                default:
                    if (Collection.class.isInstance(raw)) {
                        schema.withEntry(toCollectionEntry(name, "", raw));
                    } else {
                        log.warn("unmanaged type: {} for {}.", type, name);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
        return schema.build();
    }

    private void onInt(final Entry entry, final Object value) {
        recordBuilder.withInt(entry, Integer.class.cast(MappingUtils.coerce(Integer.class, value, entry.getName())));
    }

    private void onLong(final Entry entry, final Object value) {
        recordBuilder.withLong(entry, Long.class.cast(MappingUtils.coerce(Long.class, value, entry.getName())));
    }

    private void onFloat(final Entry entry, final Object value) {
        recordBuilder.withFloat(entry, Float.class.cast(MappingUtils.coerce(Float.class, value, entry.getName())));
    }

    private void onDouble(final Entry entry, final Object value) {
        recordBuilder.withDouble(entry, Double.class.cast(MappingUtils.coerce(Double.class, value, entry.getName())));
    }

    private void onBoolean(final Entry entry, final Object value) {
        recordBuilder
                .withBoolean(entry, Boolean.class.cast(MappingUtils.coerce(Boolean.class, value, entry.getName())));
    }

    private void onString(final Entry entry, final Object value) {
        recordBuilder.withString(entry, String.class.cast(MappingUtils.coerce(String.class, value, entry.getName())));
    }

    private void onDatetime(final Entry entry, final ZonedDateTime value) {
        recordBuilder.withDateTime(entry, value);
    }

    private void onBytes(final Entry entry, final byte[] value) {
        recordBuilder.withBytes(entry, value);
    }

    private void onArray(final Entry entry, final Collection array) {
        recordBuilder.withArray(entry, array);
    }

    private Entry toEntry(final String name, final Schema.Type type) {
        return factory.newEntryBuilder().withName(name).withNullable(true).withType(type).build();
    }

    private Entry toEntry(final String name, final String originalName, final Schema.Type type,
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
            Object coll = Collection.class.cast(value).iterator().next();
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
            return DOUBLE;
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
