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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.talend.sdk.component.api.record.Schema.sanitizeConnectionName;
import static org.talend.sdk.component.runtime.beam.avro.AvroSchemas.unwrapUnion;

import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Date;
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
import org.talend.sdk.component.runtime.record.RecordImpl;

public class AvroRecord implements Record, AvroPropertyMapper, Unwrappable {

    private static final RecordConverters RECORD_CONVERTERS = new RecordConverters();

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
        this.schema = AvroSchema.toAvroSchema(record.getSchema());
        this.delegate = new GenericData.Record(this.schema.getActualDelegate());

        record
                .getSchema()
                .getAllEntries()
                .forEach(entry -> ofNullable(record.get(Object.class, sanitizeConnectionName(entry.getName())))
                        .ifPresent(v -> {
                            final Object avroValue = directMapping(v);

                            if (avroValue != null) {
                                final org.apache.avro.Schema.Field field =
                                        this.schema.getActualDelegate()
                                                .getField(sanitizeConnectionName(entry.getName()));
                                delegate.put(field.pos(), avroValue);
                            }
                        }));
    }

    private Object directMapping(final Object value) {
        if (value instanceof Collection) {
            return Collection.class.cast(value).stream().map(this::directMapping).collect(toList());
        }
        if (value instanceof RecordImpl) {
            return new AvroRecord((Record) value).delegate;
        }
        if (value instanceof Record) {
            return Unwrappable.class.cast(value).unwrap(IndexedRecord.class);
        }
        if (value instanceof ZonedDateTime) {
            return ZonedDateTime.class.cast(value).toInstant().toEpochMilli();
        }
        if (value instanceof Date) {
            return Date.class.cast(value).getTime();
        }
        if (value instanceof byte[]) {
            return ByteBuffer.wrap(byte[].class.cast(value));
        }
        return value;
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

    public Builder withNewSchema(final Schema newSchema) {
        final AvroRecordBuilder builder = new AvroRecordBuilder(newSchema);
        newSchema.getAllEntries()
                .filter(e -> Objects.equals(schema.getEntry(e.getName()), e))
                .forEach(e -> builder.with(e, get(Object.class, e.getName())));
        return builder;
    }

    @Override
    public <T> T get(final Class<T> expectedType, final String name) {
        if (expectedType == Collection.class) {
            return expectedType.cast(getArray(Object.class, name));
        }
        return doGet(expectedType, sanitizeConnectionName(name));
    }

    @Override
    public <T> T get(final Class<T> expectedType, final Schema.Entry entry) {
        if (expectedType == Collection.class) {
            return expectedType.cast(this.doGetArray(Object.class, entry.getName()));
        }
        return doGet(expectedType, entry.getName());
    }

    @Override
    public <T> Collection<T> getArray(final Class<T> type, final String name) {
        final String sanitizedName = sanitizeConnectionName(name);
        return this.doGetArray(type, sanitizedName);
    }

    private <T> Collection<T> doGetArray(final Class<T> type, final String sanitizedName) {
        final Collection<?> collection = doGet(Collection.class, sanitizedName);
        if (collection == null) {
            return null;
        }
        final org.apache.avro.Schema elementType =
                unwrapUnion(delegate.getSchema().getField(sanitizedName).schema()).getElementType();
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
        final org.apache.avro.Schema.Field field = delegate.getSchema().getField(name);
        if (field == null) {
            return null;
        }
        final Object value = delegate.get(field.pos());
        final org.apache.avro.Schema fieldSchema = field.schema();
        return doMap(expectedType, unwrapUnion(fieldSchema), value);
    }

    private <T> T doMap(final Class<T> expectedType, final org.apache.avro.Schema fieldSchemaRaw, final Object value) {

        if (value != null && expectedType == value.getClass() && !(value instanceof Collection)) {
            return expectedType.cast(value);
        }
        if (value instanceof IndexedRecord && (Record.class == expectedType || Object.class == expectedType)) {
            return expectedType.cast(new AvroRecord(IndexedRecord.class.cast(value)));
        }
        if (value instanceof ByteBuffer && byte[].class == expectedType) {
            return expectedType.cast(ByteBuffer.class.cast(value).array());
        }
        final org.apache.avro.Schema fieldSchema = unwrapUnion(fieldSchemaRaw);
        if (value instanceof Long && expectedType != Long.class
                && Boolean.parseBoolean(readProp(fieldSchema, Schema.Type.DATETIME.name()))) {
            return RECORD_CONVERTERS.coerce(expectedType, value, fieldSchema.getName());
        }
        if (value instanceof GenericArray && !GenericArray.class.isAssignableFrom(expectedType)) {
            final Class<?> itemType = expectedType == Collection.class ? Object.class : expectedType;
            return expectedType
                    .cast(doMapCollection(itemType, Collection.class.cast(value), fieldSchema.getElementType()));
        }

        if (value instanceof org.joda.time.DateTime && ZonedDateTime.class == expectedType) {
            final long epochMilli = org.joda.time.DateTime.class.cast(value).getMillis();
            return expectedType.cast(ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMilli), UTC));
        }
        if (!expectedType.isInstance(value)) {
            if (value instanceof Utf8 && String.class == expectedType) {
                return expectedType.cast(value.toString());
            }
            return RECORD_CONVERTERS.coerce(expectedType, value, fieldSchema.getName());
        }
        if (value instanceof Utf8 && Object.class == expectedType) {
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
            final Collection<?> objects = this.doMapCollection(toType, Collection.class.cast(value), elementSchema);
            return expectedType.cast(objects);
        }
        return expectedType.cast(value);
    }

    @Override
    public String toString() {
        return "AvroRecord{delegate=" + delegate + '}';
    }
}
