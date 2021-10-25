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

import static java.util.Arrays.asList;
import static org.talend.sdk.component.api.record.Schema.sanitizeConnectionName;
import static org.talend.sdk.component.runtime.record.Schemas.EMPTY_RECORD;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Builder;
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
    }
            .setProp(
                    LogicalTypes
                            .timestampMillis()
                            .addToSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG)),
                    Schema.Type.DATETIME.name(), "true"));

    private static final AvroSchema STRING_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING));

    private static final AvroSchema DOUBLE_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.DOUBLE));

    private static final AvroSchema FLOAT_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.FLOAT));

    private static final AvroSchema BOOLEAN_SCHEMA =
            new AvroSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BOOLEAN));

    private static final AvroSchema BYTES_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(NULL_SCHEMA, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BYTES))));

    private static final AvroSchema INT_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(NULL_SCHEMA, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT))));

    private static final AvroSchema LONG_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(NULL_SCHEMA, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG))));

    private static final AvroSchema DATETIME_SCHEMA_NULLABLE =
            new AvroSchema(org.apache.avro.Schema.createUnion(asList(NULL_SCHEMA, new AvroPropertyMapper() {
            }
                    .setProp(
                            LogicalTypes
                                    .timestampMillis()
                                    .addToSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG)),
                            Schema.Type.DATETIME.name(), "true"))));

    private static final AvroSchema STRING_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(NULL_SCHEMA, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING))));

    private static final AvroSchema DOUBLE_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(NULL_SCHEMA, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.DOUBLE))));

    private static final AvroSchema FLOAT_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(NULL_SCHEMA, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.FLOAT))));

    private static final AvroSchema BOOLEAN_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(NULL_SCHEMA, org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BOOLEAN))));

    private List<Schema.Entry> fields;

    private Schema.Type type;

    private Schema elementSchema;

    private final Map<String, String> props = new LinkedHashMap<>(0);

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
        final Schema.Entry realEntry = Schema.avoidCollision(entry, this.fields::stream, this::replaceEntry);
        fields.add(realEntry);
        return this;
    }

    private void replaceEntry(final String entryName, final Schema.Entry entry) {
        for (int index = 0; index < fields.size(); index++) {
            if (Objects.equals(entryName, fields.get(index).getName())) {
                fields.set(index, entry);
                return;
            }
        }
    }

    private Field entryToAvroField(final Schema.Entry entry) {
        final Unwrappable unwrappable;
        switch (entry.getType()) {
        case RECORD:
            unwrappable = Unwrappable.class.cast(entry.getElementSchema());
            break;
        case ARRAY:
            unwrappable = new Unwrappable() {

                @Override
                public <T> T unwrap(final Class<T> type) {
                    return type
                            .cast(org.apache.avro.Schema
                                    .createArray(Unwrappable.class
                                            .cast(entry.getElementSchema())
                                            .unwrap(org.apache.avro.Schema.class)));
                }
            };
            break;
        case BOOLEAN:
            unwrappable = !entry.isNullable() ? BOOLEAN_SCHEMA : BOOLEAN_SCHEMA_NULLABLE;
            break;
        case DOUBLE:
            unwrappable = !entry.isNullable() ? DOUBLE_SCHEMA : DOUBLE_SCHEMA_NULLABLE;
            break;
        case INT:
            unwrappable = !entry.isNullable() ? INT_SCHEMA : INT_SCHEMA_NULLABLE;
            break;
        case FLOAT:
            unwrappable = !entry.isNullable() ? FLOAT_SCHEMA : FLOAT_SCHEMA_NULLABLE;
            break;
        case BYTES:
            unwrappable = !entry.isNullable() ? BYTES_SCHEMA : BYTES_SCHEMA_NULLABLE;
            break;
        case LONG:
            unwrappable = !entry.isNullable() ? LONG_SCHEMA : LONG_SCHEMA_NULLABLE;
            break;
        case STRING:
            unwrappable = !entry.isNullable() ? STRING_SCHEMA : STRING_SCHEMA_NULLABLE;
            break;
        case DATETIME:
            unwrappable = !entry.isNullable() ? DATETIME_SCHEMA : DATETIME_SCHEMA_NULLABLE;
            break;
        default:
            unwrappable = Unwrappable.class.cast(new AvroSchemaBuilder().withType(entry.getType()).build());
        }
        final org.apache.avro.Schema schema = Unwrappable.class.cast(unwrappable).unwrap(org.apache.avro.Schema.class);
        return AvroHelper.toField(schema, entry);
    }

    @Override
    public Schema.Builder withElementSchema(final Schema schema) {
        if (type != Schema.Type.ARRAY && schema != null) {
            throw new IllegalArgumentException("elementSchema is only valid for ARRAY type of schema");
        }
        // Check schema is Avro Schema, otherwise, convert it.
        final Schema avroSchema = this.toAvroSchema(schema);
        this.elementSchema = avroSchema;
        return this;
    }

    /**
     * Convert a non avro schema to schema.
     * 
     * @param schema : Non Avro schema.
     * @return Avro schema.
     */
    private Schema toAvroSchema(final Schema schema) {
        if (schema == null || schema instanceof AvroSchema) {
            return schema;
        }
        final Builder builder = new AvroSchemaBuilder().withType(schema.getType());

        final Schema elementSchema = schema.getElementSchema();
        if (elementSchema != null) {
            final Schema avroSchema = this.toAvroSchema(elementSchema);
            builder.withElementSchema(avroSchema);
        }
        builder.withProps(schema.getProps());
        schema.getEntries().stream().map(this::convertEntry).forEach(builder::withEntry);

        return builder.build();
    }

    private Schema.Entry convertEntry(final Schema.Entry entry) {
        final Schema elementSchema = entry.getElementSchema();
        if (elementSchema == null || elementSchema instanceof AvroSchema) {
            return entry;
        }
        final Schema avroSchema = this.toAvroSchema(elementSchema);
        return entry
                .toBuilder()
                .withElementSchema(avroSchema) //
                .build();
    }

    @Override
    public Builder withProp(final String key, final String value) {
        props.put(key, value);
        return this;
    }

    @Override
    public Builder withProps(final Map<String, String> props) {
        if (props == null) {
            return this;
        }
        this.props.putAll(props);
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
            if (fields == null) {
                return new AvroSchema(AvroSchemas.getEmptySchema());
            }
            final List<Field> avroFields =
                    this.fields.stream().map(this::entryToAvroField).collect(Collectors.toList());
            final org.apache.avro.Schema record = org.apache.avro.Schema
                    .createRecord(SchemaIdGenerator.generateRecordName(avroFields), null, "talend.component.schema",
                            false);
            record.setFields(avroFields);
            return new AvroSchema(record);
        case ARRAY:
            if (elementSchema == null) {
                throw new IllegalStateException("No elementSchema set for this ARRAY schema");
            }
            // FIXME: 7/12/21 => TCOMP-1957
            final org.apache.avro.Schema elementType = elementSchema == EMPTY_RECORD ? AvroSchemas.getEmptySchema()
                    : Unwrappable.class.cast(elementSchema).unwrap(org.apache.avro.Schema.class);
            return new AvroSchema(org.apache.avro.Schema.createArray(elementType));
        default:
            throw new IllegalArgumentException("Unsupported: " + type);
        }
    }

    public static class AvroHelper {

        public static Field toField(final org.apache.avro.Schema schema, final Schema.Entry entry) {
            final Field field = new Field(sanitizeConnectionName(entry.getName()),
                    entry.isNullable() && schema.getType() != Type.UNION
                            ? org.apache.avro.Schema.createUnion(asList(NULL_SCHEMA, schema))
                            : schema,
                    entry.getComment(), (Object) entry.getDefaultValue());
            if (entry.isMetadata()) {
                field.addAlias(KeysForAvroProperty.METADATA_ALIAS_NAME);
            }
            if (entry.getRawName() != null) {
                field.addProp(KeysForAvroProperty.LABEL, entry.getRawName());
            }
            entry.getProps().forEach((k, v) -> field.addProp(k, v));

            return field;
        }
    }
}
