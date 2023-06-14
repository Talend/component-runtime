/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.talend.sdk.component.api.configuration.dependency.ConnectorRef;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DependencyParameterEnricher extends BaseParameterEnricher {

    public static final String TCOMP_DEPENDENCIES_CONNECTOR_KEY = "tcomp::dependencies::connector";

    @Override
    public Map<String, String> onParameterAnnotation(final String parameterName,
            final Type parameterType,
            final Annotation annotation) {

        // mark class type as the one that carry dependencies
        if (this.isConnectorReference(parameterType)
                || this.isCollectionConnectorReference(parameterType)) {
            return Collections.singletonMap(TCOMP_DEPENDENCIES_CONNECTOR_KEY, "family");
        }

        // mark relevant fields with metadata
        if (String.class.equals(parameterType) && annotation != null
                && ConnectorRef.class.equals(annotation.annotationType())) {
            return Collections.singletonMap(TCOMP_DEPENDENCIES_CONNECTOR_KEY,
                    ((ConnectorRef) annotation).value().getRefValue());
        }

        return Collections.emptyMap();
    }

    // Check if parameter is of a simple collection (List, Set ...) of ConnectorReference
    // don't work with Map (composed collection), nor List<? extends ConnectorReference> (not used in framework)
    private boolean isCollectionConnectorReference(final Type parameterType) {
        // check generic
        if (!(parameterType instanceof ParameterizedType)) {
            return false;
        }
        final ParameterizedType parameterClass = (ParameterizedType) parameterType;

        // Check it's a Collection (java.util)
        final Type rawType = parameterClass.getRawType();
        if ((!(rawType instanceof Class)) || !Collection.class.isAssignableFrom((Class) rawType)) {
            return false;
        }

        // Check arguments.
        final Type[] arguments = parameterClass.getActualTypeArguments();
        if (arguments.length != 1) {
            return false;
        }
        final Type argument = arguments[0];
        return this.isConnectorReference(argument);
    }

    // Check if parameter is of type or subtype of ConnectorReference
    private boolean isConnectorReference(final Type parameterType) {
        return parameterType instanceof Class
                && Stream.of(((Class<?>) parameterType).getDeclaredFields())
                        .anyMatch(field -> field.isAnnotationPresent(ConnectorRef.class));
    }
}
