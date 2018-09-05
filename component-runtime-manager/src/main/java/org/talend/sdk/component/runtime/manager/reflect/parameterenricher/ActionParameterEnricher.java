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
import static java.util.Optional.ofNullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.action.meta.ActionRef;
import org.talend.sdk.component.api.service.ActionType;

public class ActionParameterEnricher extends BaseParameterEnricher {

    public static final String META_PREFIX = "tcomp::action::";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final ActionRef ref = annotation.annotationType().getAnnotation(ActionRef.class);
        if (ref == null) {
            return emptyMap();
        }
        final String type = ref.value().getAnnotation(ActionType.class).value();
        return new HashMap<String, String>() {

            {
                put(META_PREFIX + type, getValueString(ref.ref(), annotation));
                ofNullable(getParametersString(annotation)).ifPresent(v -> put(META_PREFIX + type + "::parameters", v));
            }
        };
    }

    private String getValueString(final String method, final Annotation annotation) {
        try {
            return String.valueOf(annotation.annotationType().getMethod(method).invoke(annotation));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException("No value for " + annotation);
        }
    }

    private String getParametersString(final Annotation annotation) {
        try {
            return Stream
                    .of(String[].class.cast(annotation.annotationType().getMethod("parameters").invoke(annotation)))
                    .collect(Collectors.joining(","));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
