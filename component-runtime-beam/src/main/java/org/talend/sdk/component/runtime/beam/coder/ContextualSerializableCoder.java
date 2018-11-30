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

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.beam.sdk.coders.CoderException;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class ContextualSerializableCoder<T extends Serializable> extends CustomCoder<T> {

    private String plugin;

    private TypeDescriptor<T> typeDescriptor;

    public static <T extends Serializable> ContextualSerializableCoder<T> of(final Class<T> type, final String plugin) {
        return new ContextualSerializableCoder<>(plugin, TypeDescriptor.of(type));
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
                && typeDescriptor == ContextualSerializableCoder.class.cast(other).typeDescriptor;
    }

    @Override
    public int hashCode() {
        return typeDescriptor.hashCode();
    }

    @Override
    public TypeDescriptor<T> getEncodedTypeDescriptor() {
        return typeDescriptor;
    }

    @Override
    public void verifyDeterministic() throws NonDeterministicException {
        throw new NonDeterministicException(this, "Java Serialization, no more comment");
    }

    private LightContainer getContainer() {
        return ContainerFinder.Instance.get().find(plugin);
    }
}
