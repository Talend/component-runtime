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
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.avro.Schema.Type.NULL;
import static org.apache.avro.Schema.Type.UNION;
import static org.talend.sdk.component.api.record.Schema.sanitizeConnectionName;
import static org.talend.sdk.component.runtime.beam.avro.AvroSchemas.unwrapUnion;
import static org.talend.sdk.component.runtime.beam.spi.record.SchemaIdGenerator.generateRecordName;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.json.bind.annotation.JsonbTransient;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.util.Utf8;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;
import org.talend.sdk.component.runtime.record.RecordConverters;

public class AvroRecord implements Record, AvroPropertyMapper, Unwrappable {

    private static final RecordConverters RECORD_CONVERTERS = new RecordConverters();

    private static final org.apache.avro.Schema NULL_SCHEMA = org.apache.avro.Schema.create(NULL);

    @JsonbTransient
    private final IndexedRecord delegate;

    @JsonbTransient
    private final AvroSchema schema;

    public AvroRecord(final IndexedRecord record) {
        schema = new AvroSchema(record.getSchema());
        delegate = record;
        // dirty fix for Avro DateTime related logicalTypes converted to org.joda.time.DateTime
        delegate
                .getSchema()
                .getFields()
                .stream()
                .filter(f -> org.joda.time.DateTime.class.isInstance(delegate.get(f.pos())))
                .forEach(f -> delegate
                        .put(f.pos(), org.joda.time.DateTime.class.cast(delegate.get(f.pos())).getMillis()));
    }

    public AvroRecord(final Record record) {
        if (record instanceof AvroRecord) {
            final AvroRecord avr = (AvroRecord) record;
            this.delegate = avr.delegate;
            this.schema = avr.schema;
            return;
        }
        final List<org.apache.avro.Schema.Field> fields = record.getSchema().getAllEntries().map(entry -> {
            final org.apache.avro.Schema avroSchema = toSchema(entry);
            final org.apache.avro.Schema.Field f = AvroSchemaBuilder.AvroHelper.toField(avroSchema, entry);
            return f;
        }).collect(toList());
        final org.apache.avro.Schema avroSchema =
                org.apache.avro.Schema.createRecord(generateRecordName(fields), null, null, false);
        record.getSchema().getProps().forEach((k, v) -> avroSchema.addProp(k, v));
        avroSchema.setFields(fields);
        schema = new AvroSchema(avroSchema);
        delegate = new GenericData.Record(avroSchema);
        record
                .getSchema()
                .getAllEntries()
                .forEach(entry -> ofNullable(record.get(Object.class, sanitizeConnectionName(entry.getName())))
                        .ifPresent(v -> {
                            final Object avroValue = directMapping(v);

                            if (avroValue != null) {
                                final org.apache.avro.Schema.Field field =
                                        avroSchema.getField(sanitizeConnectionName(entry.getName()));
                                delegate.put(field.pos(), avroValue);
                            }
                        }));
    }

    private Object directMapping(final Object value) {
        if (Collection.class.isInstance(value)) {
            return Collection.class.cast(value).stream().map(this::directMapping).collect(toList());
        }
        if (Record.class.isInstance(value)) {
            return Unwrappable.class.cast(value).unwrap(IndexedRecord.class);
        }
        if (ZonedDateTime.class.isInstance(value)) {
            return ZonedDateTime.class.cast(value).toInstant().toEpochMilli();
        }
        if (Date.class.isInstance(value)) {
            return Date.class.cast(value).getTime();
        }
        if (byte[].class.isInstance(value)) {
            return ByteBuffer.wrap(byte[].class.cast(value));
        }
        return value;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    @Override
    public <T> T get(final Class<T> expectedType, final String name) {
        if (expectedType == Collection.class) {
            return expectedType.cast(getArray(Object.class, name));
        }
        return doGet(expectedType, name);
    }

    @Override
    public <T> Collection<T> getArray(final Class<T> type, final String name) {
        final Collection<?> collection = doGet(Collection.class, name);
        if (collection == null) {
            return null;
        }
        final org.apache.avro.Schema elementType =
                unwrapUnion(delegate.getSchema().getField(name).schema()).getElementType();
        return doMapCollection(type, collection, elementType);
    }

    @Override
    public <T> T unwrap(final Class<T> type) {
        if (IndexedRecord.class.isAssignableFrom(type)) {
            return type.cast(delegate);
        }
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AvroRecord that = AvroRecord.class.cast(o);
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    private <T> Collection<T> doMapCollection(final Class<T> type, final Collection<?> collection,
            final org.apache.avro.Schema elementType) {
        return ofNullable(collection)
                .map(c -> c.stream().map(item -> doMap(type, elementType, item)).collect(toList()))
                .orElse(null);
    }

    private <T> T doGet(final Class<T> expectedType, final String name) {
        final org.apache.avro.Schema.Field field = delegate.getSchema().getField(sanitizeConnectionName(name));
        if (field == null) {
            return null;
        }
        final Object value = delegate.get(field.pos());
        final org.apache.avro.Schema schema = field.schema();
        return doMap(expectedType, unwrapUnion(schema), value);
    }

    private <T> T doMap(final Class<T> expectedType, final org.apache.avro.Schema fieldSchema, final Object value) {
        if (Boolean.parseBoolean(readProp(fieldSchema, Schema.Type.DATETIME.name())) && Long.class.isInstance(value)
                && expectedType != Long.class) {
            return RECORD_CONVERTERS.coerce(expectedType, value, fieldSchema.getName());
        }
        if (IndexedRecord.class.isInstance(value) && (Record.class == expectedType || Object.class == expectedType)) {
            return expectedType.cast(new AvroRecord(IndexedRecord.class.cast(value)));
        }
        if (GenericArray.class.isInstance(value) && !GenericArray.class.isAssignableFrom(expectedType)) {
            final Class<?> itemType = expectedType == Collection.class ? Object.class : expectedType;
            return expectedType
                    .cast(doMapCollection(itemType, Collection.class.cast(value), fieldSchema.getElementType()));
        }
        if (ByteBuffer.class.isInstance(value) && byte[].class == expectedType) {
            return expectedType.cast(ByteBuffer.class.cast(value).array());
        }
        if (org.joda.time.DateTime.class.isInstance(value) && ZonedDateTime.class == expectedType) {
            final long epochMilli = org.joda.time.DateTime.class.cast(value).getMillis();
            return expectedType.cast(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMilli), UTC));
        }
        if (!expectedType.isInstance(value)) {
            if (Utf8.class.isInstance(value) && String.class == expectedType) {
                return expectedType.cast(value.toString());
            }
            return RECORD_CONVERTERS.coerce(expectedType, value, fieldSchema.getName());
        }
        if (Utf8.class.isInstance(value) && Object.class == expectedType) {
            return expectedType.cast(value.toString());
        }
        if (Collection.class.isAssignableFrom(expectedType) && value instanceof Collection) {
            final org.apache.avro.Schema elementType = fieldSchema.getElementType();
            final org.apache.avro.Schema elementSchema = unwrapUnion(elementType);
            Class<?> toType = Object.class;
            if (elementSchema.getType() == org.apache.avro.Schema.Type.RECORD) {
                toType = Record.class;
            } else if (elementSchema.getType() == org.apache.avro.Schema.Type.ARRAY) {
                toType = Collection.class;
            }
            final Collection<?> objects = this.doMapCollection(toType, Collection.class.cast(value), elementType);
            return expectedType.cast(objects);
        }
        return expectedType.cast(value);
    }

    private org.apache.avro.Schema toSchema(final Schema.Entry entry) {
        final org.apache.avro.Schema schema = doToSchema(entry);
        if (entry.isNullable() && schema.getType() != UNION) {
            return org.apache.avro.Schema.createUnion(asList(NULL_SCHEMA, schema));
        }
        if (!entry.isNullable() && schema.getType() == UNION) {
            return org.apache.avro.Schema
                    .createUnion(schema.getTypes().stream().filter(it -> it.getType() != NULL).collect(toList()));
        }
        return schema;
    }

    private org.apache.avro.Schema doToSchema(final Schema.Entry entry) {
        final Schema.Builder builder = new AvroSchemaBuilder().withType(entry.getType());
        switch (entry.getType()) {
        case ARRAY:
            ofNullable(entry.getElementSchema()).ifPresent(builder::withElementSchema);
            break;
        case RECORD:
            ofNullable(entry.getElementSchema()).ifPresent(s -> s.getAllEntries().forEach(builder::withEntry));
            break;
        default:
            // no-op
        }
        return Unwrappable.class.cast(builder.build()).unwrap(org.apache.avro.Schema.class);
    }

    @Override
    public String toString() {
        return "AvroRecord{delegate=" + delegate + '}';
    }
}
