/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.internationalization.ParameterBundle;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ParameterMeta {

    private static final ParameterBundle NO_PARAMETER_BUNDLE = new ParameterBundle(null, null) {

        @Override
        protected Optional<String> readValue(final String key) {
            return empty();
        }

        @Override
        protected Optional<String> readRawValue(final String key) {
            return empty();
        }

        @Override
        public Optional<String> displayName() {
            return empty();
        }

        @Override
        public Optional<String> fallbackDisplayName(final ParameterBundle parameterBundle) {
            return empty();
        }
    };

    private final Source source;

    private final java.lang.reflect.Type javaType;

    private final Type type;

    private final String path;

    private final String name;

    private final String[] i18nPackages; // fallback when the type is not sufficient (java.* types)

    private final List<ParameterMeta> nestedParameters;

    private final Collection<String> proposals;

    private final Map<String, String> metadata;

    private final ConcurrentMap<Locale, ParameterBundle> bundles = new ConcurrentHashMap<>();

    public ParameterBundle findBundle(final ClassLoader loader, final Locale locale) {
        final Class<?> type = of(javaType)
                .filter(Class.class::isInstance)
                .map(Class.class::cast)
                .filter(c -> !c.getName().startsWith("java.") && !c.isPrimitive())
                .orElse(null);
        return bundles.computeIfAbsent(locale, l -> {
            try {
                final ResourceBundle[] bundles =
                        (i18nPackages != null ? Stream.of(i18nPackages) : Stream.<String> empty())
                                .filter(Objects::nonNull)
                                .filter(s -> !s.isEmpty())
                                .distinct()
                                .map(p -> p + (!p.isEmpty() ? "." : "") + "Messages")
                                .map(n -> {
                                    try {
                                        return ResourceBundle.getBundle(n, locale, loader);
                                    } catch (final MissingResourceException mre) {
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .toArray(ResourceBundle[]::new);
                if (bundles.length == 0) {
                    log.warn(noBundleMessage());
                    return NO_PARAMETER_BUNDLE;
                }

                final Collection<String> fallbacks = new ArrayList<>(2);
                final Class<?> declaringClass = source == null ? null : source.declaringClass();
                if (declaringClass != null && !declaringClass.getName().startsWith("java")) {
                    fallbacks.add(declaringClass.getSimpleName() + '.' + source.name());
                }
                if (type != null) {
                    fallbacks.add(type.getSimpleName() + '.' + name);
                }
                return new ParameterBundle(bundles, path + '.', fallbacks.toArray(new String[fallbacks.size()]));
            } catch (final MissingResourceException mre) {
                log.warn(noBundleMessage());
                log.debug(mre.getMessage(), mre);
                return NO_PARAMETER_BUNDLE;
            }
        });
    }

    private String noBundleMessage() {
        return (i18nPackages == null ? "No bundle " : "No bundle in " + asList(i18nPackages)) + " (" + path
                + "), means the display names will be the technical names";
    }

    public interface Source {

        String name();

        Class<?> declaringClass();
    }

    public enum Type {
        OBJECT,
        ARRAY,
        BOOLEAN,
        STRING,
        NUMBER,
        ENUM
    }
}
