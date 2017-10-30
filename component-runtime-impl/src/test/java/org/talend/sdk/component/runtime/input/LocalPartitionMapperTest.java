/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.runtime.input;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;

import org.junit.Test;
import org.talend.sdk.component.runtime.serialization.Serializer;
import org.talend.sdk.component.api.input.Producer;

public class LocalPartitionMapperTest {

    @Test
    public void split() {
        final Mapper mapper = new LocalPartitionMapper("Root", "Test", "Plugin", null);
        assertEquals(1, mapper.assess());
        assertEquals(singletonList(mapper), mapper.split(1));
        assertEquals(singletonList(mapper), mapper.split(10));
        assertEquals(singletonList(mapper), mapper.split(-159));
    }

    @Test
    public void createReader() {
        final Mapper mapper = new LocalPartitionMapper("Root", "Test", "Plugin", new Component());
        final Input input = mapper.create();
        assertTrue(Sample.class.isInstance(input.next()));
        assertNotSame(input.next(), input.next());
    }

    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        final LocalPartitionMapper mapper = Serializer
                .roundTrip(new LocalPartitionMapper("Root", "Test", "Plugin", new Component()));
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
