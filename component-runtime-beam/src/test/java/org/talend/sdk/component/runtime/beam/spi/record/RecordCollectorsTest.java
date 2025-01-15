/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class RecordCollectorsTest {

    private final RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test");

    @Test
    void merge() {
        // Prepare
        final Schema.Entry field1 = entryBuilder("field1").build();
        final Schema.Entry field2 = entryBuilder("field2").build();
        final Schema.Entry meta1 = entryBuilder("meta1").withMetadata(true).build();
        final Schema.Entry meta2 = entryBuilder("meta2").withMetadata(true).build();
        final Schema schema = factory
                .newSchemaBuilder(Schema.Type.RECORD) //
                .withEntry(field1) //
                .withEntry(meta1) //
                .withEntry(field2) //
                .withEntry(meta2) //
                .build();

        final Record.Builder record1 = this.factory
                .newRecordBuilder(schema) //
                .withString(field1, "value1") //
                .withString(meta1, "mateValue1");
        final Record.Builder record2 = this.factory
                .newRecordBuilder(schema) //
                .withString(field2, "value2") //
                .withString(meta2, "mateValue2");

        // Call function to test
        RecordCollectors.merge(record1, record2);

        // Check
        final Record record = record1.build();
        Assertions.assertEquals("value1", record.getString("field1"));
        Assertions.assertEquals("mateValue1", record.getString("meta1"));
        Assertions.assertEquals("value2", record.getString("field2"));
        Assertions.assertEquals("mateValue2", record.getString("meta2"));
    }

    private Schema.Entry.Builder entryBuilder(String name) {
        return this.factory.newEntryBuilder().withName(name).withType(Schema.Type.STRING).withNullable(true);
    }
}