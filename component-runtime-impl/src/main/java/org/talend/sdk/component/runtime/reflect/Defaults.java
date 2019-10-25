/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class Defaults {

    private static final Constructor<MethodHandles.Lookup> LOOKUP;

    private static final Method PRIVATE_LOOKUP;

    static {
        Constructor<MethodHandles.Lookup> lookup = null;
        Method privateLookup = null;
        try { // java 9
            lookup = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
            if (!lookup.isAccessible()) {
                lookup.setAccessible(true);
            }
        } catch (final NoSuchMethodException e) { // java 8
            try {
                privateLookup =
                        MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);
            } catch (final NoSuchMethodException ex) {
                throw new IllegalStateException("Incompatible JVM", e);
            }
        }
        PRIVATE_LOOKUP = privateLookup;
        LOOKUP = lookup;
    }

    public static boolean isDefaultAndShouldHandle(final Method method) {
        return method.isDefault();
    }

    public static MethodHandles.Lookup of(final Class<?> declaringClass) {
        try {
            if (PRIVATE_LOOKUP != null) {
                return MethodHandles.Lookup.class
                        .cast(PRIVATE_LOOKUP.invoke(null, declaringClass, MethodHandles.lookup()));
            }
            return LOOKUP.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).in(declaringClass);
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (final InvocationTargetException e) {
            throw toRuntimeException(e);
        }
    }

    public static Object handleDefault(final Class<?> declaringClass, final Method method, final Object proxy,
            final Object[] args) throws Throwable {
        return Defaults
                .of(declaringClass)
                .unreflectSpecial(method, declaringClass)
                .bindTo(proxy)
                .invokeWithArguments(args);
    }
}
