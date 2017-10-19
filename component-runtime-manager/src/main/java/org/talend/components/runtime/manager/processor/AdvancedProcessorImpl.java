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
package org.talend.components.runtime.manager.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.talend.component.api.processor.data.ObjectMap;
import org.talend.components.runtime.output.ProcessorImpl;
import org.talend.components.runtime.serialization.ContainerFinder;

import lombok.AllArgsConstructor;

// processor impl supporting subclassing for the chaining of objectmaps
public class AdvancedProcessorImpl extends ProcessorImpl {

    private transient SubclassesCache cache;

    protected AdvancedProcessorImpl() {
        // no-op
    }

    public AdvancedProcessorImpl(final String plugin, final String family, final String name, final Serializable delegate) {
        super(plugin, family, name, delegate);
    }

    @Override
    protected Object newSubClassInstance(final Class<?> type, final ObjectMap data) {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = ofNullable(ContainerFinder.Instance.get().find(plugin()).findService(SubclassesCache.class))
                            .orElseGet(SubclassesCache::new);
                }
            }
        }
        try {
            return cache.find(type).newInstance(data);
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new AdvancedSerializationReplacer(plugin(), rootName(), name(), serializeDelegate());
    }

    @AllArgsConstructor
    private static class AdvancedSerializationReplacer implements Serializable {

        private final String plugin;

        private final String component;

        private final String name;

        private final byte[] value;

        Object readResolve() throws ObjectStreamException {
            try {
                return new ProcessorImpl(component, name, plugin, loadDelegate(value, plugin));
            } catch (final IOException | ClassNotFoundException e) {
                throw new InvalidObjectException(e.getMessage());
            }
        }
    }
}
