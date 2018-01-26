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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.condition.ActiveIfs;
import org.talend.sdk.component.api.configuration.condition.meta.Condition;
import org.talend.sdk.component.spi.parameter.ParameterExtensionEnricher;

public class ConditionParameterEnricher implements ParameterExtensionEnricher {

    private static final String META_PREFIX = "tcomp::condition::";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final Condition condition = annotation.annotationType().getAnnotation(Condition.class);
        if (condition != null) {
            final String type = condition.value();
            if (ActiveIfs.class.isInstance(annotation)) {
                return Stream
                        .of(ActiveIfs.class.cast(annotation).value())
                        .map(ai -> onParameterAnnotation(parameterName, parameterType, ai))
                        .collect(HashMap::new, (map, entry) -> {
                            final String suffix = "::" + (map.size() / 2);
                            map.putAll(entry.entrySet().stream().collect(
                                    toMap(e -> e.getKey() + suffix, Map.Entry::getValue)));
                        }, HashMap::putAll);
            }
            return Stream
                    .of(annotation.annotationType().getMethods())
                    .filter(m -> m.getDeclaringClass() == annotation.annotationType())
                    .collect(toMap(m -> META_PREFIX + type + "::" + m.getName(), m -> {
                        try {
                            final Object invoke = m.invoke(annotation);
                            if (String[].class.isInstance(invoke)) {
                                return Stream.of(String[].class.cast(invoke)).collect(joining(","));
                            }
                            return String.valueOf(invoke);
                        } catch (final InvocationTargetException | IllegalAccessException e) {
                            throw new IllegalStateException(e);
                        }
                    }));

        }
        return emptyMap();
    }
}
