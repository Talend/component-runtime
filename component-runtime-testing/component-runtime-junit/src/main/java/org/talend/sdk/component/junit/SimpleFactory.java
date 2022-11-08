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
package org.talend.sdk.component.junit;

import static java.lang.Character.toUpperCase;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.configuration.ConfigurationMapper;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.xbean.registry.EnrichedPropertyEditorRegistry;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class SimpleFactory {

    public static ByExample configurationByExample() {
        return new ByExample();
    }

    public static <T> Map<String, String> configurationByExample(final T instance) {
        return configurationByExample().forInstance(instance).configured().toMap();
    }

    public static <T> Map<String, String> configurationByExample(final T instance, final String prefix) {
        return configurationByExample().forInstance(instance).withPrefix(prefix).configured().toMap();
    }

    private static class SimpleParameterModelService extends ParameterModelService {

        public SimpleParameterModelService() {
            super(new EnrichedPropertyEditorRegistry());
        }

        private ParameterMeta build(final String name, final String prefix, final Type genericType,
                final Annotation[] annotations, final Collection<String> i18nPackages) {
            return super.buildParameter(name, prefix, null, genericType, annotations, i18nPackages, false,
                    new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
        }
    }

    @NoArgsConstructor(access = PRIVATE)
    public static class ByExample {

        private String prefix;

        private Object instance;

        public ByExample withPrefix(final String prefix) {
            this.prefix = prefix;
            return this;
        }

        public <T> ByExample forInstance(final T instance) {
            this.instance = instance;
            return this;
        }

        public ConfigurationByExample configured() {
            return new ConfigurationByExample(this);
        }
    }

    @AllArgsConstructor(access = PRIVATE)
    public static class ConfigurationByExample {

        private static final ConfigurationMapper CONFIGURATION_MAPPER = new ConfigurationMapper();

        private final ByExample byExample;

        public Map<String, String> toMap() {
            if (byExample.instance == null) {
                return emptyMap();
            }
            final String usedPrefix = ofNullable(byExample.prefix).orElse("configuration.");
            final ParameterMeta params = new SimpleParameterModelService()
                    .build(usedPrefix, usedPrefix, byExample.instance.getClass(), new Annotation[0],
                            new ArrayList<>(singletonList(byExample.instance.getClass().getPackage().getName())));
            return CONFIGURATION_MAPPER.map(params.getNestedParameters(), byExample.instance);
        }

        public String toQueryString() {
            return toMap()
                    .entrySet()
                    .stream()
                    .sorted(comparing(Map.Entry::getKey))
                    .map(entry -> entry.getKey() + "=" + encode(entry.getValue()))
                    .collect(Collectors.joining("&"));
        }

        private static String encode(final String source) {
            final byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length);
            boolean changed = false;
            for (final byte rawByte : bytes) {
                byte b = rawByte;
                if (b < 0) {
                    b += 256;
                }
                if ((b >= 'a' && b <= 'z' || b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9') || '-' == b || '.' == b
                        || '_' == b || '~' == b) {
                    bos.write(b);
                } else {
                    bos.write('%');
                    char hex1 = toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
                    char hex2 = toUpperCase(Character.forDigit(b & 0xF, 16));
                    bos.write(hex1);
                    bos.write(hex2);
                    changed = true;
                }
            }
            return changed ? new String(bos.toByteArray(), StandardCharsets.UTF_8) : source;
        }
    }
}
