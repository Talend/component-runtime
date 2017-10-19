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
package org.talend.components.runtime.manager.proxy;

import java.io.Externalizable;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;

import org.talend.components.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
public class JavaProxyEnricherFactory {

    public Object asSerializable(final ClassLoader loader, final String plugin, final String key, final Object instanceToWrap) {
        final Class<?>[] interfaces = instanceToWrap.getClass().getInterfaces();
        if (Stream.of(interfaces).anyMatch(i -> i == Serializable.class || i == Externalizable.class)) {
            return instanceToWrap;
        }
        final Class[] api = Stream.concat(Stream.of(Serializable.class), Stream.of(interfaces)).toArray(Class[]::new);
        return Proxy.newProxyInstance(loader, api, new DelegatingSerializableHandler(instanceToWrap, plugin, key));
    }

    @RequiredArgsConstructor
    private static class DelegatingSerializableHandler implements InvocationHandler, Serializable {

        private final Object delegate;

        private final String plugin;

        private final String key;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (Object.class == method.getDeclaringClass()) {
                switch (method.getName()) {
                case "equals":
                    return args != null && args.length == 1 && method.getDeclaringClass().isInstance(args[0])
                            && Proxy.isProxyClass(args[0].getClass())
                            && (this == Proxy.getInvocationHandler(args[0]) || delegate == Proxy.getInvocationHandler(args[0]));
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
            return new SerializableHandlerService(plugin, key);
        }
    }

    @RequiredArgsConstructor
    public static class SerializableHandlerService implements Serializable {

        private final String plugin;

        private final String type;

        public Object readResolve() throws ObjectStreamException {
            return Proxy.getInvocationHandler(new SerializableService(plugin, type).readResolve());
        }
    }
}
