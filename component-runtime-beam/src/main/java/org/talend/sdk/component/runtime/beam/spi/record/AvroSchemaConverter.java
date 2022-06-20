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

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.avro.Schema.Type.NULL;
import static org.apache.avro.Schema.Type.UNION;
import static org.talend.sdk.component.runtime.beam.spi.record.SchemaIdGenerator.generateRecordName;
import static org.talend.sdk.component.runtime.record.SchemaImpl.ENTRIES_ORDER_PROP;

import java.util.List;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.manager.service.api.Unwrappable;
import org.talend.sdk.component.runtime.record.SchemaImpl;

/**
 * Convert TCK Schema (SchemaImpl) to AvroSchema.
 */
public class AvroSchemaConverter {

    private static final org.apache.avro.Schema NULL_SCHEMA = org.apache.avro.Schema.create(NULL);

    public AvroSchema convert(final SchemaImpl schema) {
        final Schema.EntriesOrder eo = Schema.EntriesOrder.of(schema.getProp(ENTRIES_ORDER_PROP));
        final List<org.apache.avro.Schema.Field> fields = schema.getAllEntries().sorted(eo).map(entry -> {
            final org.apache.avro.Schema avroSchema = toSchema(entry);
            final org.apache.avro.Schema.Field f = AvroSchemaBuilder.AvroHelper.toField(avroSchema, entry);
            return f;
        }).collect(toList());
        final org.apache.avro.Schema avroSchema =
                org.apache.avro.Schema.createRecord(generateRecordName(fields), null, null, false);
        schema.getProps().forEach(avroSchema::addProp);
        avroSchema.setFields(fields);
        return new AvroSchema(avroSchema);
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

}
