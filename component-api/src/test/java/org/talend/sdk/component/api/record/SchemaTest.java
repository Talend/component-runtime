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
package org.talend.sdk.component.api.record;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;

import lombok.RequiredArgsConstructor;

class SchemaTest {

    @Test
    void testGetEntry() {
        final Schema sc1 = new SchemaExample(null, Collections.emptyMap());
        Assertions.assertNull(sc1.getEntry("unknown"));

        final Entry e1 = new EntryExample("e1");
        final Entry e2 = new EntryExample("e2");
        final Schema sc2 = new SchemaExample(Arrays.asList(e1, e2), Collections.emptyMap());
        Assertions.assertNull(sc2.getEntry("unknown"));
        Assertions.assertSame(e1, sc2.getEntry("e1"));
        Assertions.assertSame(e2, sc2.getEntry("e2"));
    }

    @RequiredArgsConstructor
    class SchemaExample implements Schema {

        private final List<Entry> entries;

        private final Map<String, String> props;

        @Override
        public Type getType() {
            return Type.RECORD;
        }

        @Override
        public Schema getElementSchema() {
            return null;
        }

        @Override
        public List<Entry> getEntries() {
            return this.entries;
        }

        @Override
        public Map<String, String> getProps() {
            return this.props;
        }

        @Override
        public String getProp(String property) {
            return this.props.get(property);
        }
    }

    @RequiredArgsConstructor
    class EntryExample implements Schema.Entry {

        private final String name;

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getRawName() {
            return null;
        }

        @Override
        public String getOriginalFieldName() {
            return null;
        }

        @Override
        public Type getType() {
            return null;
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        public <T> T getDefaultValue() {
            return null;
        }

        @Override
        public Schema getElementSchema() {
            return null;
        }

        @Override
        public String getComment() {
            return null;
        }

        @Override
        public Map<String, String> getProps() {
            return null;
        }

        @Override
        public String getProp(String property) {
            return null;
        }
    }
}