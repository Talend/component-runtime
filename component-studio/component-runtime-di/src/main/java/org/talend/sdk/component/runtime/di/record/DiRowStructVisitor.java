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
import static org.talend.sdk.component.api.record.Schema.Type.STRING;

import routines.system.Dynamic;

import java.math.BigDecimal;
import java.time.ZoneId;
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
                switch (type.getName()) {
                case "java.lang.String":
                case "java.lang.BigDecimal":
                    onString(toEntry(name, "", STRING), String.class.cast(raw));
                    break;
                case "java.lang.Integer":
                case "java.lang.Short":
                case "int":
                case "short":
                    onInt(toEntry(name, "", INT), Integer.class.cast(raw));
                    break;
                case "java.lang.Long":
                case "long":
                    onLong(toEntry(name, "", LONG), Long.class.cast(raw));
                    break;
                case "java.lang.Float":
                case "float":
                    onFloat(toEntry(name, "", FLOAT), Float.class.cast(raw));
                    break;
                case "java.lang.Double":
                case "double":
                    onDouble(toEntry(name, "", DOUBLE), Double.class.cast(raw));
                    break;
                case "java.lang.Boolean":
                case "boolean":
                    onBoolean(toEntry(name, "", BOOLEAN), Boolean.class.cast(raw));
                    break;
                case "java.util.Date":
                    onDatetime(toEntry(name, "", DATETIME),
                            Date.class.cast(raw).toInstant().atZone(ZoneId.systemDefault()));
                    break;
                case "routines.system.Dynamic":
                    Dynamic dynamic = Dynamic.class.cast(raw);
                    dynamic.metadatas.forEach(meta -> {
                        final Object value = dynamic.getColumnValue(meta.getName());
                        final String metaName = meta.getName();
                        final String metaOriginalName = meta.getDbName();
                        log.debug("[visit] Dynamic {}\t({})\t ==> {}.", meta.getName(), meta.getType(), value);
                        switch (meta.getType()) {
                        case "id_Object":
                            onString(toEntry(metaName, metaOriginalName, Type.RECORD), String.class.cast(value));
                            break;
                        case "id_List":
                            onArray(getCollectionEntry(metaName, metaOriginalName, value),
                                    Collection.class.cast(value));
                            break;
                        case "id_String":
                            onString(toEntry(metaName, metaOriginalName, STRING), String.class.cast(value));
                            break;
                        case "id_Byte":
                            final byte[] bytes =
                                    value != null ? Base64.getDecoder().decode(String.valueOf(value)) : null;
                            onBytes(toEntry(metaName, metaOriginalName, BYTES), bytes);
                            break;
                        case "id_Short":
                        case "id_Integer":
                            onInt(toEntry(metaName, metaOriginalName, INT), Integer.class.cast(value));
                            break;
                        case "id_Long":
                            onLong(toEntry(metaName, metaOriginalName, LONG), Long.class.cast(value));
                            break;
                        case "id_Float":
                            onFloat(toEntry(metaName, metaOriginalName, FLOAT), Float.class.cast(value));
                            break;
                        case "id_Double":
                            onDouble(toEntry(metaName, metaOriginalName, DOUBLE), Double.class.cast(value));
                            break;
                        case "id_BigDecimal":
                            onString(toEntry(metaName, metaOriginalName, STRING), String.class.cast(value));
                            break;
                        case "id_Boolean":
                            onBoolean(toEntry(metaName, metaOriginalName, BOOLEAN), Boolean.class.cast(value));
                            break;
                        case "id_Date":
                            ZonedDateTime dateTime = ZonedDateTime.ofInstant(ofEpochMilli(Long.class.cast(value)), UTC);
                            onDatetime(toEntry(metaName, metaOriginalName, DATETIME), dateTime);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + meta.getType());
                        }
                    });
                    break;
                default:
                    if (byte[].class.isInstance(type)) {
                        onBytes(toEntry(name, "", BYTES), byte[].class.cast(raw));
                    } else if (Collection.class.isInstance(type)) {
                        final Collection collection = Collection.class.cast(type);
                        onArray(getCollectionEntry(name, "", collection), Collection.class.cast(collection));
                    } else {
                        throw new IllegalAccessException();
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
        recordBuilder = factory.newRecordBuilder();
        visit(data);
        return recordBuilder.build();
    }

    private void onInt(final Entry entry, final int value) {
        recordBuilder.withInt(entry, value);
    }

    private void onLong(final Entry entry, final long value) {
        recordBuilder.withLong(entry, value);
    }

    private void onFloat(final Entry entry, final float value) {
        recordBuilder.withFloat(entry, value);
    }

    private void onDouble(final Entry entry, final double value) {
        recordBuilder.withDouble(entry, value);
    }

    private void onBoolean(final Entry entry, final Boolean value) {
        recordBuilder.withBoolean(entry, value);
    }

    private void onString(final Entry entry, final String value) {
        recordBuilder.withString(entry, value);
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

    private Entry toEntry(final String name, final String originalName, final Schema.Type type) {
        return factory
                .newEntryBuilder()
                .withName(name)
                .withRawName(originalName)
                .withNullable(true)
                .withType(type)
                .build();
    }

    private Entry getCollectionEntry(final String name, final String originalName, final Object value) {
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
