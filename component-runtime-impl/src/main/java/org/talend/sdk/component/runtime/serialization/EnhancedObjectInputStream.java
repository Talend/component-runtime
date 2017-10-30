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
package org.talend.sdk.component.runtime.serialization;

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
