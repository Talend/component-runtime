/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.runtime.manager;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.talend.component.api.component.MigrationHandler;
import org.talend.components.runtime.input.Mapper;
import org.talend.components.runtime.internationalization.ComponentBundle;
import org.talend.components.runtime.output.Processor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ComponentFamilyMeta {

    private final String plugin;

    private final Collection<String> categories;

    private final String name;

    private final Map<String, PartitionMapperMeta> partitionMappers = new HashMap<>();

    private final Map<String, ProcessorMeta> processors = new HashMap<>();

    @Data
    public static class BaseMeta<T> {

        private static final ComponentBundle NO_COMPONENT_BUNDLE = new ComponentBundle(null, null) {

            @Override
            public Optional<String> displayName() {
                return empty();
            }
        };

        private final ComponentFamilyMeta parent;

        private final String name;

        private final String icon;

        private final int version;

        private final String packageName;

        private final MigrationHandler migrationHandler;

        private final List<ParameterMeta> parameterMetas;

        private final ConcurrentMap<Locale, ComponentBundle> bundles = new ConcurrentHashMap<>();

        private final String id;

        private final Class<?> type;

        private final Function<Map<String, String>, T> instantiator;

        private final boolean validated;

        BaseMeta(final ComponentFamilyMeta parent, final String name, final String icon, final int version, final Class<?> type,
                final List<ParameterMeta> parameterMetas, final MigrationHandler migrationHandler,
                final Function<Map<String, String>, T> instantiator, final boolean validated) {
            this.parent = parent;
            this.name = name;
            this.icon = icon;
            this.version = version;
            this.packageName = ofNullable(type.getPackage()).map(Package::getName).orElse("");
            this.parameterMetas = parameterMetas;
            this.migrationHandler = migrationHandler;
            this.type = type;
            this.instantiator = instantiator;
            this.validated = validated;

            // keep this algorithm private for now and don't assume it is reversible, we can revise it to something more
            // compressed later
            this.id = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString((parent.getPlugin() + "#" + parent.getName() + "#" + name).getBytes(StandardCharsets.UTF_8));
        }

        public ComponentBundle findBundle(final ClassLoader loader, final Locale locale) {
            return bundles.computeIfAbsent(locale, l -> {
                try {
                    final ResourceBundle bundle = ResourceBundle
                            .getBundle((packageName.isEmpty() ? packageName : (packageName + '.')) + "Messages", locale, loader);
                    return new ComponentBundle(bundle, parent.name + '.' + name + '.');
                } catch (final MissingResourceException mre) {
                    log.warn("No bundle for " + packageName + " (" + parent.name + " / " + name
                            + "), means the display names will be the technical names");
                    log.debug(mre.getMessage(), mre);
                    return NO_COMPONENT_BUNDLE;
                }
            });
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class PartitionMapperMeta extends BaseMeta<Mapper> {

        PartitionMapperMeta(final ComponentFamilyMeta parent, final String name, final String icon, final int version,
                final Class<?> type, final List<ParameterMeta> parameterMetas,
                final Function<Map<String, String>, Mapper> instantiator, final MigrationHandler migrationHandler,
                final boolean validated) {
            super(parent, name, icon, version, type, parameterMetas, migrationHandler, instantiator, validated);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ProcessorMeta extends BaseMeta<Processor> {

        ProcessorMeta(final ComponentFamilyMeta parent, final String name, final String icon, final int version,
                final Class<?> type, final List<ParameterMeta> parameterMetas,
                final Function<Map<String, String>, Processor> instantiator, final MigrationHandler migrationHandler,
                final boolean validated) {
            super(parent, name, icon, version, type, parameterMetas, migrationHandler, instantiator, validated);
        }
    }
}
