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

import static java.util.Optional.ofNullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.checkpoint.CheckpointAvailable;
import org.talend.sdk.component.api.input.checkpoint.CheckpointData;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.LifecycleImpl;
import org.talend.sdk.component.runtime.record.RecordConverters;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InputImpl extends LifecycleImpl implements Input, Delegated {

    private transient Method next;

    private transient RecordConverters converters;

    private transient RecordConverters.MappingMetaRegistry registry;

    private transient Jsonb jsonb;

    private transient RecordBuilderFactory recordBuilderFactory;

    private transient Method checkpoint;

    private transient Method shouldCheckpoint;

    private transient Consumer<CheckpointState> checkpointCallback;

    private boolean checkpointEnabled = Boolean.parseBoolean(System.getProperty("talend.checkpoint.enabled", "false"));

    public InputImpl(final String rootName, final String name, final String plugin, final Serializable instance) {
        super(instance, rootName, name, plugin);
    }

    protected InputImpl() {
        // no-op
    }

    @Override
    public void start() {
        super.start();
        initCheckpointFunctions();
    }

    @Override
    public void start(final Consumer<CheckpointState> checkpointCallback) {
        start();
        if (checkpointEnabled) {
            this.checkpointCallback = checkpointCallback;
        }
    }

    protected void initCheckpointFunctions() {
        if (checkpointEnabled) {
            checkpoint = findMethods(CheckpointData.class).findFirst().orElse(null);
            shouldCheckpoint = findMethods(CheckpointAvailable.class).findFirst().orElse(null);
        }
    }

    @Override
    public Object next() {
        if (next == null) {
            init();
        }
        final Object data = readNext();
        if (data == null) {
            return null;
        }
        // do we need to checkpoint here?
        if (isCheckpointReady() && checkpointCallback != null) {
            checkpointCallback.accept(getCheckpoint());
        }
        final Class<?> recordClass = data.getClass();
        if (recordClass.isPrimitive() || String.class == recordClass) {
            // mainly for tests, can be dropped while build is green
            return data;
        }
        return converters.toRecord(registry, data, this::jsonb, this::recordBuilderFactory);
    }

    @Override
    public CheckpointState getCheckpoint() {
        if (checkpoint != null) {
            Object state = doInvoke(this.checkpoint);
            int version = 1;
            if (ofNullable(state.getClass().getAnnotation(Version.class)).isPresent()) {
                version = state.getClass().getAnnotation(Version.class).value();
            }
            return new CheckpointState(version, state);
        }
        return null;
    }

    @Override
    public boolean isCheckpointReady() {
        boolean checked = checkpointEnabled;
        if (shouldCheckpoint != null) {
            checked = (Boolean) doInvoke(this.shouldCheckpoint);
        }
        return checked;
    }

    @Override
    public void stop() {
        // do we need to checkpoint here?
        if (checkpointCallback != null) {
            checkpointCallback.accept(getCheckpoint());
        }
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
