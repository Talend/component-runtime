/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import static java.util.stream.Collectors.joining;
import static org.talend.sdk.component.api.record.SchemaCompanionUtil.sanitizeName;
import static org.talend.sdk.component.runtime.record.SchemaImpl.ENTRIES_ORDER_PROP;
import static org.talend.sdk.component.runtime.record.Schemas.EMPTY_RECORD;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.avro.AvroTypeException;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.talend.sdk.component.api.record.OrderedMap;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Builder;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.SchemaCompanionUtil;
import org.talend.sdk.component.api.record.SchemaProperty;
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

    private static final AvroSchema DATE_SCHEMA = new AvroSchema(new AvroPropertyMapper() {
    }
            .setProp(
                    LogicalTypes
                            .date()
                            .addToSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT)),
                    Schema.Type.DATETIME.name(), "true"));

    private static final AvroSchema TIME_SCHEMA = new AvroSchema(new AvroPropertyMapper() {
    }
            .setProp(
                    LogicalTypes
                            .timeMillis()
                            .addToSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT)),
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
            .createUnion(asList(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BYTES), NULL_SCHEMA)));

    private static final AvroSchema INT_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT), NULL_SCHEMA)));

    private static final AvroSchema LONG_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG), NULL_SCHEMA)));

    private static final AvroSchema DATETIME_SCHEMA_NULLABLE =
            new AvroSchema(org.apache.avro.Schema.createUnion(asList(new AvroPropertyMapper() {
            }
                    .setProp(
                            LogicalTypes
                                    .timestampMillis()
                                    .addToSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.LONG)),
                            Schema.Type.DATETIME.name(), "true"),
                    NULL_SCHEMA)));

    private static final AvroSchema DATE_SCHEMA_NULLABLE =
            new AvroSchema(org.apache.avro.Schema.createUnion(asList(new AvroPropertyMapper() {
            }
                    .setProp(
                            LogicalTypes
                                    .date()
                                    .addToSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.INT)),
                            Schema.Type.DATETIME.name(), "true"),
                    NULL_SCHEMA)));

    private static final AvroSchema TIME_SCHEMA_NULLABLE =
            new AvroSchema(org.apache.avro.Schema.createUnion(asList(new AvroPropertyMapper() {
            }
                    .setProp(
                            LogicalTypes
                                    .timeMillis()
                                    .addToSchema(org.apache.avro.Schema.create(Type.INT)),
                            Schema.Type.DATETIME.name(), "true"),
                    NULL_SCHEMA)));

    private static final AvroSchema STRING_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING), NULL_SCHEMA)));

    private static final AvroSchema DOUBLE_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.DOUBLE), NULL_SCHEMA)));

    private static final AvroSchema FLOAT_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.FLOAT), NULL_SCHEMA)));

    private static final AvroSchema BOOLEAN_SCHEMA_NULLABLE = new AvroSchema(org.apache.avro.Schema
            .createUnion(asList(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.BOOLEAN), NULL_SCHEMA)));

    private static final AvroSchema DECIMAL_SCHEMA_NULLABLE =
            new AvroSchema(org.apache.avro.Schema.createUnion(asList(new AvroPropertyMapper() {
            }
                    .setProp(
                            Decimal.logicalType()
                                    .addToSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING)),
                            Schema.Type.DECIMAL.name(), "true"),
                    NULL_SCHEMA)));

    private static final AvroSchema DECIMAL_SCHEMA = new AvroSchema(
            new AvroPropertyMapper() {
            }
                    .setProp(
                            Decimal.logicalType()
                                    .addToSchema(org.apache.avro.Schema.create(org.apache.avro.Schema.Type.STRING)),
                            Schema.Type.DECIMAL.name(), "true"));

    private OrderedMap<Schema.Entry> fields;

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
            fields = new OrderedMap<>(Schema.Entry::getName, Collections.singletonList(entry));
        }

        final Schema.Entry realEntry = SchemaCompanionUtil.avoidCollision(entry, fields::getValue, fields::replace);
        fields.addValue(realEntry);
        return this;
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
                String logicalType = entry.getLogicalType();
                if (SchemaProperty.LogicalType.DATE.key().equals(logicalType)) {
                    unwrappable = !entry.isNullable() ? DATE_SCHEMA : DATE_SCHEMA_NULLABLE;
                } else if (SchemaProperty.LogicalType.TIME.key().equals(logicalType)) {
                    unwrappable = !entry.isNullable() ? TIME_SCHEMA : TIME_SCHEMA_NULLABLE;
                } else {
                    // should be logicalType == null || SchemaProperty.LogicalType.TIMESTAMP.key().equals(logicalType)
                    unwrappable = !entry.isNullable() ? DATETIME_SCHEMA : DATETIME_SCHEMA_NULLABLE;
                }
                break;
            case DECIMAL:
                unwrappable = !entry.isNullable() ? DECIMAL_SCHEMA : DECIMAL_SCHEMA_NULLABLE;
                break;
            default:
                unwrappable = Unwrappable.class.cast(new AvroSchemaBuilder().withType(entry.getType()).build());
        }
        final org.apache.avro.Schema schema = Unwrappable.class.cast(unwrappable).unwrap(org.apache.avro.Schema.class);
        return AvroHelper.toField(schema, entry);
    }

    @Override
    public Builder withEntryAfter(final String after, final Entry entry) {
        withEntry(entry);
        return moveAfter(after, entry.getName());
    }

    @Override
    public Builder withEntryBefore(final String before, final Entry entry) {
        withEntry(entry);
        return moveBefore(before, entry.getName());
    }

    @Override
    public Builder remove(final String name) {
        final Entry entry = fields.getValue(name);
        return remove(entry);
    }

    @Override
    public Builder remove(final Entry entry) {
        if (entry != null) {
            fields.removeValue(entry);
        }
        return this;
    }

    @Override
    public Builder moveAfter(final String after, final String name) {
        final Entry entryToMove = this.fields.getValue(name);
        if (entryToMove == null) {
            throw new IllegalArgumentException(String.format("%s not in schema", name));
        }
        this.fields.moveAfter(after, entryToMove);
        return this;
    }

    @Override
    public Builder moveBefore(final String before, final String name) {
        final Entry entryToMove = this.fields.getValue(name);
        if (entryToMove == null) {
            throw new IllegalArgumentException(String.format("%s not in schema", name));
        }
        this.fields.moveBefore(before, entryToMove);
        return this;
    }

    @Override
    public Builder swap(final String name, final String with) {
        this.fields.swap(name, with);
        return this;
    }

    @Override
    public Schema.Builder withElementSchema(final Schema schema) {
        if (type != Schema.Type.ARRAY && schema != null) {
            throw new IllegalArgumentException("elementSchema is only valid for ARRAY type of schema");
        }
        // Check schema is Avro Schema, otherwise, convert it.
        final Schema avroSchema = this.toAvroSchema(schema);
        final AvroSchema avro = (AvroSchema) avroSchema;

        this.elementSchema = this.wrapNullable(avro);
        return this;
    }

    private AvroSchema wrapNullable(final AvroSchema schema) {
        if (schema == null) {
            return null;
        }
        org.apache.avro.Schema delegate = schema.getDelegate();
        if (delegate.getType() != Type.UNION) {
            org.apache.avro.Schema nullableType = org.apache.avro.Schema.createUnion(delegate,
                    org.apache.avro.Schema.create(Type.NULL));
            return new AvroSchema(nullableType);
        }
        return schema;
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
        return build(null);
    }

    @Override
    public Schema build(final Comparator<Entry> order) {
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
            case DECIMAL:
                return DECIMAL_SCHEMA;
            case RECORD:
                if (fields == null) {
                    return new AvroSchema(AvroSchemas.getEmptySchema());
                }
                final List<Field> avroFields =
                        this.fields.streams().map(this::entryToAvroField).collect(Collectors.toList());
                final org.apache.avro.Schema record = org.apache.avro.Schema
                        .createRecord(SchemaIdGenerator.generateRecordName(avroFields), null, "talend.component.schema",
                                false);
                record.setFields(avroFields);
                if (order != null) {
                    final String entriesOrder =
                            fields.streams().sorted(order).map(Entry::getName).collect(joining(","));
                    record.addProp(ENTRIES_ORDER_PROP, entriesOrder);
                } else {
                    record.addProp(ENTRIES_ORDER_PROP,
                            fields.streams().map(Entry::getName).collect(joining(",")));
                }
                this.props.entrySet()
                        .stream()
                        .filter((Map.Entry<String, String> e) -> !ENTRIES_ORDER_PROP.equals(e.getKey()))
                        .forEach((Map.Entry<String, String> e) -> record.addProp(e.getKey(), e.getValue()));
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
            Field field = null;
            try {
                field = new Field(sanitizeName(entry.getName()),
                        entry.isNullable() && schema.getType() != Type.UNION
                                ? org.apache.avro.Schema.createUnion(asList(schema, NULL_SCHEMA))
                                : schema,
                        entry.getComment(), (Object) entry.getDefaultValue());
            } catch (AvroTypeException e) {
                field = new Field(sanitizeName(entry.getName()),
                        entry.isNullable() && schema.getType() != Type.UNION
                                ? org.apache.avro.Schema.createUnion(asList(schema, NULL_SCHEMA))
                                : schema,
                        entry.getComment());
            }
            if (entry.isMetadata()) {
                field.addAlias(KeysForAvroProperty.METADATA_ALIAS_NAME);
            }
            if (entry.getRawName() != null) {
                field.addProp(KeysForAvroProperty.LABEL, entry.getRawName());
            }
            for (Map.Entry<String, String> e : entry.getProps().entrySet()) {
                field.addProp(e.getKey(), e.getValue());
            }
            if (entry.isErrorCapable()) {
                field.addProp(KeysForAvroProperty.IS_ERROR_CAPABLE, String.valueOf(entry.isErrorCapable()));
            }
            return field;
        }
    }
}
