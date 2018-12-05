/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.coder;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.RequiredArgsConstructor;

public class ContextualSerializableCoder<T extends Serializable> extends SerializableCoder<T> {

    private String plugin;

    protected ContextualSerializableCoder() {
        super(null, null);
    }

    private ContextualSerializableCoder(final Class<T> type, final TypeDescriptor<T> typeDescriptor,
            final String plugin) {
        super(type, typeDescriptor);
        this.plugin = plugin;
    }

    public static <T extends Serializable> SerializableCoder<T> of(final Class<T> type, final String plugin) {
        return new ContextualSerializableCoder<>(type, TypeDescriptor.of(type), plugin);
    }

    @Override
    public void encode(final T value, final OutputStream outStream) throws IOException {
        final Thread thread = Thread.currentThread();
        final ClassLoader loader = thread.getContextClassLoader();
        final LightContainer container = getContainer();
        thread.setContextClassLoader(container.classloader());
        try {
            final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outStream);
            objectOutputStream.writeObject(value);
            objectOutputStream.flush();
        } finally {
            thread.setContextClassLoader(loader);
        }
    }

    @Override
    public T decode(final InputStream inStream) throws IOException {
        final Thread thread = Thread.currentThread();
        final ClassLoader loader = thread.getContextClassLoader();
        final LightContainer container = getContainer();
        thread.setContextClassLoader(container.classloader());
        try {
            final ObjectInputStream objectInputStream =
                    new EnhancedObjectInputStream(inStream, container.classloader());
            return (T) objectInputStream.readObject();
        } catch (final ClassNotFoundException e) {
            throw new CoderException(e);
        } finally {
            thread.setContextClassLoader(loader);
        }
    }

    @Override
    public boolean equals(final Object other) {
        return !(other == null || getClass() != other.getClass())
                && getRecordType() == ContextualSerializableCoder.class.cast(other).getRecordType();
    }

    @Override
    public int hashCode() {
        return getRecordType().hashCode();
    }

    @Override
    public String toString() {
        return "ContextualSerializableCoder{plugin='" + plugin + "', clazz=" + getRecordType().getName() + "}";
    }

    private LightContainer getContainer() {
        return ContainerFinder.Instance.get().find(plugin);
    }

    Object writeReplace() throws ObjectStreamException {
        return new Replacer(plugin, getRecordType().getName());
    }

    @RequiredArgsConstructor
    private static class Replacer implements Serializable {

        private final String plugin;

        private final String className;

        Object readResolve() throws ObjectStreamException {
            final ContainerFinder containerFinder = ContainerFinder.Instance.get();
            final LightContainer container = containerFinder.find(plugin);
            final Thread thread = Thread.currentThread();
            final ClassLoader oldLoader = thread.getContextClassLoader();
            final ClassLoader classLoader = container.classloader();
            thread.setContextClassLoader(classLoader);
            try {
                final Class clazz = classLoader.loadClass(className);
                return of(clazz, plugin);
            } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException(className + " not found", e);
            } finally {
                thread.setContextClassLoader(oldLoader);
            }
        }
    }
}
