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
package org.talend.sdk.component.api.record;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.RequiredArgsConstructor;

class RecordTest {

    @Test
    void optionalString() {
        final Optional<String> opt = new MockRecord("ok").getOptionalString("dd");
        assertTrue(opt.isPresent());
        assertEquals("ok", opt.orElseThrow(IllegalStateException::new));
    }

    @Test
    void optionalArray() {
        final List<String> value = asList("a", "b");
        final Optional<Collection<String>> opt = new MockRecord(value).getOptionalArray(String.class, "ddd");
        assertTrue(opt.isPresent());
        assertEquals(value, opt.orElseThrow(IllegalStateException::new));
    }

    @Test
    void optionalDate() {
        final ZonedDateTime value = ZonedDateTime.now();
        final Optional<ZonedDateTime> opt = new MockRecord(value).getOptionalDateTime("ddd");
        assertTrue(opt.isPresent());
        assertEquals(value, opt.orElseThrow(IllegalStateException::new));
    }

    @Test
    void optionalLong() {
        final long value = 1L;
        final OptionalLong opt = new MockRecord(value).getOptionalLong("ddd");
        assertTrue(opt.isPresent());
        assertEquals(value, opt.orElseThrow(IllegalStateException::new));
    }

    /**
     * Ensure API contract will not break
     */
    @Test
    void testDefaults() {
        Record record = new MockRecord(null);
        assertThrows(UnsupportedOperationException.class, () -> record.withNewSchema(null));
    }

    @Test
    void testGetterWithEntry() {
        MockRecordWithCol record = new MockRecordWithCol();
        record.addValue("v1", "value");
        Schema.Entry e1 = new Schema.Entry() {

            @Override
            public String getName() {
                return "v1";
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
            public Schema.Type getType() {
                return null;
            }

            @Override
            public boolean isNullable() {
                return false;
            }

            @Override
            public boolean isErrorCapable() {
                return false;
            }

            @Override
            public boolean isMetadata() {
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

            @Override
            public boolean isValid() {
                return true;
            }
        };
        Assertions.assertEquals("value", record.get(String.class, e1));
    }

    @RequiredArgsConstructor
    private static class MockRecord implements Record {

        private final Object value;

        @Override
        public Schema getSchema() {
            return null;
        }

        @Override
        public <T> T get(final Class<T> expectedType, final String name) {
            return expectedType.cast(value);
        }
    }

    private static class MockRecordWithCol implements Record {

        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Schema getSchema() {
            return null;
        }

        @Override
        public <T> T get(final Class<T> expectedType, final String name) {
            return (T) values.get(name);
        }

        public <T> void addValue(String name, T value) {
            values.put(name, value);
        }
    }
}
