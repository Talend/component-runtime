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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.SchemaImpl;

class AvroSchemaConverterTest {

    @Test
    void convert() {
        AvroSchemaConverter converter = new AvroSchemaConverter();

        final SchemaImpl s1 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withType(Schema.Type.STRING)
                        .withNullable(true)
                        .withName("field1")
                        .build())
                .withProp("Hello", "World")
                .build();
        final AvroSchema a1 = converter.convert(s1);
        Assertions.assertNotNull(a1);
        final org.apache.avro.Schema.Field field1 = a1.getDelegate().getField("field1");
        Assertions.assertNotNull(field1);
        Assertions.assertTrue(field1.schema()
                .getTypes()
                .stream()
                .map(org.apache.avro.Schema::getType)
                .anyMatch(org.apache.avro.Schema.Type.STRING::equals));
        Assertions.assertEquals("World", a1.getProp("Hello"));
    }

    @Test
    void convertDecimal() {
        AvroSchemaConverter converter = new AvroSchemaConverter();

        final SchemaImpl s1 = (SchemaImpl) new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withType(Schema.Type.DECIMAL)
                        .withNullable(true)
                        .withName("field1")
                        .build())
                .withProp("Hello", "World")
                .build();
        final AvroSchema a1 = converter.convert(s1);
        Assertions.assertNotNull(a1);
        final org.apache.avro.Schema.Field field1 = a1.getDelegate().getField("field1");
        Assertions.assertNotNull(field1);
        Assertions.assertTrue(field1.schema()
                .getTypes()
                .stream()
                .map(org.apache.avro.Schema::getLogicalType)
                .anyMatch(type -> {
                    return type != null && "decimal".equals(type.getName());
                }));
        Assertions.assertEquals("World", a1.getProp("Hello"));
    }
}