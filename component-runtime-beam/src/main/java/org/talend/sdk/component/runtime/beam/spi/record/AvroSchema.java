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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbTransient;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(of = "delegate")
@ToString(of = "delegate")
public class AvroSchema implements org.talend.sdk.component.api.record.Schema, AvroPropertyMapper, Unwrappable {

    @JsonbTransient
    private final Schema delegate;

    private volatile AvroSchema elementSchema;

    private volatile List<Entry> entries;

    @JsonbTransient
    private volatile List<Entry> metadataEntries;

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
            entries = this
                    .getNonNullFields() //
                    .filter(f -> !AvroSchema.isMetadata(f)) // only data fields
                    .map(this::fromAvro) //
                    .collect(toList());
        }
        return entries;
    }

    @Override
    public List<Entry> getMetadata() {
        if (getActualDelegate().getType() != Schema.Type.RECORD) {
            return emptyList();
        }
        if (this.metadataEntries == null) {
            synchronized (this) {
                if (this.metadataEntries == null) {
                    this.metadataEntries = this
                            .getNonNullFields() //
                            .filter(AvroSchema::isMetadata) // only metadata fields
                            .map(this::fromAvro) //
                            .collect(Collectors.toList());
                }
            }
        }
        return this.metadataEntries;
    }

    @Override
    @JsonbTransient
    public Stream<Entry> getAllEntries() {
        return Stream.concat(this.getEntries().stream(), this.getMetadata().stream());
    }

    private Stream<Field> getNonNullFields() {
        return getActualDelegate().getFields().stream().filter(it -> it.schema().getType() != NULL);
    }

    private static boolean isMetadata(final Field f) {
        return f.aliases() != null && f.aliases().contains(KeysForAvroProperty.METADATA_ALIAS_NAME);
    }

    private Entry fromAvro(final Field field) {
        final Type type = mapType(field.schema());
        final AvroSchema elementSchema =
                new AvroSchema(type == Type.ARRAY ? unwrapUnion(field.schema()).getElementType() : field.schema());

        return AvroSchema.buildFromAvro(field, type, elementSchema);
    }

    private static Entry buildFromAvro(final Field field, final Type type, final AvroSchema elementSchema) {
        return new Entry.Builder() //
                .withName(field.name()) //
                .withRawName(field.getProp(KeysForAvroProperty.LABEL)) //
                .withType(type) //
                .withNullable(field.schema().getType() == UNION) //
                .withMetadata(AvroSchema.isMetadata(field)) //
                .withDefaultValue(field.defaultVal()) //
                .withElementSchema(elementSchema) //
                .withComment(field.doc()) //
                .withProps(field.getProps()) //
                .build();
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
    public Builder toBuilder() {
        final Builder builder = new AvroSchemaBuilder()
                .withElementSchema(this.elementSchema)
                .withType(this.type)
                .withProps(this
                        .getProps()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        this.getAllEntries().forEach(builder::withEntry);
        return builder;
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
