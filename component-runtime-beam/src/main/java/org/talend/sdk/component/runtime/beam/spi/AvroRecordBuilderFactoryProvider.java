/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import static org.talend.sdk.component.api.record.Schema.SKIP_SANITIZE_PROPERTY;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.spi.record.AvroEntryBuilder;
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
                if (!hasAvroRecordBuilderFactory()) {
                    log.warn(
                            "AvroRecordBuilderFactoryProvider if forced by System property but seems not available, this may lead to issues.");
                }
                return new AvroRecordBuilderFactory(containerId);
            default:
                if (hasAvroRecordBuilderFactory()) {
                    return new AvroRecordBuilderFactory(containerId);
                } else {
                    return new RecordBuilderFactoryImpl(containerId);
                }
        }
    }

    protected boolean hasAvroRecordBuilderFactory() {
        try {
            final ClassLoader cl = ofNullable(Thread.currentThread().getContextClassLoader())
                    .orElseGet(ClassLoader::getSystemClassLoader);
            final Class<?> c1 = cl.loadClass("com.fasterxml.jackson.databind.node.TextNode");
            final Class<?> c2 = cl.loadClass("org.talend.sdk.component.runtime.beam.spi.record.AvroSchema");
            return c1.getClassLoader().equals(c2.getClassLoader());
        } catch (final ClassNotFoundException | NoClassDefFoundError cnfe) {
            log.info("component-runtime-beam is not available, skipping AvroRecordBuilderFactory ({}).",
                    getClass().getName());
            return false;
        }
    }

    private static class AvroRecordBuilderFactory extends RecordBuilderFactoryImpl
            implements RecordBuilderFactory, Serializable {

        private AvroRecordBuilderFactory(final String plugin) {
            super(plugin);
            if (Boolean.getBoolean(SKIP_SANITIZE_PROPERTY)) {
                throw new RuntimeException("component-runtime-beam environment needs `" + SKIP_SANITIZE_PROPERTY
                        + "` property to be false.");
            }
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
            return new AvroEntryBuilder();
        }

        Object writeReplace() throws ObjectStreamException {
            if (plugin == null) {
                return DefaultServices.lookup(RecordBuilderFactory.class.getName());
            }
            return new SerializableService(plugin, RecordBuilderFactory.class.getName());
        }
    }
}
