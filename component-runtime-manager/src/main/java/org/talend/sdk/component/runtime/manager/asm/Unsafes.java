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
package org.talend.sdk.component.runtime.manager.asm;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;

public class Unsafes {

    private static final Object UNSAFE;

    private static final Method UNSAFE_DEFINE_CLASS;

    static {
        Class<?> unsafeClass;
        try {
            unsafeClass = AccessController.doPrivileged((PrivilegedAction<Class<?>>) () -> {
                try {
                    return Thread.currentThread().getContextClassLoader().loadClass("sun.misc.Unsafe");
                } catch (final Exception e) {
                    try {
                        return ClassLoader.getSystemClassLoader().loadClass("sun.misc.Unsafe");
                    } catch (ClassNotFoundException e1) {
                        throw new IllegalStateException("Cannot get sun.misc.Unsafe", e);
                    }
                }
            });
        } catch (final Exception e) {
            throw new IllegalStateException("Cannot get sun.misc.Unsafe class", e);
        }

        UNSAFE = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try {
                final Field field = unsafeClass.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return field.get(null);
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        });

        if (UNSAFE != null) {
            UNSAFE_DEFINE_CLASS = AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
                try {
                    return unsafeClass.getDeclaredMethod("defineClass", String.class, byte[].class, int.class,
                        int.class, ClassLoader.class, ProtectionDomain.class);
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot get Unsafe.defineClass", e);
                }
            });
        } else {
            UNSAFE_DEFINE_CLASS = null;
        }
    }

    /**
     * The 'defineClass' method on the ClassLoader is private, thus we need to
     * invoke it via reflection.
     *
     * @return the Class which got loaded in the classloader
     */
    public static <T> Class<T> defineAndLoadClass(final ClassLoader classLoader, final String proxyName,
        final byte[] proxyBytes) {
        Class<?> clazz = classLoader.getClass();

        Method defineClassMethod = null;
        do {
            try {
                defineClassMethod =
                    clazz.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            } catch (NoSuchMethodException e) {
                // do nothing, we need to search the superclass
            }

            clazz = clazz.getSuperclass();
        } while (defineClassMethod == null && clazz != Object.class);

        if (defineClassMethod != null && !defineClassMethod.isAccessible()) {
            try {
                defineClassMethod.setAccessible(true);
            } catch (final RuntimeException re) { // likely j9, let's use unsafe
                defineClassMethod = null;
            }
        }

        try {
            Class<T> definedClass;

            if (defineClassMethod != null) {
                definedClass =
                    (Class<T>) defineClassMethod.invoke(classLoader, proxyName, proxyBytes, 0, proxyBytes.length);
            } else {
                definedClass = (Class<T>) UNSAFE_DEFINE_CLASS.invoke(UNSAFE, proxyName, proxyBytes, 0,
                    proxyBytes.length, classLoader, null);
            }

            return (Class<T>) Class.forName(definedClass.getName(), true, classLoader);
        } catch (final InvocationTargetException le) {
            if (LinkageError.class.isInstance(le.getCause())) {
                try {
                    return (Class<T>) Class.forName(proxyName.replace('/', '.'), true, classLoader);
                } catch (ClassNotFoundException e) {
                    // default error handling
                }
            }
            throw new IllegalStateException(le.getCause());
        } catch (final Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
