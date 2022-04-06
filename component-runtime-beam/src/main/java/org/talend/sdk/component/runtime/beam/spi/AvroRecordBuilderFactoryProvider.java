/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
import org.talend.sdk.component.runtime.record.SchemaImpl;
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
                // This part is tricky, we need to ensure that we've everything needed to instantiate an ARBF to user.
                // Testing w/ loadClass won't be accurate enough as we may load all needed classes but w/ distinct cl
                // like ConfigurableClassLoader vs Launcher$AppClassLoader.
                // Safest way is to try a direct instantiation.
                final Record r = new AvroRecordBuilderFactory(containerId)
                        .newRecordBuilder()
                        .withString("test", "instantiation")
                        .build();
                log.warn("[apply] AvroRecord instantiated.");
                //
                return new AvroRecordBuilderFactory(containerId);
            } catch (final NoClassDefFoundError cnfe) {
                log.warn("[apply] Error: ", cnfe);
                log.info("component-runtime-beam is not available, skipping AvroRecordBuilderFactory ({})",
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
            return new SchemaImpl.EntryImpl.BuilderImpl();
        }

        Object writeReplace() throws ObjectStreamException {
            if (plugin == null) {
                return DefaultServices.lookup(RecordBuilderFactory.class.getName());
            }
            return new SerializableService(plugin, RecordBuilderFactory.class.getName());
        }
    }
}
