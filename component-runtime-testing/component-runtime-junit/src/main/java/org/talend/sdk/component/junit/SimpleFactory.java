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
package org.talend.sdk.component.junit;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.configuration.ConfigurationMapper;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class SimpleFactory {

    private static final ConfigurationMapper CONFIGURATION_MAPPER = new ConfigurationMapper();

    public static <T> Map<String, String> configurationByExample(final T instance) {
        return configurationByExample(instance, null);
    }

    public static <T> Map<String, String> configurationByExample(final T instance, final String prefix) {
        if (instance == null) {
            return emptyMap();
        }
        final String usedPrefix = ofNullable(prefix).orElse("configuration.");
        final ParameterMeta params =
                new SimpleParameterModelService().build(usedPrefix, usedPrefix, instance.getClass(), new Annotation[0],
                        new ArrayList<>(singletonList(instance.getClass().getPackage().getName())));
        return CONFIGURATION_MAPPER.map(params.getNestedParameters(), instance);
    }

    private static class SimpleParameterModelService extends ParameterModelService {

        private ParameterMeta build(final String name, final String prefix, final Type genericType,
                final Annotation[] annotations, final Collection<String> i18nPackages) {
            return super.buildParameter(name, prefix, null, genericType, annotations, i18nPackages);
        }
    }
}
