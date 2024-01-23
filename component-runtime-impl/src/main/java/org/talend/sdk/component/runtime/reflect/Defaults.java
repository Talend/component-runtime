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
package org.talend.sdk.component.runtime.reflect;

import static lombok.AccessLevel.PRIVATE;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class Defaults {

    private static final Handler HANDLER;

    static {
        final int javaVersion = JavaVersion.major();
        final Boolean isJava8 = javaVersion == 8 ? true : false;
        if (javaVersion > 8 && javaVersion < 17) {
            try {
                /**
                 * Disable Access Warnings: only available below jdk17 (JEP403)
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
        final Constructor<MethodHandles.Lookup> constructor = findLookupConstructor(isJava8);
        if (isJava8) { // j8
            HANDLER = (clazz, method, proxy, args) -> constructor
                    .newInstance(clazz, MethodHandles.Lookup.PRIVATE)
                    .unreflectSpecial(method, clazz)
                    .bindTo(proxy)
                    .invokeWithArguments(args);
        } else { // j > 8 - can need some --add-opens, we will add a module-info later to be clean when dropping j8
            final Method privateLookup = findPrivateLookup();
            HANDLER = (clazz, method, proxy, args) -> MethodHandles.Lookup.class
                    .cast(privateLookup.invoke(null, clazz, constructor.newInstance(clazz)))
                    .unreflectSpecial(method, clazz)
                    .bindTo(proxy)
                    .invokeWithArguments(args);
        }
    }

    public static boolean isDefaultAndShouldHandle(final Method method) {
        return method.isDefault();
    }

    public static Object handleDefault(final Class<?> declaringClass, final Method method, final Object proxy,
            final Object[] args) throws Throwable {
        return HANDLER.handle(declaringClass, method, proxy, args);
    }

    private interface Handler {

        Object handle(Class<?> clazz, Method method, Object proxy, Object[] args) throws Throwable;
    }

    private static Method findPrivateLookup() {
        try {
            return MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Constructor<MethodHandles.Lookup> findLookupConstructor(final Boolean isJava8) {
        try {
            Constructor<MethodHandles.Lookup> constructor;
            if (isJava8) {
                constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, int.class);
            } else {
                constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class);
            }
            if (!constructor.isAccessible()) {
                // this needs the `--add-opens java.base/java.lang.invoke=ALL-UNNAMED` jvm flag when java9+.
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
