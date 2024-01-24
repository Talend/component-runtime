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
package org.talend.sdk.component.runtime.beam.factory.service;

import static java.beans.Introspector.decapitalize;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.xbean.propertyeditor.PropertyEditorException;
import org.apache.xbean.propertyeditor.PropertyEditors;

/**
 * Intended to be used to instantiate from properties or a mix properties/object
 * Beam IO developped with @AutoValue and using a fluent API.
 */
// dev note: this could be in the API and in main services but it is "fragile" so better to keep it a bit hidden
// @Service - for doc purposes only
public class AutoValueFluentApiFactory implements Serializable {

    public <T> T create(final Class<T> base, final String factoryMethod, final Map<String, Object> config) {
        try {
            // for now entry methods don't have params
            final Method method = findFactory(base, factoryMethod);
            return base.cast(setConfig(method.invoke(null), config, ""));
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    private <T> Method findFactory(final Class<T> base, final String name) {
        // direct try but unlikely
        return Stream
                .of(base.getMethods())
                .filter(m -> name.equals(m.getName()))
                .min(new FactorySorter())
                .orElseGet(() -> {
                    // next try is enclosing class if exist
                    return ofNullable(base.getEnclosingClass())
                            .flatMap(c -> Stream
                                    .of(c.getMethods())
                                    .filter(m -> name.equals(m.getName()))
                                    .min(new FactorySorter()))
                            .orElseThrow(() -> new IllegalArgumentException("No factory '" + name + "' in " + base));
                });
    }

    private Object setConfig(final Object root, final Map<String, Object> config, final String prefix) {
        Object io = root;
        final Map<String, Method> potentialConfigs =
                Stream
                        .of(io.getClass().getMethods())
                        .filter(m -> m.getName().length() > 5
                                && (m.getName().startsWith("with") || m.getName().startsWith("set"))
                                && m.getParameters().length == 1 && m.getReturnType().isInstance(root))
                        .collect(toMap(
                                m -> decapitalize(
                                        m.getName().startsWith("with") ? m.getName().substring("with".length())
                                                : m.getName().substring("set".length())),
                                identity(), (method1, method2) -> {
                                    if (Class.class.isInstance(method1.getGenericParameterTypes()[0])) {
                                        return method1;
                                    }
                                    if (Class.class.isInstance(method2.getGenericParameterTypes()[0])) {
                                        return method2;
                                    }
                                    if (method1.toGenericString().compareTo(method2.toGenericString()) < 0) {
                                        return method1;
                                    }
                                    return method2;
                                }));

        for (final Map.Entry<String, Method> entry : potentialConfigs.entrySet()) {
            final String configKey = entry.getKey();
            final Method wither = entry.getValue();
            final Type paramType = wither.getParameters()[0].getParameterizedType();

            Object value = createValue(config, prefix, configKey, paramType);
            if (value != null) {
                io = with(io, wither, value);
            }
        }

        return io;
    }

    private Object createValue(final Map<String, Object> config, final String prefix, final String configKey,
            final Type paramType) {
        Object value = config.get(configKey);
        if (value != null) {
            value = createPrimitiveValue(value, paramType);
        } else {
            final String filter = prefix + configKey + '.';
            final Map<String, Object> subObjectConfig = config
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey().startsWith(filter))
                    .collect(toMap(e -> e.getKey().substring(filter.length()), Map.Entry::getValue));
            if (!subObjectConfig.isEmpty()) {
                value = createObjectValue(subObjectConfig, paramType, prefix);
            }
        }
        return value;
    }

    private Object createObjectValue(final Map<String, Object> config, final Type paramType, final String prefix) {
        if (!Class.class.isInstance(paramType)) {
            throw new IllegalArgumentException("Unsupported type: " + paramType);
        }
        final Method factory = findFactory(Class.class.cast(paramType), "create");
        final List<Map.Entry<String, Object>> params = Stream
                .of(factory.getParameters())
                .map(p -> new AbstractMap.SimpleEntry<>(p.getName(),
                        createValue(config, prefix, p.getName(), p.getParameterizedType())))
                .collect(toList());
        try {
            return setConfig(factory.invoke(null, params.stream().map(Map.Entry::getValue).toArray(Object[]::new)),
                    config, prefix);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    private Object with(final Object io, final Method wither, final Object arg) {
        try {
            return wither.invoke(io, arg);
        } catch (final IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalStateException(e.getTargetException());
        }
    }

    private Object createPrimitiveValue(final Object v, final Type type) {
        if (String.class == type) { // fast path
            return v;
        }
        if (Class.class.isInstance(type) && Class.class.cast(type).isInstance(v)) {
            return v;
        }
        if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType pt = ParameterizedType.class.cast(type);
            final Type raw = pt.getRawType();
            // we know what we do if we use that
            if (Class.class.isInstance(raw) && Class.class.cast(raw).isInstance(v)) {
                return v;
            }
        }
        try {
            return PropertyEditors.getValue(type, v.toString());
        } catch (final PropertyEditorException pee) {
            throw new IllegalArgumentException(pee);
        }
    }

    private static class FactorySorter implements Comparator<Method> {

        @Override
        public int compare(final Method o1, final Method o2) {
            final int supportedParams1 = countSupportedParams(o1);
            if (supportedParams1 == o1.getParameterCount()) {
                return -1;
            }
            final int supportedParams2 = countSupportedParams(o2);
            if (supportedParams2 == o2.getParameterCount()) {
                return 1;
            }
            final int diff1 = o1.getParameterCount() - supportedParams1;
            final int diff2 = o2.getParameterCount() - supportedParams2;
            return diff2 - diff1;
        }

        private int countSupportedParams(final Method mtd) { // can be enhanced for nested objects, for now support
                                                             // primitives
            return (int) Stream.of(mtd.getParameters()).filter(p -> PropertyEditors.canConvert(p.getType())).count();
        }
    }
}
