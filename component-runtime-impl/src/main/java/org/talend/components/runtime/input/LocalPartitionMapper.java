// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.input;

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

import org.talend.components.runtime.base.Delegated;
import org.talend.components.runtime.base.Named;
import org.talend.components.runtime.serialization.ContainerFinder;
import org.talend.components.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;

public class LocalPartitionMapper extends Named implements Mapper, Delegated {

    private Serializable input;

    protected LocalPartitionMapper() {
        // no-op
    }

    public LocalPartitionMapper(final String rootName, final String name, final String plugin, final Serializable instance) {
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
        return Input.class.isInstance(input) ? Input.class.cast(input) : new InputImpl(rootName(), name(), plugin(), input);
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
                throw new InvalidObjectException(e.getMessage());
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
