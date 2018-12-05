/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.serialization;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.NoArgsConstructor;

public class EnhancedObjectInputStream extends ObjectInputStream {

    private final ClassLoader loader;

    private final Predicate<String> classesWhitelist;

    protected EnhancedObjectInputStream(final InputStream in, final ClassLoader loader, final Predicate<String> filter)
            throws IOException {
        super(in);
        this.loader = loader;
        this.classesWhitelist = filter;
    }

    public EnhancedObjectInputStream(final InputStream in, final ClassLoader loader) throws IOException {
        this(in, loader, Defaults.SECURITY_FILTER_WHITELIST);
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc) throws ClassNotFoundException {
        final String name = desc.getName();
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

        doSecurityCheck(name);

        try {
            return Class.forName(name, false, loader);
        } catch (final ClassNotFoundException e) {
            // try again from beam classloader for complex classloader graphs,
            // really a fallback mode
            return Class.forName(name, false, getClass().getClassLoader());
        }
    }

    @Override
    protected Class<?> resolveProxyClass(final String[] interfaces) throws ClassNotFoundException {
        final Class[] interfaceTypes = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            doSecurityCheck(interfaces[i]);
            interfaceTypes[i] = Class.forName(interfaces[i], false, loader);
        }

        try {
            return Proxy.getProxyClass(loader, interfaceTypes);
        } catch (final IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    private void doSecurityCheck(final String name) {
        if (!classesWhitelist.test(processForWhiteListing(name))) {
            throw new SecurityException("'" + name + "' not supported, add it in "
                    + "-Dtalend.component.runtme.serialization.java" + ".inputstream.whitelist if needed");
        }
    }

    private String processForWhiteListing(final String name) {
        if (name.startsWith("[L") && name.endsWith(";")) {
            return name.substring(2, name.length() - 1);
        }
        return name;
    }

    @NoArgsConstructor(access = PRIVATE)
    static class Defaults {

        static final Predicate<String> SECURITY_FILTER_WHITELIST =
                ofNullable(System.getProperty("talend.component.runtime.serialization.java.inputstream.whitelist"))
                        .map(s -> Stream
                                .of(s.split(","))
                                .map(String::trim)
                                .filter(it -> !it.isEmpty())
                                .collect(toList()))
                        .map(l -> (Predicate<String>) name -> l.stream().anyMatch(name::startsWith))
                        .orElseGet(() -> name -> true);
    }
}
