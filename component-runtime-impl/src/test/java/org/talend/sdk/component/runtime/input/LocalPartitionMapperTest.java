/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.input;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.serialization.Serializer;

public class LocalPartitionMapperTest {

    @Test
    void split() {
        final Mapper mapper = new LocalPartitionMapper("Root", "Test", "Plugin", null);
        assertEquals(1, mapper.assess());
        assertEquals(singletonList(mapper), mapper.split(1));
        assertEquals(singletonList(mapper), mapper.split(10));
        assertEquals(singletonList(mapper), mapper.split(-159));
    }

    @Test
    void createReader() {
        final Mapper mapper = new LocalPartitionMapper("Root", "Test", "Plugin", new Component());
        final Input input = mapper.create();
        assertTrue(Record.class.isInstance(input.next()));
        assertNotSame(input.next(), input.next());
    }

    @Test
    void serialization() throws IOException, ClassNotFoundException {
        final LocalPartitionMapper mapper =
                Serializer.roundTrip(new LocalPartitionMapper("Root", "Test", "Plugin", new Component()));
        assertEquals("Root", mapper.rootName());
        assertEquals("Test", mapper.name());
        assertEquals("Plugin", mapper.plugin());

        final Input input = mapper.create();
        assertNotNull(input);
    }

    public static class Component implements Serializable {

        @Producer
        public Sample next() {
            return new Sample();
        }
    }

    public static class Sample {
    }
}
