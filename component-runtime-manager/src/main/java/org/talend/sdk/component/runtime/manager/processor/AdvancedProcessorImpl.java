/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.processor;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import org.talend.sdk.component.api.processor.data.ObjectMap;
import org.talend.sdk.component.runtime.output.ProcessorImpl;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;

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
