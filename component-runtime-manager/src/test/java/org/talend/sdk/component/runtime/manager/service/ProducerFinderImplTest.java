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
package org.talend.sdk.component.runtime.manager.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.source.ProducerFinder;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.manager.service.api.ComponentInstantiator;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;

class ProducerFinderImplTest {

    public static int state;

    @Test
    void find() {
        final ComponentInstantiator.Builder builder =
                (final String pluginId, final ComponentInstantiator.MetaFinder finder,
                        final ComponentManager.ComponentType componentType) -> this::instantiate;

        ProducerFinderImplTest.state = 0;
        final ProducerFinder finder = new ProducerFinderImpl().init("ThePlugin", builder, this::toRecord);
        final Iterator<Record> recordIterator = finder.find("", "", 1, null);
        int counter = 0;
        while (recordIterator.hasNext()) {
            final Record record = recordIterator.next();
            counter++;
            Assertions.assertNotNull(record, "Record is null");
            Assertions.assertEquals(Integer.toString(counter), record.getString("field"), "wrong field value");
        }
        Assertions.assertEquals(3, counter, "expected 3 records");
        Assertions.assertEquals(2, ProducerFinderImplTest.state, "not stopped ?");
    }

    @Test
    void findException() {
        final ComponentInstantiator.Builder builder =
                (final String pluginId, final ComponentInstantiator.MetaFinder finder,
                        final ComponentManager.ComponentType componentType) -> null;
        final ProducerFinder finder = new ProducerFinderImpl().init("ThePlugin", builder, this::toRecord);
        final IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> finder.find("unknownFamily", "unknownInput", 1, null));

        Assertions.assertTrue(exception.getMessage().contains("unknownFamily"),
                "exception does not contains family name");
        Assertions.assertTrue(exception.getMessage().contains("unknownInput"),
                "exception does not contains input name");
    }

    private Record toRecord(final Object object) {
        if (object instanceof Record) {
            return (Record) object;
        }
        return null;
    }

    @Test
    void serial() throws IOException, ClassNotFoundException {
        final ProducerFinder implem = new ProducerFinderImpl().init("theplugin", null, this::toRecord);

        DynamicContainerFinder.SERVICES.put(ProducerFinder.class, implem);
        DynamicContainerFinder.LOADERS.put("theplugin", Thread.currentThread().getContextClassLoader());

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(out)) {
            oos.writeObject(implem);
        }
        try (final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            final Object deserialized = ois.readObject();

            Assertions.assertSame(deserialized, implem);
        }
    }

    private Lifecycle instantiate(final Map<String, String> configuration, final int configVersion) {
        return new FakeMapper();
    }

    static class FakeMapper implements Mapper {

        @Override
        public String plugin() {
            return null;
        }

        @Override
        public String rootName() {
            return null;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public long assess() {
            return 0;
        }

        @Override
        public List<Mapper> split(long desiredSize) {
            return null;
        }

        @Override
        public Input create() {
            return new FakeInput();
        }

        @Override
        public boolean isStream() {
            return false;
        }
    }

    static class FakeInput implements Input {

        private final RecordBuilderFactory factory = new RecordBuilderFactoryImpl("test");

        private int counter = 0;

        @Override
        public String plugin() {
            return null;
        }

        @Override
        public String rootName() {
            return null;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public void start() {
            Assertions.assertEquals(0, ProducerFinderImplTest.state, "started several times ?");
            ProducerFinderImplTest.state = 1;
        }

        @Override
        public void stop() {
            Assertions.assertEquals(1, ProducerFinderImplTest.state, "stopped before started ?");
            ProducerFinderImplTest.state = 2;
        }

        @Override
        public Object next() {
            Assertions.assertEquals(1, ProducerFinderImplTest.state, "Was not started or is stopped");
            this.counter++;
            if (this.counter > 3) {
                return null;
            }
            return this.factory.newRecordBuilder().withString("field", Integer.toString(this.counter)).build();
        }

        @Override
        public void start(Consumer<Object> checkpointCallback) {
            throw new UnsupportedOperationException("#start()");
        }

        @Override
        public Object getCheckpoint() {
            throw new UnsupportedOperationException("#getCheckpoint()");
        }

        @Override
        public Boolean isCheckpointReady() {
            throw new UnsupportedOperationException("#isCheckpointReady()");
        }
    }
}