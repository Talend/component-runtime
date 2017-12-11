/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.processor;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.talend.sdk.component.api.processor.data.ObjectMap;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SubclassesCache implements Serializable {

    private String plugin;

    private ProxyGenerator proxyGenerator;

    private ClassLoader loader;

    private ConcurrentMap<Class<?>, Constructor<?>> subclasses;

    public SubclassesCache() {
        proxyGenerator = new ProxyGenerator();
        subclasses = new ConcurrentHashMap<>();
        loader = Thread.currentThread().getContextClassLoader();
    }

    public Constructor<?> find(final Class<?> type) {
        Constructor<?> constructor = subclasses.get(type);
        if (constructor == null) {
            synchronized (this) {
                constructor = subclasses.get(type);
                if (constructor == null) {
                    try {
                        constructor = doSubClass(type).getConstructor(ObjectMap.class);
                    } catch (final NoSuchMethodException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return constructor;
    }

    private Class<?> doSubClass(final Class<?> type) {
        return proxyGenerator.subclass(loader, type);
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, SubclassesCache.class.getName());
    }
}
