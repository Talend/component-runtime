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
package org.talend.sdk.component.runtime.record;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.Data;

@Data
public class RecordBuilderFactoryImpl implements RecordBuilderFactory, Serializable {

    protected final String plugin;

    @Override
    public Schema.Builder newSchemaBuilder(final Schema.Type type) {
        switch (type) {
        case RECORD:
        case ARRAY:
            return new SchemaImpl.BuilderImpl().withType(type);
        default:
            return Schemas.valueOf(type.name());
        }
    }

    @Override
    public Schema.Builder newSchemaBuilder(final Schema schema) {
        final Schema.Builder builder = newSchemaBuilder(schema.getType());
        switch (schema.getType()) {
        case RECORD:
            schema.getAllEntries().forEach(builder::withEntry);
            break;
        case ARRAY:
            builder.withElementSchema(schema.getElementSchema());
            break;
        default:
        }
        return builder;
    }

    @Override
    public Record.Builder newRecordBuilder(final Schema schema, final Record record) {
        final Record.Builder builder = newRecordBuilder(schema);
        final Map<String, Schema.Entry> entriesIndex =
                schema.getAllEntries().collect(toMap(Schema.Entry::getName, identity()));
        record.getSchema().getAllEntries().filter(e -> entriesIndex.containsKey(e.getName())).forEach(entry -> {
            switch (entry.getType()) {
            case STRING:
                record
                        .getOptionalString(entry.getName())
                        .ifPresent(v -> builder.withString(entriesIndex.get(entry.getName()), v));
                break;
            case LONG:
                record
                        .getOptionalLong(entry.getName())
                        .ifPresent(v -> builder.withLong(entriesIndex.get(entry.getName()), v));
                break;
            case INT:
                record
                        .getOptionalInt(entry.getName())
                        .ifPresent(v -> builder.withInt(entriesIndex.get(entry.getName()), v));
                break;
            case DOUBLE:
                record
                        .getOptionalDouble(entry.getName())
                        .ifPresent(v -> builder.withDouble(entriesIndex.get(entry.getName()), v));
                break;
            case FLOAT:
                record
                        .getOptionalFloat(entry.getName())
                        .ifPresent(v -> builder.withFloat(entriesIndex.get(entry.getName()), (float) v));
                break;
            case BOOLEAN:
                record
                        .getOptionalBoolean(entry.getName())
                        .ifPresent(v -> builder.withBoolean(entriesIndex.get(entry.getName()), v));
                break;
            case BYTES:
                record
                        .getOptionalBytes(entry.getName())
                        .ifPresent(v -> builder.withBytes(entriesIndex.get(entry.getName()), v));
                break;
            case DATETIME:
                record
                        .getOptionalDateTime(entry.getName())
                        .ifPresent(v -> builder.withDateTime(entriesIndex.get(entry.getName()), v));
                break;
            case RECORD:
                record
                        .getOptionalRecord(entry.getName())
                        .ifPresent(v -> builder.withRecord(entriesIndex.get(entry.getName()), v));
                break;
            case ARRAY:
                record
                        .getOptionalArray(Object.class, entry.getName())
                        .ifPresent(v -> builder.withArray(entriesIndex.get(entry.getName()), v));
                break;
            default:
                throw new IllegalArgumentException("Unsupported entry type: " + entry);
            }
        });
        return builder;
    }

    @Override
    public Record.Builder newRecordBuilder(final Schema schema) {
        return new RecordImpl.BuilderImpl(schema);
    }

    @Override
    public Record.Builder newRecordBuilder() {
        return new RecordImpl.BuilderImpl();
    }

    @Override
    public Schema.Entry.Builder newEntryBuilder() {
        return new Schema.Entry.Builder();
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, RecordBuilderFactory.class.getName());
    }
}
