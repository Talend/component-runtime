/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.talend.sdk.component.runtime.internationalization.ParameterBundle;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Data
@Slf4j
public class ParameterMeta {

    private static final ParameterBundle NO_PARAMETER_BUNDLE = new ParameterBundle(null, null, null) {

        @Override
        public Optional<String> displayName() {
            return empty();
        }

        @Override
        public Optional<String> displayName(final String child) {
            return empty();
        }
    };

    private final java.lang.reflect.Type javaType;

    private final Type type;

    private final String path;

    private final String name;

    private final String i18nPackage; // fallback when the type is not sufficient (java.* types)

    private final List<ParameterMeta> nestedParameters;

    private final Collection<String> proposals;

    private final Map<String, String> metadata;

    private final ConcurrentMap<Locale, ParameterBundle> bundles = new ConcurrentHashMap<>();

    public ParameterBundle findBundle(final ClassLoader loader, final Locale locale) {
        final Class<?> type = of(javaType)
                .filter(Class.class::isInstance)
                .map(Class.class::cast)
                .filter(c -> !c.getName().startsWith("java."))
                .orElse(null);
        final String packageName = ofNullable(type).map(Class::getPackage).map(Package::getName).orElse(i18nPackage);
        return bundles.computeIfAbsent(locale, l -> {
            try {
                final String baseName = (packageName.isEmpty() ? i18nPackage : (packageName + '.')) + "Messages";
                log.debug("Using resource bundle " + baseName + " for " + ParameterMeta.this);
                final ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, loader);
                return new ParameterBundle(bundle, path + '.', type == null ? null : type.getSimpleName());
            } catch (final MissingResourceException mre) {
                log.warn("No bundle for " + packageName + " (" + ParameterMeta.this
                        + "), means the display names will be the technical names");
                log.debug(mre.getMessage(), mre);
                return NO_PARAMETER_BUNDLE;
            }
        });
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
