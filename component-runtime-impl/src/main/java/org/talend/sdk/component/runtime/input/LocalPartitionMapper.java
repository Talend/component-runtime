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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Named;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;

public class LocalPartitionMapper extends Named implements Mapper, Delegated {

    private Serializable input;

    protected LocalPartitionMapper() {
        // no-op
    }

    public LocalPartitionMapper(final String rootName, final String name, final String plugin,
            final Serializable instance) {
        super(rootName, name, plugin);
        this.input = instance;
    }

    @Override
    public long assess() {
        return 1;
    }

    @Override
    public List<Mapper> split(final long desiredSize) {
        return new ArrayList<>(singletonList(this));
    }

    @Override
    public Input create() {
        return Input.class.isInstance(input) ? Input.class.cast(input)
                : new InputImpl(rootName(), name(), plugin(), input);
    }

    @Override
    public boolean isStream() {
        return false;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }

    @Override
    public Object getDelegate() {
        return input;
    }

    Object writeReplace() throws ObjectStreamException {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread.setContextClassLoader(input.getClass().getClassLoader());
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(input);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            thread.setContextClassLoader(old);
        }
        return new SerializationReplacer(plugin(), rootName(), name(), baos.toByteArray());
    }

    @AllArgsConstructor
    private static class SerializationReplacer implements Serializable {

        private final String plugin;

        private final String component;

        private final String name;

        private final byte[] input;

        Object readResolve() throws ObjectStreamException {
            try {
                return new LocalPartitionMapper(component, name, plugin, loadDelegate());
            } catch (final IOException | ClassNotFoundException e) {
                final InvalidObjectException invalidObjectException = new InvalidObjectException(e.getMessage());
                invalidObjectException.initCause(e);
                throw invalidObjectException;
            }
        }

        private Serializable loadDelegate() throws IOException, ClassNotFoundException {
            try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(input),
                    ContainerFinder.Instance.get().find(plugin).classloader())) {
                return Serializable.class.cast(ois.readObject());
            }
        }
    }
}
