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
import org.talend.sdk.component.api.service.record.RecordBuilderFactoryTest.MockEntry.MockEntryBuilder;

import lombok.RequiredArgsConstructor;

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
            return new MockTCKEntryBuilder(MockEntry.builder());
        }
    }

    @lombok.Builder(setterPrefix = "with")
    @lombok.Getter
    static class MockEntry implements Schema.Entry {

        private final String name;

        private final String rawName;

        private final String originalFieldName;

        private final Type type;

        private final boolean nullable;

        private final boolean errorCapable;

        private final boolean metadata;

        private final Object defaultVal;

        @Override
        public <T> T getDefaultValue() {
            return (T) defaultVal;
        }

        private final Schema elementSchema;

        private final String comment;

        private final Map<String, String> props;

        @Override
        public String getProp(final String property) {
            return this.getProps().get(property);
        }

        @Override
        public Builder toBuilder() {
            throw new UnsupportedOperationException("#toBuilder()");
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    @RequiredArgsConstructor
    static class MockTCKEntryBuilder implements Entry.Builder {

        private final Map<String, String> props = new HashMap<>();

        private final MockEntryBuilder builder;

        @Override
        public Entry.Builder withName(String name) {
            builder.withName(name);
            return this;
        }

        @Override
        public Entry.Builder withRawName(String rawName) {
            this.builder.withRawName(rawName);
            return this;
        }

        @Override
        public Entry.Builder withType(Type type) {
            this.builder.withType(type);
            return this;
        }

        @Override
        public Entry.Builder withNullable(boolean nullable) {
            this.builder.withNullable(nullable);
            return this;
        }

        @Override
        public Entry.Builder withErrorCapable(boolean errorCapable) {
            this.builder.withErrorCapable(errorCapable);
            return this;
        }

        @Override
        public Entry.Builder withMetadata(boolean metadata) {
            this.builder.withMetadata(metadata);
            return this;
        }

        @Override
        public <T> Entry.Builder withDefaultValue(T value) {
            builder.withDefaultVal(value);
            return this;
        }

        @Override
        public Entry.Builder withElementSchema(Schema schema) {
            builder.withElementSchema(schema);
            return this;
        }

        @Override
        public Entry.Builder withComment(String comment) {
            this.builder.withComment(comment);
            return this;
        }

        @Override
        public Entry.Builder withProps(Map<String, String> inProps) {
            this.props.clear();
            this.props.putAll(inProps);
            return this;
        }

        @Override
        public Entry.Builder withProp(String key, String value) {
            this.props.put(key, value);
            return this;
        }

        @Override
        public Entry build() {
            builder.withProps(this.props);
            return builder.build();
        }
    }

    @Test
    void newEntryBuilder() {
        final RecordBuilderFactory factory = new MockRecordFactory();

        final Map<String, String> props = new HashMap<>();
        props.put("p1", "v1");

        Schema.Entry e1 = MockEntry
                .builder() //
                .withName("n1") //
                .withDefaultVal("default") //
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