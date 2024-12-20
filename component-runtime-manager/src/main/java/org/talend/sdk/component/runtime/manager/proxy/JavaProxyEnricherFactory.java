/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.proxy;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.Externalizable;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.reflect.Defaults;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
public class JavaProxyEnricherFactory {

    public Object asSerializable(final ClassLoader loader, final String plugin, final String key,
            final Object instanceToWrap) {
        return this.asSerializable(loader, plugin, key, instanceToWrap, false);
    }

    public Object asSerializable(final ClassLoader loader, final String plugin, final String key,
            final Object instanceToWrap, final boolean force) {
        final Class<?>[] interfaces = instanceToWrap.getClass().getInterfaces();
        final boolean isSerializable =
                Stream.of(interfaces).anyMatch(i -> i == Serializable.class || i == Externalizable.class);
        if ((!force) && isSerializable && !instanceToWrap.getClass().getName().startsWith("org.apache.johnzon.core.")) {
            return instanceToWrap;
        }
        final Class[] api = isSerializable ? interfaces
                : Stream.concat(Stream.of(Serializable.class), Stream.of(interfaces)).toArray(Class[]::new);
        return Proxy
                .newProxyInstance(selectLoader(api, loader), api,
                        new DelegatingSerializableHandler(instanceToWrap, plugin, key));
    }

    private ClassLoader selectLoader(final Class[] api, final ClassLoader loader) {
        if (Stream.of(api).anyMatch(t -> t.getClassLoader() == loader) || loader.getParent() == null
                || loader == getSystemClassLoader()) {
            return loader;
        }
        final ClassLoader parent = loader.getParent();
        if (parent == null) {
            return getSystemClassLoader();
        }
        for (final Class<?> test : api) {
            try {
                parent.loadClass(test.getName());
            } catch (final ClassNotFoundException e) {
                for (final Class<?> test2 : api) {
                    try {
                        loader.loadClass(test2.getName());
                    } catch (final ClassNotFoundException ex) {
                        throw new IllegalStateException("No matching classloader for " + asList(api) + ":\n"
                                + "Current: " + loader + "\n" + "Parent: " + parent + "\n" + "API: "
                                + Stream.of(api).collect(toMap(identity(), Class::getClassLoader)) + "\n");
                    }
                }
                return loader;
            }
        }
        return parent;
    }

    @RequiredArgsConstructor
    private static class DelegatingSerializableHandler implements InvocationHandler, Serializable {

        private final Object delegate;

        private final String plugin;

        private final String key;

        private final ConcurrentMap<Method, Boolean> defaultMethods = new ConcurrentHashMap<>();

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (Defaults.isDefaultAndShouldHandle(method) && defaultMethods.computeIfAbsent(method, m -> {
                try {
                    delegate.getClass().getMethod(method.getName(), method.getParameterTypes());
                    return false;
                } catch (final NoSuchMethodException e) {
                    return true;
                }
            })) {
                return Defaults.handleDefault(method.getDeclaringClass(), method, proxy, args);
            }

            if (Object.class == method.getDeclaringClass()) {
                switch (method.getName()) {
                    case "equals":
                        return args != null && args.length == 1 && method.getDeclaringClass().isInstance(args[0])
                                && Proxy.isProxyClass(args[0].getClass())
                                && (this == Proxy.getInvocationHandler(args[0])
                                        || delegate == Proxy.getInvocationHandler(args[0]));
                    default:
                }
            }
            try {
                return method.invoke(delegate, args);
            } catch (final InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }

        Object writeReplace() throws ObjectStreamException {
            return new SerializationHandlerReplacer(plugin, key);
        }
    }
}
