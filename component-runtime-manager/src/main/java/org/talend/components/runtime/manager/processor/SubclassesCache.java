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

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.talend.component.api.processor.data.ObjectMap;
import org.talend.components.runtime.manager.asm.ProxyGenerator;
import org.talend.components.runtime.serialization.SerializableService;

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
        return new Replacer(plugin);
    }

    @AllArgsConstructor
    private static class Replacer implements Serializable {

        private final String plugin;

        Object readResolve() throws ObjectStreamException {
            return new SerializableService(plugin, SubclassesCache.class.getName());
        }
    }
}
