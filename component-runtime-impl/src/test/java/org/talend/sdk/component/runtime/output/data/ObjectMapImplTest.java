/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.output.data;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.talend.sdk.component.api.processor.data.ObjectMap;
import org.talend.sdk.component.runtime.serialization.Serializer;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

public class ObjectMapImplTest {

    private final ObjectMap map = new ObjectMapImpl(null,
            new Person(new Address("here"), singleton(new Address("other")), 30, "tester", asList("t1", "t2")),
            new AccessorCache("null"));

    @Test
    public void any() {
        final Sink sink = new Sink(singletonMap("def", "value"));
        final ObjectMapImpl objectMap = new ObjectMapImpl(null, sink, new AccessorCache(null));
        assertEquals("value", objectMap.get("def"));
        assertNull(objectMap.get("missing"));
        assertEquals(singleton("def"), objectMap.keys());
    }

    @Test
    public void serialization() throws IOException, ClassNotFoundException {
        final ObjectMap copy = Serializer.roundTrip(map);
        assertNotNull(copy);
    }

    @Test
    public void keys() {
        assertEquals(new HashSet<>(asList("subAddresses", "address", "name", "age", "tags")), map.keys());
    }

    @Test
    public void firstLevelField() {
        assertEquals(new Address("here"), map.get("address"));
        assertEquals(30, (int) map.get("age"), 0);
        assertEquals("tester", map.get("name"));
        assertEquals(singleton(new Address("other")), map.get("subAddresses"));
    }

    @Test
    public void linearlyNested() {
        assertEquals("here", map.get("address.street"));
    }

    @Test
    public void nestedMaps() {
        final ObjectMap nested = map.getMap("address");
        assertEquals("here", nested.get("street"));
    }

    @Test
    public void map() {
        final Map<String, Object> map = new HashMap<>();
        map.put("k1", "v1");
        map.put("k2", new Address("test"));
        map.put("k3", singletonMap("k4", "v4"));
        final ObjectMap wrapper = new ObjectMapImpl(null, map, new AccessorCache("null"));
        assertEquals("v1", wrapper.get("k1"));
        assertEquals(new Address("test"), wrapper.get("k2"));
        assertEquals(singletonMap("k4", "v4"), wrapper.get("k3"));
        assertEquals("v4", wrapper.get("k3.k4"));
    }

    @AllArgsConstructor
    public static class Person implements Serializable {

        private Address address;

        private Collection<Address> subAddresses;

        private int age;

        private String name;

        private List<String> tags;
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    public static class Address implements Serializable {

        private String street;
    }

    @EqualsAndHashCode
    @AllArgsConstructor
    public static class Sink implements Serializable {

        @ObjectMap.Any
        private Map<String, Object> values;
    }
}
