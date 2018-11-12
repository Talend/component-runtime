/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.talend.sdk.component.runtime.beam.avro.AvroSchemas.unwrapUnion;
import static org.talend.sdk.component.runtime.beam.spi.record.Jacksons.toObject;

import java.util.List;

import org.apache.avro.Schema;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;
import org.talend.sdk.component.runtime.record.SchemaImpl;

import lombok.Data;

@Data
public class AvroSchema implements org.talend.sdk.component.api.record.Schema, AvroPropertyMapper, Unwrappable {

    private final Schema delegate;

    private volatile AvroSchema elementSchema;

    private volatile List<Entry> entries;

    @Override
    public Type getType() {
        return mapType(delegate);
    }

    @Override
    public org.talend.sdk.component.api.record.Schema getElementSchema() {
        if (elementSchema != null) {
            return elementSchema;
        }
        if (delegate.getType() == Schema.Type.ARRAY) {
            synchronized (this) {
                if (elementSchema != null) {
                    return elementSchema;
                }
                elementSchema = new AvroSchema(delegate.getElementType());
            }
        }
        return elementSchema;
    }

    @Override
    public List<Entry> getEntries() {
        if (delegate.getType() != Schema.Type.RECORD) {
            return emptyList();
        }
        if (entries != null) {
            return entries;
        }
        synchronized (this) {
            if (entries != null) {
                return entries;
            }
            entries = delegate.getFields().stream().map(field -> {
                final Type type = mapType(field.schema());
                final AvroSchema elementSchema = new AvroSchema(
                        type == Type.ARRAY ? unwrapUnion(field.schema()).getElementType() : field.schema());
                return new SchemaImpl.EntryImpl(field.name(), type, field.defaultValue() == null,
                        field.defaultValue() != null ? toObject(field.defaultValue()) : null, elementSchema,
                        field.doc());
            }).collect(toList());
        }
        return entries;
    }

    @Override
    public <T> T unwrap(final Class<T> type) {
        if (type.isInstance(delegate)) {
            return type.cast(delegate);
        }
        if (type.isInstance(this)) {
            return type.cast(this);
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private Type mapType(final Schema schema) {
        return doMapType(unwrapUnion(schema));
    }

    private Type doMapType(final Schema schema) {
        switch (schema.getType()) {
        case LONG:
            if (Boolean.parseBoolean(readProp(schema, Type.DATETIME.name()))) {
                return Type.DATETIME;
            }
            return Type.LONG;
        default:
            return Type.valueOf(schema.getType().name());
        }
    }
}
