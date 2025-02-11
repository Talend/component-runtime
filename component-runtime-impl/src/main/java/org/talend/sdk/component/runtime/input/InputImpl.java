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
package org.talend.sdk.component.runtime.input;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.checkpoint.Resume;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.LifecycleImpl;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.AllArgsConstructor;

public class InputImpl extends LifecycleImpl implements Input, Delegated, Checkpoint {

    private transient Method resume;

    private transient Method next;

    private transient Method checkpoint;

    private transient RecordConverters converters;

    private transient RecordConverters.MappingMetaRegistry registry;

    private transient Jsonb jsonb;

    private transient RecordBuilderFactory recordBuilderFactory;

    public InputImpl(final String rootName, final String name, final String plugin, final Serializable instance) {
        super(instance, rootName, name, plugin);
    }

    protected InputImpl() {
        // no-op
    }

    @Override
    public void start() {
        super.start();
        //
        // do we need to resume from latest checkpoint?
        //
        resume = findMethods(Resume.class).findFirst().orElse(null);
        checkpoint = findMethods(org.talend.sdk.component.api.input.checkpoint.Checkpoint.class).findFirst().orElse(null);
    }

    @Override
    public void resume(Object checkpoint) {
        if (this.resume != null) {
            doInvoke(this.resume, checkpoint);
        }
    }

    @Override
    public Object next() {
        if (next == null) {
            init();
        }
        final Object record = readNext();
        if (record == null) {
            return null;
        }
        final Class<?> recordClass = record.getClass();
        if (recordClass.isPrimitive() || String.class == recordClass) {
            // mainly for tests, can be dropped while build is green
            return record;
        }
        return converters.toRecord(registry, record, this::jsonb, this::recordBuilderFactory);
    }

    @Override
    public Object checkpoint() {
        if (checkpoint != null) {
            return doInvoke(this.checkpoint);
        }
        return null;
    }

    @Override
    public void stop() {
        //
        // serialize the current state to be able to resume later
        //
        super.stop();
    }

    @Override
    public Object getDelegate() {
        return delegate;
    }

    protected Object readNext() {
        return doInvoke(this.next);
    }

    protected void init() {
        next = findMethods(Producer.class).findFirst().get();
        converters = new RecordConverters();
        registry = new RecordConverters.MappingMetaRegistry();
    }

    private Jsonb jsonb() {
        if (jsonb != null) {
            return jsonb;
        }
        synchronized (this) {
            if (jsonb == null) {
                final LightContainer container = ContainerFinder.Instance.get().find(plugin());
                jsonb = container.findService(Jsonb.class);
            }
        }
        return jsonb;
    }

    private RecordBuilderFactory recordBuilderFactory() {
        if (recordBuilderFactory != null) {
            return recordBuilderFactory;
        }
        synchronized (this) {
            if (recordBuilderFactory == null) {
                final LightContainer container = ContainerFinder.Instance.get().find(plugin());
                recordBuilderFactory = container.findService(RecordBuilderFactory.class);
            }
        }
        return recordBuilderFactory;
    }

    protected Object writeReplace() throws ObjectStreamException {
        return new SerializationReplacer(plugin(), rootName(), name(), serializeDelegate());
    }

    @AllArgsConstructor
    protected static class SerializationReplacer implements Serializable {

        protected String plugin;

        protected String component;

        protected String name;

        protected byte[] value;

        protected Object readResolve() throws ObjectStreamException {
            try {
                return new InputImpl(component, name, plugin, loadDelegate());
            } catch (final IOException | ClassNotFoundException e) {
                final InvalidObjectException invalidObjectException = new InvalidObjectException(e.getMessage());
                invalidObjectException.initCause(e);
                throw invalidObjectException;
            }
        }

        protected Serializable loadDelegate() throws IOException, ClassNotFoundException {
            try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(value),
                    ContainerFinder.Instance.get().find(plugin).classloader())) {
                return Serializable.class.cast(ois.readObject());
            }
        }
    }
}
