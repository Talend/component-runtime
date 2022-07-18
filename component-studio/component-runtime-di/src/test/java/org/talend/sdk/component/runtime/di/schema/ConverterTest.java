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
package org.talend.sdk.component.runtime.di.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.talend.sdk.component.runtime.di.schema.StudioRecordProperties.STUDIO_PATTERN;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class ConverterTest {

    private final RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test");

    @Test
    void toColumns() {
        final JavaTypesManager typesManager = new JavaTypesManager();
        final Converter converter = new Converter(typesManager);

        // To Column
        final Schema.Entry f1 = this.field("f1", Schema.Type.STRING).build();
        final Schema.Entry f2 = this.field("f2", Schema.Type.LONG).build();
        final Schema.Entry f3 = this.field("f3", Schema.Type.DOUBLE)
                .withProp(StudioRecordProperties.STUDIO_LENGTH, "8")
                .withProp(StudioRecordProperties.STUDIO_PRECISION, "4")
                .build();
        final Schema.Entry f4 = this.field("f4", Schema.Type.DATETIME)
                .build();
        Schema schema = this.record(f1, f2, f3, f4);
        Map<String, Column> columns = converter.toColumns(schema);
        Assertions.assertEquals(4, columns.size());

        final Column c1 = columns.get("f1");
        Assertions.assertEquals("f1", c1.getLabel());
        Assertions.assertEquals(typesManager.STRING.getId(), c1.getTalendType());

        final Column c2 = columns.get("f2");
        Assertions.assertEquals("f2", c2.getLabel());
        Assertions.assertEquals(typesManager.LONG.getId(), c2.getTalendType());

        final Column c3 = columns.get("f3");
        Assertions.assertEquals("f3", c3.getLabel());
        Assertions.assertEquals(typesManager.DOUBLE.getId(), c3.getTalendType());
        Assertions.assertEquals(8, c3.getLength());
        Assertions.assertEquals(4, c3.getPrecision());

        final Column c4 = columns.get("f4");
        Assertions.assertEquals("f4", c4.getLabel());
        Assertions.assertEquals(typesManager.DATE.getId(), c4.getTalendType());

        // To Schema
        Schema schema1 = converter.toSchema(factory, columns);
        Assertions.assertEquals(Schema.Type.RECORD, schema1.getType());
        Assertions.assertEquals(4, schema1.getEntries().size());
        Assertions.assertEquals(f1, schema1.getEntry("f1"));
        Assertions.assertEquals(f2, schema1.getEntry("f2"));
        Assertions.assertEquals(f3, schema1.getEntry("f3"));

        Schema.Entry newF4 = schema1.getEntry("f4");
        Assertions.assertEquals(f4.getType(), newF4.getType());
        Assertions.assertEquals("dd-MM-yyyy", newF4.getProp(STUDIO_PATTERN));
    }

    private Schema record(Schema.Entry... entries) {
        final Schema.Builder builder = factory.newSchemaBuilder(Schema.Type.RECORD);
        for (Schema.Entry entry : entries) {
            builder.withEntry(entry);
        }
        return builder.build();
    }

    private Schema.Entry.Builder field(String name, Schema.Type type) {
        return factory.newEntryBuilder()
                .withName(name)
                .withType(type);
    }
}