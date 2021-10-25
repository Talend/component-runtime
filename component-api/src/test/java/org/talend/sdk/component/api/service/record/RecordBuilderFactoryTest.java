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
package org.talend.sdk.component.api.service.record;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;

class RecordBuilderFactoryTest {

    static class MockRecordFactory implements RecordBuilderFactory {

        @Override
        public Builder newRecordBuilder(Schema schema, Record record) {
            return null;
        }

        @Override
        public Builder newRecordBuilder(Schema schema) {
            return null;
        }

        @Override
        public Builder newRecordBuilder() {
            return null;
        }

        @Override
        public Schema.Builder newSchemaBuilder(Type type) {
            return null;
        }

        @Override
        public Schema.Builder newSchemaBuilder(Schema schema) {
            return null;
        }

        @Override
        public Entry.Builder newEntryBuilder() {
            return new Entry.Builder();
        }
    }

    @Test
    void newEntryBuilder() {
        final RecordBuilderFactory factory = new MockRecordFactory();

        final Map<String, String> props = new HashMap<>();
        props.put("p1", "v1");

        Schema.Entry e1 = new Entry.Builder()
                .withName("n1") //
                .withDefaultValue("default") //
                .withType(Type.STRING) //
                .withNullable(true) //
                .withProps(props) //
                .build();

        final Entry e2 = factory
                .newEntryBuilder(e1) //
                .withNullable(false) //
                .withDefaultValue("def2") //
                .withProp("p2", "v2") //
                .build();
        Assertions.assertNotNull(e2);
        Assertions.assertEquals(Type.STRING, e2.getType());
        Assertions.assertEquals("def2", e2.getDefaultValue());
        Assertions.assertEquals("default", e1.getDefaultValue());

        Assertions.assertFalse(e2.isNullable());
        Assertions.assertTrue(e1.isNullable());
        Assertions.assertEquals(e1.getName(), e2.getName());

        Assertions.assertEquals("v1", e2.getProp("p1"));
        Assertions.assertEquals("v1", e1.getProp("p1"));

        Assertions.assertEquals("v2", e2.getProp("p2"));
        Assertions.assertEquals(null, e1.getProp("p2"));
    }
}