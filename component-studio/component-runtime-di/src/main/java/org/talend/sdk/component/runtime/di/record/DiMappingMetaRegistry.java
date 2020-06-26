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
import routines.system.DynamicMetadata;
import routines.system.DynamicMetadata.sourceTypes;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.dynamic.DynamicHelper;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMeta;
import org.talend.sdk.component.runtime.record.RecordConverters.MappingMetaRegistry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * This class overides the component-runtime-impl and provides the needed provisioning for dynamic columns.
 * Its scope is runtime Studio only.
 */
public class DiMappingMetaRegistry extends MappingMetaRegistry {

    @Override
    public MappingMeta find(final Class<?> parameterType, final Supplier<RecordBuilderFactory> factorySupplier) {
        final MappingMeta meta = registry.get(parameterType);
        if (meta != null) {
            return meta;
        }
        final MappingMeta mappingMeta = new MappingMeta(parameterType, this, factorySupplier,
                this::dynamicInstanceProvisioner, this::dynamicRecordProvisioner);
        final MappingMeta existing = registry.putIfAbsent(parameterType, mappingMeta);
        if (existing != null) {
            return existing;
        }
        return mappingMeta;
    }

    BiConsumer<Object, Record> dynamicInstanceProvisioner(final Field field, final String name) {
        return (instance, record) -> {
            final Record rcd = record.getOptionalRecord(name).orElse(null);
            final Dynamic dynamic = new Dynamic();
            if (rcd != null) {
                rcd.getSchema().getEntries().forEach(entry -> {
                    final DynamicMetadata metadata = new DynamicMetadata();
                    metadata.setName(entry.getName());
                    metadata.setDbName(entry.getOriginalFieldName());
                    metadata.setNullable(entry.isNullable());
                    metadata.setDescription(entry.getComment());
                    metadata.setKey(false);
                    metadata.setSourceType(sourceTypes.unknown);
                    metadata.setLength(100);
                    metadata.setPrecision(0);
                    switch (entry.getType()) {
                    case RECORD:
                        metadata.setType("id_Object");
                        break;
                    case ARRAY:
                        metadata.setType("id_List");
                        break;
                    case STRING:
                        metadata.setType("id_String");
                        break;
                    case BYTES:
                        metadata.setType("id_Byte");
                        break;
                    case INT:
                        metadata.setType("id_Integer");
                        break;
                    case LONG:
                        metadata.setType("id_Long");
                        break;
                    case FLOAT:
                        metadata.setType("id_Float");
                        metadata.setLength(10);
                        metadata.setPrecision(5);
                        break;
                    case DOUBLE:
                        metadata.setType("id_Double");
                        metadata.setLength(20);
                        metadata.setPrecision(10);
                        break;
                    case BOOLEAN:
                        metadata.setType("id_Boolean");
                        break;
                    case DATETIME:
                        metadata.setType("id_Date");
                        metadata.setLogicalType("timestamp-millis");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + entry.getType());
                    }
                    dynamic.metadatas.add(metadata);
                    dynamic.addColumnValue(rcd.get(Object.class, entry.getName()));
                });
            }
            try {
                field.set(instance, dynamic);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    BiConsumer<Record.Builder, Object> dynamicRecordProvisioner(final Field field, final RecordBuilderFactory factory) {
        return (builder, instance) -> {
            try {
                final Dynamic dynamic = Dynamic.class.cast(field.get(instance));
                final Builder dynRecordBuilder = factory.newRecordBuilder();
                dynamic.metadatas.forEach(meta -> {
                    final Object value = dynamic.getColumnValue(meta.getName());
                    log.debug("[dynamicRecordProvisioner] {}\t({})\t ==> {}.", meta.getName(), meta.getType(), value);
                    final Entry.Builder entry = factory
                            .newEntryBuilder()
                            .withName(meta.getName())
                            .withRawName(meta.getDbName())
                            .withNullable(meta.isNullable())
                            .withComment(meta.getDescription());
                    switch (meta.getType()) {
                    case "id_Object":
                        entry.withType(RECORD);
                        dynRecordBuilder.withRecord(entry.build(), Record.class.cast(value));
                        break;
                    case "id_List":
                        entry.withType(ARRAY);
                        final Collection ary = Collection.class.cast(value);
                        entry.withElementSchema(RecordConverters.toSchema(factory, ary));
                        dynRecordBuilder.withArray(entry.build(), ary);
                        break;
                    case "id_String":
                        entry.withType(STRING);
                        dynRecordBuilder.withString(entry.build(), String.class.cast(value));
                        break;
                    case "id_Byte":
                        entry.withType(BYTES);
                        final byte[] bytes = value != null ? ((java.nio.ByteBuffer) value).array() : null;
                        dynRecordBuilder.withBytes(entry.build(), bytes);
                        break;
                    case "id_Integer":
                        entry.withType(INT);
                        dynRecordBuilder.withInt(entry.build(), Integer.class.cast(value));
                        break;
                    case "id_Long":
                        entry.withType(LONG);
                        dynRecordBuilder.withLong(entry.build(), Long.class.cast(value));
                        break;
                    case "id_Float":
                        entry.withType(FLOAT);
                        dynRecordBuilder.withFloat(entry.build(), Float.class.cast(value));
                        break;
                    case "id_Double":
                        entry.withType(DOUBLE);
                        dynRecordBuilder.withDouble(entry.build(), Double.class.cast(value));
                        break;
                    case "id_Boolean":
                        entry.withType(BOOLEAN);
                        dynRecordBuilder.withBoolean(entry.build(), Boolean.class.cast(value));
                        break;
                    case "id_Date":
                        entry.withType(DATETIME);
                        final long millis = Long.class.cast(value);
                        dynRecordBuilder
                                .withDateTime(entry.build(), ZonedDateTime.ofInstant(ofEpochMilli(millis), UTC));
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + meta.getType());
                    }
                });
                final Record dynamicRecord = dynRecordBuilder.build();
                builder
                        .withRecord(factory
                                .newEntryBuilder()
                                .withName(field.getName() + DynamicHelper.DYNAMIC_MARKER)
                                .withRawName(field.getName())
                                .withNullable(true)
                                .withType(RECORD)
                                .withElementSchema(dynamicRecord.getSchema())
                                .build(), dynamicRecord);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        };
    }

}
