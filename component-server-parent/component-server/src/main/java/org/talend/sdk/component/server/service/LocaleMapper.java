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
package org.talend.sdk.component.server.service;

import static java.util.Locale.ENGLISH;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;

@ApplicationScoped
public class LocaleMapper {

    @Inject
    private ComponentServerConfiguration configuration;

    private Map<Predicate<String>, String> mapping;

    @PostConstruct
    private void init() {
        final Properties properties = new Properties();
        try (final StringReader reader = new StringReader(configuration.getLocaleMapping())) {
            properties.load(reader);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
        mapping = properties.stringPropertyNames().stream().collect(toMap(it -> {
            if (it.endsWith("*")) {
                final String prefix = it.substring(0, it.length() - 1);
                return (Predicate<String>) s -> s.startsWith(prefix);
            }
            return (Predicate<String>) s -> s.equals(it);
        }, properties::getProperty));
    }

    // intended to limit and normalize the locales to avoid a tons when used with caching
    public Locale mapLocale(final String requested) {
        return new Locale(getLanguage(requested).toLowerCase(ENGLISH));
    }

    private String getLanguage(final String requested) {
        if (requested == null) {
            return "en";
        }
        return mapping
                .entrySet()
                .stream()
                .filter(it -> it.getKey().test(requested))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse("en");
    }
}
