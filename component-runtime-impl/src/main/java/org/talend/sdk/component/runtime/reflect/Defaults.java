/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Defaults {

    private static final Constructor<MethodHandles.Lookup> LOOKUP;

    static {
        try {
            LOOKUP = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Integer.TYPE);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException("Incompatible JVM", e);
        }
        if (!LOOKUP.isAccessible()) {
            LOOKUP.setAccessible(true);
        }
    }

    public static MethodHandles.Lookup of(final Class<?> declaringClass) {
        try {
            return LOOKUP.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE).in(declaringClass);
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (final InvocationTargetException e) {
            throw toRuntimeException(e);
        }
    }
}
