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
package org.talend.sdk.component.runtime.beam.spi.record;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.avro.Schema.Type.NULL;
import static org.apache.avro.Schema.Type.UNION;
import static org.talend.sdk.component.runtime.beam.avro.AvroSchemas.unwrapUnion;

import java.util.List;
import java.util.Map;

import javax.json.bind.annotation.JsonbTransient;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;
import org.talend.sdk.component.runtime.record.SchemaImpl;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(of = "delegate")
public class AvroSchema implements org.talend.sdk.component.api.record.Schema, AvroPropertyMapper, Unwrappable {

    @JsonbTransient
    private final Schema delegate;

    private volatile AvroSchema elementSchema;

    private volatile List<Entry> entries;

    private volatile Type type;

    private volatile Schema actualDelegate;

    private Schema getActualDelegate() {
        if (actualDelegate != null) {
            return actualDelegate;
        }
        synchronized (this) {
            if (actualDelegate != null) {
                return actualDelegate;
            }
            actualDelegate = unwrapUnion(delegate);
        }
        return actualDelegate;
    }

    @Override
    public Type getType() {
        return mapType(getActualDelegate());
    }

    @Override
    public org.talend.sdk.component.api.record.Schema getElementSchema() {
        if (elementSchema != null) {
            return elementSchema;
        }
        if (getActualDelegate().getType() == Schema.Type.ARRAY) {
            synchronized (this) {
                if (elementSchema != null) {
                    return elementSchema;
                }
                elementSchema = new AvroSchema(getActualDelegate().getElementType());
            }
        }
        return elementSchema;
    }

    @Override
    public List<Entry> getEntries() {
        if (getActualDelegate().getType() != Schema.Type.RECORD) {
            return emptyList();
        }
        if (entries != null) {
            return entries;
        }
        synchronized (this) {
            if (entries != null) {
                return entries;
            }
            entries =
                    getActualDelegate().getFields().stream().filter(it -> it.schema().getType() != NULL).map(field -> {
                        final Type type = mapType(field.schema());
                        final AvroSchema elementSchema = new AvroSchema(
                                type == Type.ARRAY ? unwrapUnion(field.schema()).getElementType() : field.schema());
                        // readProp(unwrapUnion(field.schema()), KeysForAvroProperty.LABEL) is not good location in my
                        // view
                        return new SchemaImpl.EntryImpl(field.name(), field.getProp(KeysForAvroProperty.LABEL), type,
                                field.schema().getType() == UNION, field.defaultVal(), elementSchema, field.doc(),
                                field.getProps());
                    }).collect(toList());
        }
        return entries;
    }

    @Override
    public Map<String, String> getProps() {
        if (getActualDelegate().getType() != Schema.Type.RECORD) {
            return emptyMap();
        }
        return getActualDelegate().getProps();
    }

    @Override
    public String getProp(final String property) {
        if (getActualDelegate().getType() != Schema.Type.RECORD) {
            return null;
        }
        return getActualDelegate().getProp(property);
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
            if (Boolean.parseBoolean(readProp(schema, Type.DATETIME.name()))
                    || LogicalTypes.timestampMillis().equals(LogicalTypes.fromSchemaIgnoreInvalid(schema))) {
                return Type.DATETIME;
            }
            return Type.LONG;
        default:
            return Type.valueOf(schema.getType().name());
        }
    }
}
