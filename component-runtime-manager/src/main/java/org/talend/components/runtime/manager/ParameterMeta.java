// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.manager;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.talend.components.runtime.internationalization.ParameterBundle;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ParameterMeta {

    private static final ParameterBundle NO_PARAMETER_BUNDLE = new ParameterBundle(null, null) {

        @Override
        public Optional<String> displayName() {
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
        final Class<?> type = of(javaType).filter(Class.class::isInstance).map(Class.class::cast)
                .filter(c -> !c.getName().startsWith("java.")).orElse(null);
        final String packageName = ofNullable(type).map(Class::getPackage).map(Package::getName).orElse(i18nPackage);
        return bundles.computeIfAbsent(locale, l -> {
            try {
                final String baseName = (packageName.isEmpty() ? i18nPackage : (packageName + '.')) + "Messages";
                log.debug("Using resource bundle " + baseName + " for " + ParameterMeta.this);
                final ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, loader);
                return new ParameterBundle(bundle, path + '.');
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
