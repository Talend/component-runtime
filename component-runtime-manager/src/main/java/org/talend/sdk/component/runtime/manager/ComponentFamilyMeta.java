/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
import static java.util.stream.Collectors.toSet;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
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
import java.util.stream.Stream;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.internationalization.ComponentBundle;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ComponentFamilyMeta {

    private final String plugin;

    private final Collection<String> categories;

    private final String icon;

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
        
        /**
         * Stores data provided by extensions like ContainerListenerExtension 
         */
        @Getter(AccessLevel.NONE)
        private final ConcurrentMap<Class<?>, Object> extensionsData = new ConcurrentHashMap<>();

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
        
        /**
         * Sets data provided by extension
         * 
         * @param key {@link Class} of data provided
         * @param instance data instance
         * @return data instance
         */
        public <D> D set(final Class<D> key, final D instance) {
            return (D) extensionsData.put(key, instance);
        }

        /**
         * Returns extension data instance
         * 
         * @param key {@link Class} of data instance to return
         * @return data instance
         */
        public <D> D get(final Class<D> key) {
            return (D) extensionsData.get(key);
        }
        
        /**
         * Returns a {@link Collection} of input flows names of this Component
         * Should be overridden in inheritors
         * 
         * @return input flows names collection
         */
        public Collection<String> getInputFlows() {
            return Collections.emptySet();
        }
        
        /**
         * Returns a {@link Collection} of output flows names of this Component
         * Should be overridden in inheritors
         * 
         * @return output flows names collection
         */
        public Collection<String> getOutputFlows() {
            return Collections.emptySet();
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

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<String> getOutputFlows() {
            return Collections.singleton(Branches.DEFAULT_BRANCH);
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

        /**
         * Returns {@link Processor} class method annotated with {@link ElementListener}
         * 
         * @return listener method
         */
        public Method getListener() {
            return Stream.of(getType().getMethods()).filter(m -> m.isAnnotationPresent(ElementListener.class)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No @ElementListener method in " + getType()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<String> getInputFlows() {
            return getInputParameters()
                    .map(p -> ofNullable(p.getAnnotation(Input.class)).map(Input::value).orElse(Branches.DEFAULT_BRANCH))
                    .collect(toSet());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<String> getOutputFlows() {
            Method listener = getListener();
            return Stream.concat(listener.getReturnType().equals(Void.TYPE) ? Stream.empty() : Stream.of(Branches.DEFAULT_BRANCH),
                    Stream.of(listener.getParameters()).filter(p -> p.isAnnotationPresent(Output.class))
                            .map(p -> p.getAnnotation(Output.class).value()))
                    .collect(toSet());
        }

        /**
         * Returns all {@link ElementListener} method parameters, which are not annotated with {@link Output}
         * 
         * @return listener method input parameters
         */
        private Stream<Parameter> getInputParameters() {
            return Stream.of(getListener().getParameters())
                    .filter(p -> p.isAnnotationPresent(Input.class) || !p.isAnnotationPresent(Output.class));
        }
    }
}
