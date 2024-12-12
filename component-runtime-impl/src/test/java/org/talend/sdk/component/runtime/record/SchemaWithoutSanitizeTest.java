/*
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */
package org.talend.sdk.component.runtime.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.runtime.record.SchemaImpl.EntryImpl;

public class SchemaWithoutSanitizeTest {

    @BeforeAll
    static void setup() {
        System.setProperty(Schema.SKIP_SANITIZE_PROPERTY, "true");
    }

    @AfterAll
    static void teardown() {
        System.clearProperty(Schema.SKIP_SANITIZE_PROPERTY);
    }

    @Test
    void noSanitizeSchemaRecord() {
        final Entry entry1 = new EntryImpl.BuilderImpl().withType(Schema.Type.STRING).withName("entryé").build();
        final Entry entry2 = new EntryImpl.BuilderImpl().withType(Schema.Type.STRING).withName("entry ").build();

        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(entry1)
                .withEntry(entry2)
                .build();
        assertEquals("entryé", schema.getEntries().get(0).getName());
        assertEquals("entryé", schema.getEntries().get(0).getOriginalFieldName());
        assertNull(schema.getEntries().get(0).getRawName());
        assertEquals("entry ", schema.getEntries().get(1).getName());
        assertEquals("entry ", schema.getEntries().get(1).getOriginalFieldName());
        assertNull(schema.getEntries().get(1).getRawName());

        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        builder.withString(entry1.getName(), "Aloa1");
        builder.withString(entry2.getName(), "Aloa2");
        final Record record = builder.build();
        final List<Entry> entries = record.getSchema().getEntries();
        assertEquals(2, entries.size());
        assertEquals(entry1, entries.get(0));
        assertEquals(entry2, entries.get(1));
        assertEquals("Aloa1", record.getString(entry1.getName()));
        assertEquals("Aloa2", record.getString(entry2.getName()));
    }

    @Test
    void noSanitizeWithRecordBuilder() {
        final Record record = new RecordImpl.BuilderImpl()
                .withString("スイートホーム", "Aloa1")
                .withString("田舎万歳", "Aloa2")
                .build();
        assertEquals("Aloa1", record.getString("スイートホーム"));
        assertEquals("Aloa2", record.getString("田舎万歳"));

        final List<Entry> entries = record.getSchema().getEntries();
        assertEquals("スイートホーム", entries.get(0).getName());
        assertEquals("スイートホーム", entries.get(0).getOriginalFieldName());
        assertNull(entries.get(0).getRawName());
        assertEquals("田舎万歳", entries.get(1).getName());
        assertEquals("田舎万歳", entries.get(1).getOriginalFieldName());
        assertNull(entries.get(1).getRawName());
    }
}
