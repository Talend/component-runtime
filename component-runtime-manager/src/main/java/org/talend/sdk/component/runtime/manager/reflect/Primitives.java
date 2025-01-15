/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.reflect;

import static lombok.AccessLevel.PRIVATE;

import java.util.HashMap;
import java.util.Map;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Primitives {

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVES = new HashMap<Class<?>, Class<?>>() {

        {
            put(Integer.class, int.class);
            put(Short.class, short.class);
            put(Byte.class, byte.class);
            put(Character.class, char.class);
            put(Long.class, long.class);
            put(Float.class, float.class);
            put(Double.class, double.class);
            put(Boolean.class, boolean.class);
        }
    };

    public static Class<?> unwrap(final Class<?> type) {
        return WRAPPER_TO_PRIMITIVES.getOrDefault(type, type);
    }
}
