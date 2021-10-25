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
package org.talend.sdk.component.runtime.beam.spi;

import static java.util.Optional.ofNullable;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.record.AvroRecordBuilder;
import org.talend.sdk.component.runtime.beam.spi.record.AvroSchemaBuilder;
import org.talend.sdk.component.runtime.manager.service.DefaultServices;
import org.talend.sdk.component.runtime.manager.service.record.RecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AvroRecordBuilderFactoryProvider implements RecordBuilderFactoryProvider {

    @Override
    public RecordBuilderFactory apply(final String containerId) {
        switch (System.getProperty("talend.component.beam.record.factory.impl", "auto")) {
        case "memory":
        case "default":
            return new RecordBuilderFactoryImpl(containerId);
        case "avro":
            return new AvroRecordBuilderFactory(containerId);
        default:
            try {
                ofNullable(Thread.currentThread().getContextClassLoader())
                        .orElseGet(ClassLoader::getSystemClassLoader)
                        .loadClass("org.codehaus.jackson.node.TextNode");
                return new AvroRecordBuilderFactory(containerId);
            } catch (final ClassNotFoundException | NoClassDefFoundError cnfe) {
                log
                        .info("jackson-mapper-asl is not available, skipping AvroRecordBuilderFactory ({})",
                                getClass().getName());
                return new RecordBuilderFactoryImpl(containerId);
            }
        }
    }

    private static class AvroRecordBuilderFactory extends RecordBuilderFactoryImpl
            implements RecordBuilderFactory, Serializable {

        private AvroRecordBuilderFactory(final String plugin) {
            super(plugin);
        }

        @Override
        public Schema.Builder newSchemaBuilder(final Schema.Type type) {
            return new AvroSchemaBuilder().withType(type);
        }

        @Override
        public Record.Builder newRecordBuilder(final Schema schema) {
            return new AvroRecordBuilder(schema);
        }

        @Override
        public Record.Builder newRecordBuilder() {
            return new AvroRecordBuilder();
        }

        @Override
        public Schema.Entry.Builder newEntryBuilder() {
            return new Schema.Entry.Builder();
        }

        Object writeReplace() throws ObjectStreamException {
            if (plugin == null) {
                return DefaultServices.lookup(RecordBuilderFactory.class.getName());
            }
            return new SerializableService(plugin, RecordBuilderFactory.class.getName());
        }
    }
}
