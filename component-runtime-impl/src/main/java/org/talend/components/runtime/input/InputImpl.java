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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.talend.component.api.input.Producer;
import org.talend.components.runtime.base.Delegated;
import org.talend.components.runtime.base.LifecycleImpl;
import org.talend.components.runtime.serialization.ContainerFinder;
import org.talend.components.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;

public class InputImpl extends LifecycleImpl implements Input, Delegated {

    private transient Method next;

    public InputImpl(final String rootName, final String name, final String plugin, final Serializable instance) {
        super(instance, rootName, name, plugin);
    }

    protected InputImpl() {
        // no-op
    }

    @Override
    public Object next() {
        if (next == null) {
            next = findMethods(Producer.class).findFirst().get();
        }
        return doInvoke(next);
    }

    @Override
    public Object getDelegate() {
        return delegate;
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializationReplacer(plugin(), rootName(), name(), serializeDelegate());
    }

    @AllArgsConstructor
    private static class SerializationReplacer implements Serializable {

        private final String plugin;

        private final String component;

        private final String name;

        private final byte[] value;

        Object readResolve() throws ObjectStreamException {
            try {
                return new InputImpl(component, name, plugin, loadDelegate());
            } catch (final IOException | ClassNotFoundException e) {
                throw new InvalidObjectException(e.getMessage());
            }
        }

        private Serializable loadDelegate() throws IOException, ClassNotFoundException {
            try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(value),
                    ContainerFinder.Instance.get().find(plugin).classloader())) {
                return Serializable.class.cast(ois.readObject());
            }
        }
    }
}
