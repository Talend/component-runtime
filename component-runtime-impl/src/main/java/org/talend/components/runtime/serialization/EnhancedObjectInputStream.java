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
package org.talend.components.runtime.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

// note that it ignores the 0-day vulnerability since it is already in a cluster considered secured
public class EnhancedObjectInputStream extends ObjectInputStream {

    private final ClassLoader loader;

    public EnhancedObjectInputStream(final InputStream in, final ClassLoader loader) throws IOException {
        super(in);
        this.loader = loader;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        final String name = desc.getName();
        try {
            return Class.forName(name, false, loader);
        } catch (final ClassNotFoundException e) {
            // handle primitives, rare so done in fallback mode
            if (name.equals("boolean")) {
                return boolean.class;
            }
            if (name.equals("byte")) {
                return byte.class;
            }
            if (name.equals("char")) {
                return char.class;
            }
            if (name.equals("short")) {
                return short.class;
            }
            if (name.equals("int")) {
                return int.class;
            }
            if (name.equals("long")) {
                return long.class;
            }
            if (name.equals("float")) {
                return float.class;
            }
            if (name.equals("double")) {
                return double.class;
            }

            // try again from beam classloader for complex classloader graphs,
            // really a fallback mode
            return Class.forName(name, false, getClass().getClassLoader());
        }
    }

    @Override
    protected Class<?> resolveProxyClass(final String[] interfaces) throws IOException, ClassNotFoundException {
        final Class[] interfaceTypes = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceTypes[i] = Class.forName(interfaces[i], false, loader);
        }

        try {
            return Proxy.getProxyClass(loader, interfaceTypes);
        } catch (final IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }
}
