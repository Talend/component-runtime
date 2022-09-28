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
package org.talend.sdk.component.runtime.manager.reflect.parameterenricher;

import static java.util.Collections.emptyMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.api.configuration.type.meta.ConfigurationType;

public class ConfigurationTypeParameterEnricher extends BaseParameterEnricher {

    public static final String META_PREFIX = "tcomp::configurationtype::";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName, final Type parameterType,
            final Annotation annotation) {
        final ConfigurationType configType = annotation.annotationType().getAnnotation(ConfigurationType.class);
        if (configType != null) {
            final String type = configType.value();
            final String name = getName(annotation);
            if (name != null) {
                return new HashMap<String, String>() {

                    {
                        put(META_PREFIX + "type", type);
                        put(META_PREFIX + "name", name);
                    }
                };
            }
        }
        return emptyMap();
    }

    private String getName(final Annotation annotation) {
        try {
            return String.class.cast(annotation.annotationType().getMethod("value").invoke(annotation));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }
}
