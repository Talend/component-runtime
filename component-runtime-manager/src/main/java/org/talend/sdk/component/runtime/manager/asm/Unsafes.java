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
package org.talend.sdk.component.runtime.manager.asm;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PRIVATE;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.stream.Stream;

import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.runtime.reflect.JavaVersion;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
// as deprecation was introduced since = "17", can ignore it for now...
@SuppressWarnings({ "deprecation", "removal" })
public final class Unsafes {

    private static final Object UNSAFE;

    private static final Object INTERNAL_UNSAFE;

    private static final Method UNSAFE_DEFINE_CLASS;

    static {
        Class<?> unsafeClass;
        final int javaVersion = JavaVersion.major();
        if (javaVersion > 8 && javaVersion < 17) {
            try {
                /**
                 * Disable Access Warnings:
                 *
                 * <pre>
                 * {@code
                 * WARNING: An illegal reflective access operation has occurred
                 * WARNING: Illegal reflective access by org.talend.sdk.component.runtime.manager.asm.Unsafes \
                 * (file:/xxxx/component-runtime-manager-x.xx.x.jar) to method java.lang.ClassLoader.defineClass(java.lang.String,byte[],int,int)
                 * WARNING: Please consider reporting this to the maintainers of org.talend.sdk.component.runtime.manager.asm.Unsafes
                 * WARNING: Use --illegal-access=warn to enable warnings of further illegal reflective access operations
                 * WARNING: All illegal access operations will be denied in a future release
                }
                 * </pre>
                 */
                Class unsafeClazz = Class.forName("sun.misc.Unsafe");
                Field field = unsafeClazz.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                Object unsafe = field.get(null);
                Method putObjectVolatile =
                        unsafeClazz.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
                Method staticFieldOffset = unsafeClazz.getDeclaredMethod("staticFieldOffset", Field.class);
                Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
                Field loggerField = loggerClass.getDeclaredField("logger");
                Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
                putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
            } catch (Exception e) {
                System.err.println("Disabling unsafe warnings failed: " + e.getMessage());
            }
        }
        try {
            unsafeClass = AccessController
                    .doPrivileged((PrivilegedAction<Class<?>>) () -> Stream
                            .of(Thread.currentThread().getContextClassLoader(), ClassLoader.getSystemClassLoader())
                            .flatMap(classloader -> Stream
                                    .of("sun.misc.Unsafe", "jdk.internal.misc.Unsafe")
                                    .flatMap(name -> {
                                        try {
                                            return Stream.of(classloader.loadClass(name));
                                        } catch (final ClassNotFoundException e) {
                                            return Stream.empty();
                                        }
                                    }))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Cannot get Unsafe")));
        } catch (final Exception e) {
            throw new IllegalStateException("Cannot get Unsafe class", e);
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
        INTERNAL_UNSAFE = AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
            try { // j11, unwrap unsafe, it owns defineClass now and no more theUnsafe
                final Field theInternalUnsafe = unsafeClass.getDeclaredField("theInternalUnsafe");
                theInternalUnsafe.setAccessible(true);
                return theInternalUnsafe.get(null).getClass();
            } catch (final Exception notJ11OrMore) {
                return UNSAFE;
            }
        });

        if (UNSAFE != null) {
            UNSAFE_DEFINE_CLASS = AccessController.doPrivileged((PrivilegedAction<Method>) () -> {
                try {
                    return INTERNAL_UNSAFE
                            .getClass()
                            .getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class,
                                    ClassLoader.class, ProtectionDomain.class);
                } catch (final Exception e) {
                    return null;
                }
            });

            try {
                final Class<?> rootLoaderClass = Class.forName("java.lang.ClassLoader");
                rootLoaderClass
                        .getDeclaredMethod("defineClass",
                                new Class[] { String.class, byte[].class, int.class, int.class })
                        .setAccessible(true);
                rootLoaderClass
                        .getDeclaredMethod("defineClass",
                                new Class[] { String.class, byte[].class, int.class, int.class,
                                        ProtectionDomain.class })
                        .setAccessible(true);
            } catch (final Exception e) {
                try { // some j>8, since we have unsafe let's use it
                    final Class<?> rootLoaderClass = Class.forName("java.lang.ClassLoader");
                    final Method objectFieldOffset =
                            UNSAFE.getClass().getDeclaredMethod("objectFieldOffset", Field.class);
                    final Method putBoolean =
                            UNSAFE.getClass().getDeclaredMethod("putBoolean", Object.class, long.class, boolean.class);
                    objectFieldOffset.setAccessible(true);
                    final long accOffset = Long.class
                            .cast(objectFieldOffset
                                    .invoke(UNSAFE, AccessibleObject.class.getDeclaredField("override")));
                    putBoolean
                            .invoke(UNSAFE,
                                    rootLoaderClass
                                            .getDeclaredMethod("defineClass",
                                                    new Class[] { String.class, byte[].class, int.class, int.class }),
                                    accOffset, true);
                    putBoolean
                            .invoke(UNSAFE,
                                    rootLoaderClass
                                            .getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class,
                                                    int.class, int.class, ProtectionDomain.class }),
                                    accOffset, true);
                } catch (final Exception ex) {
                    // no-op: no more supported by the JVM
                }
            }
        } else {
            UNSAFE_DEFINE_CLASS = null;
        }
    }

    /**
     * The 'defineClass' method on the ClassLoader is private, thus we need to
     * invoke it via reflection.
     *
     * @param classLoader the classloader to use to define the proxy.
     * @param proxyName the class name to define.
     * @param proxyBytes the bytes of the class to define.
     * @param <T> the Class type
     *
     * @return the Class which got loaded in the classloader
     */
    public static <T> Class<T> defineAndLoadClass(final ClassLoader classLoader, final String proxyName,
            final byte[] proxyBytes) {
        if (ConfigurableClassLoader.class.isInstance(classLoader)) {
            return (Class<T>) ConfigurableClassLoader.class
                    .cast(classLoader)
                    .registerBytecode(proxyName.replace('/', '.'), proxyBytes);
        }
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
                requireNonNull(UNSAFE_DEFINE_CLASS, "No Unsafe.defineClass available");
                definedClass = (Class<T>) UNSAFE_DEFINE_CLASS
                        .invoke(UNSAFE, proxyName, proxyBytes, 0, proxyBytes.length, classLoader, null);
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
