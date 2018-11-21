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
package org.talend.sdk.component.runtime.record;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

class RecordBuilderImplTest {

    @Test
    void nullSupportString() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withString("test", null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getString("test"));
    }

    @Test
    void nullSupportDate() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withDateTime("test", (Date) null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getDateTime("test"));
    }

    @Test
    void nullSupportBytes() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder.withBytes("test", null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getBytes("test"));
    }

    @Test
    void nullSupportCollections() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        builder
                .withArray(new SchemaImpl.EntryImpl("test", Schema.Type.ARRAY, true, null,
                        new SchemaImpl(Schema.Type.STRING, null, null), null), null);
        final Record record = builder.build();
        assertEquals(1, record.getSchema().getEntries().size());
        assertNull(record.getArray(String.class, "test"));
    }

    @Test
    void notNullableNullBehavior() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl();
        assertThrows(IllegalArgumentException.class, () -> builder
                .withString(new SchemaImpl.EntryImpl.BuilderImpl().withNullable(false).withName("test").build(), null));
    }
}
