/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di.studio;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Lifecycle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParameterSetter {

    private final Object delegate;

    public ParameterSetter(final Lifecycle lifecycle) {
        if (lifecycle instanceof Delegated) {
            delegate = ((Delegated) lifecycle).getDelegate();
        } else {
            throw new IllegalArgumentException("Not supported implementation of lifecycle : " + lifecycle);
        }
    }

    @Getter
    @AllArgsConstructor
    private static class TargetAndField {

        Object target;

        Field field;
    }

    private Map<String, TargetAndField> cache;

    /**
     * xxx
     */
    public void change(final String path, final Object value) {
        if (cache == null) {
            cache = new HashMap<>();
        }

        TargetAndField targetAndField = cache.get(path);

        Object target = delegate;
        Field field = null;

        if (targetAndField == null) {
            Class<?> currentClass = target.getClass();
            String[] names = path.split("\\.");
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                int index = name.indexOf('[');
                int arrayLocation = -1;
                if (index > 0) {
                    arrayLocation = Integer.valueOf(name.substring(index + 1, name.lastIndexOf(']')));
                    name = name.substring(0, index);
                }

                field = findField(name, currentClass);

                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                if (i == names.length - 1) {
                    break;
                }

                try {
                    target = field.get(target);
                    if (arrayLocation > -1) {
                        if (target instanceof List) {
                            target = List.class.cast(target).get(arrayLocation);
                        } else {
                            log.warn("expect a list, but not");
                            return;
                        }
                    }
                } catch (Exception e) {
                    log.warn("fail to get option value with path " + path, e);
                    return;
                }

                if (target == null) {
                    // now only care this case : all object must exist in the path, here only change
                    return;
                }

                currentClass = target.getClass();
            }
        } else {
            target = targetAndField.getTarget();
            field = targetAndField.getField();
        }

        try {
            field.set(target, value);
            cache.put(path, new TargetAndField(target, field));
        } catch (Exception e) {
            log.warn("fail to get option value with path " + path, e);
        }
    }

    private Field findField(final String name, final Class clazz) {
        Class<?> type = clazz;
        while (type != Object.class && type != null) {
            try {
                return type.getDeclaredField(name);
            } catch (final NoSuchFieldException e) {
                // no-op
            }
            type = type.getSuperclass();
        }
        throw new IllegalArgumentException(
                String.format("Unknown field: %s in class: %s.", name, clazz != null ? clazz.getName() : "null"));
    }

}
