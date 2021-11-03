/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.lang.reflect.Method;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.internationalization.ComponentBundle;
import org.talend.sdk.component.runtime.internationalization.FamilyBundle;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.standalone.DriverRunner;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ComponentFamilyMeta {

    private static final FamilyBundle NO_COMPONENT_BUNDLE = new FamilyBundle(null, null) {

        @Override
        public Optional<String> actionDisplayName(final String type, final String name) {
            return empty();
        }

        @Override
        public Optional<String> category(final String value) {
            return empty();
        }

        @Override
        public Optional<String> configurationDisplayName(final String type, final String name) {
            return empty();
        }

        @Override
        public Optional<String> displayName() {
            return empty();
        }
    };

    private final String id;

    private final String plugin;

    private final Collection<String> categories;

    private final String icon;

    private final String name;

    private final String packageName;

    private final Map<String, PartitionMapperMeta> partitionMappers = new HashMap<>();

    private final Map<String, ProcessorMeta> processors = new HashMap<>();

    private final Map<String, DriverRunnerMeta> driverRunners = new HashMap<>();

    private final ConcurrentMap<Locale, FamilyBundle> bundles = new ConcurrentHashMap<>();

    public ComponentFamilyMeta(final String plugin, final Collection<String> categories, final String icon,
            final String name, final String packageName) {
        this.id = IdGenerator.get(plugin, name);
        this.plugin = plugin;
        this.categories = categories;
        this.icon = icon;
        this.name = name;
        this.packageName = packageName;
    }

    public FamilyBundle findBundle(final ClassLoader loader, final Locale locale) {
        return bundles.computeIfAbsent(locale, l -> {
            try {
                final ResourceBundle bundle = ResourceBundle
                        .getBundle((packageName.isEmpty() ? packageName : (packageName + '.')) + "Messages", locale,
                                loader);
                return new FamilyBundle(bundle, name + '.');
            } catch (final MissingResourceException mre) {
                log
                        .warn("No bundle for " + packageName + " (" + name
                                + "), means the display names will be the technical names");
                log.debug(mre.getMessage(), mre);
                return NO_COMPONENT_BUNDLE;
            }
        });
    }

    @Data
    public static class BaseMeta<T extends Lifecycle> {

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

        private final Supplier<MigrationHandler> migrationHandler;

        private final Supplier<List<ParameterMeta>> parameterMetas;

        private final ConcurrentMap<Locale, ComponentBundle> bundles = new ConcurrentHashMap<>();

        /**
         * Stores data provided by extensions like ContainerListenerExtension
         */
        @Getter(AccessLevel.NONE)
        private final ConcurrentMap<Class<?>, Object> extensionsData = new ConcurrentHashMap<>();

        private final String id;

        private final Class<?> type;

        private final Function<Map<String, String>, T> instantiator;

        private final boolean validated;

        private final Map<String, String> metadata;

        BaseMeta(final ComponentFamilyMeta parent, final String name, final String icon, final int version,
                final Class<?> type, final Supplier<List<ParameterMeta>> parameterMetas,
                final Supplier<MigrationHandler> migrationHandler, final Function<Map<String, String>, T> instantiator,
                final boolean validated, final Map<String, String> metas) {
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
            this.metadata = metas;

            this.id = IdGenerator.get(parent.getPlugin(), parent.getName(), name);

        }

        public ComponentBundle findBundle(final ClassLoader loader, final Locale locale) {
            return bundles.computeIfAbsent(locale, l -> {
                try {
                    final ResourceBundle bundle = ResourceBundle
                            .getBundle((packageName.isEmpty() ? packageName : (packageName + '.')) + "Messages", locale,
                                    loader);
                    return new ComponentBundle(bundle, parent.name + '.' + name + '.');
                } catch (final MissingResourceException mre) {
                    log
                            .warn("No bundle for " + packageName + " (" + parent.name + " / " + name
                                    + "), means the display names will be the technical names");
                    log.debug(mre.getMessage(), mre);
                    return NO_COMPONENT_BUNDLE;
                }
            });
        }

        public T instantiate(final Map<String, String> configuration, final int configVersion) {
            if (configuration == null) {
                return this.getInstantiator().apply(null);
            }
            final Supplier<MigrationHandler> migrationHandler = this.getMigrationHandler();
            final Map<String, String> migratedConfiguration =
                    migrationHandler.get().migrate(configVersion, configuration);
            return this.getInstantiator().apply(migratedConfiguration);
        }

        /**
         * Sets data provided by extension
         *
         * @param key {@link Class} of data provided
         * @param instance data instance
         * @param <D> the type of the instance to store.
         *
         * @return data instance
         */
        public <D> D set(final Class<D> key, final D instance) {
            return (D) extensionsData.put(key, instance);
        }

        /**
         * Returns extension data instance
         *
         * @param key {@link Class} of data instance to return
         * @param <D> the type of the instance to store.
         *
         * @return data instance
         */
        public <D> D get(final Class<D> key) {
            return (D) extensionsData.get(key);
        }
    }

    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class PartitionMapperMeta extends BaseMeta<Mapper> {

        public static final String MAPPER_INFINITE = "mapper::infinite";

        protected PartitionMapperMeta(final ComponentFamilyMeta parent, final String name, final String icon,
                final int version, final Class<?> type, final Supplier<List<ParameterMeta>> parameterMetas,
                final Function<Map<String, String>, Mapper> instantiator,
                final Supplier<MigrationHandler> migrationHandler, final boolean validated,
                final Map<String, String> metas) {
            super(parent, name, icon, version, type, parameterMetas, migrationHandler, instantiator, validated, metas);
        }

        protected PartitionMapperMeta(final ComponentFamilyMeta parent, final String name, final String icon,
                final int version, final Class<?> type, final Supplier<List<ParameterMeta>> parameterMetas,
                final Function<Map<String, String>, Mapper> instantiator,
                final Supplier<MigrationHandler> migrationHandler, final boolean validated, final boolean infinite) {
            super(parent, name, icon, version, type, parameterMetas, migrationHandler, instantiator, validated,
                    new HashMap<String, String>() {

                        {
                            put(MAPPER_INFINITE, Boolean.toString(infinite));
                        }
                    });
        }

        public boolean isInfinite() {
            return Boolean.parseBoolean(getMetadata().getOrDefault(MAPPER_INFINITE, "false"));
        }
    }

    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class ProcessorMeta extends BaseMeta<Processor> {

        protected ProcessorMeta(final ComponentFamilyMeta parent, final String name, final String icon,
                final int version, final Class<?> type, final Supplier<List<ParameterMeta>> parameterMetas,
                final Function<Map<String, String>, Processor> instantiator,
                final Supplier<MigrationHandler> migrationHandler, final boolean validated,
                final Map<String, String> metas) {
            super(parent, name, icon, version, type, parameterMetas, migrationHandler, instantiator, validated, metas);
        }

        /**
         * Returns {@link Processor} class method annotated with {@link ElementListener}
         *
         * @return listener method
         */
        public Method getListener() {
            return Stream
                    .of(getType().getMethods())
                    .filter(m -> m.isAnnotationPresent(ElementListener.class))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No @ElementListener method in " + getType()));
        }
    }

    @ToString
    @EqualsAndHashCode(callSuper = true)
    public static class DriverRunnerMeta extends BaseMeta<DriverRunner> {

        protected DriverRunnerMeta(final ComponentFamilyMeta parent, final String name, final String icon,
                final int version, final Class<?> type, final Supplier<List<ParameterMeta>> parameterMetas,
                final Function<Map<String, String>, DriverRunner> instantiator,
                final Supplier<MigrationHandler> migrationHandler, final boolean validated,
                final Map<String, String> metas) {
            super(parent, name, icon, version, type, parameterMetas, migrationHandler, instantiator, validated, metas);
        }
    }
}
