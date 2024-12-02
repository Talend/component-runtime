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

    final Entry toto1 = new EntryImpl.BuilderImpl().withType(Schema.Type.STRING).withName("totoé").build();
    final Entry toto2 = new EntryImpl.BuilderImpl().withType(Schema.Type.STRING).withName("toto ").build();

    @Test
    void testSanitizeDuplicates() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(toto1)
                .withEntry(toto2)
                .build();

        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        builder.withString(toto1.getName(), "Aloa1");
        builder.withString("toto ", "Aloa2");
        Record record = builder.build();
    }

    @Test
    void testSanitizeDuplicates2() {
        final Record record = new RecordImpl.BuilderImpl()
                .withString("toto ", "Aloa1")
                .withString("totoé", "Aloa2")
                .build();
    }

    @Test
    void testSanitizeDuplicates3() {
        final Schema schema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(toto1)
                .withEntry(toto2)
                .build();

        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(schema);
        builder.withString("totoé", "Aloa1");
        builder.withString("toto ", "Aloa2");
        Record record = builder.build();
    }
}
