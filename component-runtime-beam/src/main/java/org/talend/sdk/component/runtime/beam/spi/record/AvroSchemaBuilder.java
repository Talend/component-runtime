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

import static java.util.Arrays.asList;
import static org.talend.sdk.component.runtime.beam.avro.AvroSchemas.sanitizeConnectionName;
import static org.talend.sdk.component.runtime.beam.spi.record.Jacksons.toJsonNode;
import static org.talend.sdk.component.runtime.record.Schemas.EMPTY_RECORD;

import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.beam.avro.AvroSchemas;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;

public class AvroSchemaBuilder implements Schema.Builder {

    private static final org.apache.avro.Schema NULL_SCHEMA =
            org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL);

    private static final AvroSchema BYTES_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BYTES));

    private static final AvroSchema INT_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT));

    private static final AvroSchema LONG_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG));

    private static final AvroSchema DATETIME_SCHEMA = new AvroSchema(new AvroPropertyMapper() {
    }.setProp(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG), Schema.Type.DATETIME.name(), "true"));

    private static final AvroSchema STRING_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING));

    private static final AvroSchema DOUBLE_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.DOUBLE));

    private static final AvroSchema FLOAT_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.FLOAT));

    private static final AvroSchema BOOLEAN_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BOOLEAN));

    private List<org.apache.avro.Schema.Field> fields;

    private Schema.Type type;

    private Schema elementSchema;

    @Override
    public Schema.Builder withType(final Schema.Type type) {
        this.type = type;
        return this;
    }

    @Override
    public Schema.Builder withEntry(final Schema.Entry entry) {
        if (type != Schema.Type.RECORD) {
            throw new IllegalArgumentException("entry is only valid for RECORD type of schema");
        }
        if (fields == null) {
            fields = new ArrayList<>();
        }
        final Schema.Builder entrySchemaBuilder = new AvroSchemaBuilder().withType(entry.getType());
        switch (entry.getType()) {
        case ARRAY:
            entrySchemaBuilder.withElementSchema(entry.getElementSchema());
            break;
        case RECORD:
            if (entry.getElementSchema() != null && entry.getElementSchema().getEntries() != null) {
                entry.getElementSchema().getEntries().forEach(entrySchemaBuilder::withEntry);
            }
            break;
        default:
        }
        final org.apache.avro.Schema schema =
                Unwrappable.class.cast(entrySchemaBuilder.build()).unwrap(org.apache.avro.Schema.class);
        fields
                .add(new org.apache.avro.Schema.Field(sanitizeConnectionName(entry.getName()),
                        entry.isNullable() && schema.getType() != org.apache.avro.Schema.Type.UNION
                                ? org.apache.avro.Schema.createUnion(asList(NULL_SCHEMA, schema))
                                : schema,
                        entry.getComment(), toJsonNode(entry.getDefaultValue())));
        return this;
    }

    @Override
    public Schema.Builder withElementSchema(final Schema schema) {
        if (type != Schema.Type.ARRAY && schema != null) {
            throw new IllegalArgumentException("elementSchema is only valid for ARRAY type of schema");
        }
        this.elementSchema = schema;
        return this;
    }

    @Override
    public Schema build() {
        switch (type) {
        case BYTES:
            return BYTES_SCHEMA;
        case INT:
            return INT_SCHEMA;
        case LONG:
            return LONG_SCHEMA;
        case STRING:
            return STRING_SCHEMA;
        case DOUBLE:
            return DOUBLE_SCHEMA;
        case FLOAT:
            return FLOAT_SCHEMA;
        case BOOLEAN:
            return BOOLEAN_SCHEMA;
        case DATETIME:
            return DATETIME_SCHEMA;
        case RECORD:
            final org.apache.avro.Schema record = org.apache.avro.Schema
                    .createRecord(SchemaIdGenerator.generateRecordName(fields), null, "talend.component.schema", false);
            record.setFields(fields);
            return new AvroSchema(record);
        case ARRAY:
            if (elementSchema == null) {
                throw new IllegalStateException("No elementSchema set for this ARRAY schema");
            }
            final org.apache.avro.Schema elementType = elementSchema == EMPTY_RECORD ? AvroSchemas.getEmptySchema()
                    : Unwrappable.class.cast(elementSchema).unwrap(org.apache.avro.Schema.class);
            return new AvroSchema(org.apache.avro.Schema.createArray(elementType));
        default:
            throw new IllegalArgumentException("Unsupported: " + type);
        }
    }
}
