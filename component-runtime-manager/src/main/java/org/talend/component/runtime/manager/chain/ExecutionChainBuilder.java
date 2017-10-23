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
package org.talend.component.runtime.manager.chain;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.talend.component.runtime.manager.ComponentManager;
import org.talend.component.runtime.manager.ComponentFamilyMeta;
import org.talend.component.runtime.manager.ContainerComponentRegistry;
import org.talend.component.runtime.input.Mapper;
import org.talend.component.runtime.output.Branches;
import org.talend.component.runtime.output.Processor;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

// low typing for easy configuration (make it closer to configuration than code),
// intended to be used in generated code but not in code first approaches
@NoArgsConstructor(access = PRIVATE)
public class ExecutionChainBuilder {

    public static Head start() {
        return new Head();
    }

    private static Map<String, String> override(final String prefix, final Map<String, String> input) {
        return ofNullable(input).orElseGet(HashMap::new).entrySet().stream().collect(
                toMap(Map.Entry::getKey, e -> ofNullable(System.getProperty(prefix + "." + e.getKey())).orElseGet(e::getValue)));
    }

    @Getter
    @NoArgsConstructor(access = PRIVATE)
    public static class Head {

        private String name; // not yet used but can be needed so put a place where to store global meta

        private boolean supportsSystemPropertiesOverrides;

        private InputConfigurer inputConfigurer;

        public Head withConfiguration(final String name, final boolean supportsSystemPropertiesOverrides) {
            this.name = name;
            this.supportsSystemPropertiesOverrides = supportsSystemPropertiesOverrides;
            return this;
        }

        public InputConfigurer fromInput(final String plugin, final String name, final int version,
                final Map<String, String> configuration) {
            return inputConfigurer = new InputConfigurer(this, plugin, name, version, configuration);
        }
    }

    @Getter
    @RequiredArgsConstructor(access = PRIVATE)
    public static class ProcessorDefiner<T extends ProcessorDefiner<?>> {

        @Getter(NONE)
        private final T parent;

        private final String plugin;

        private final String name;

        private final int version;

        private final Map<String, String> configuration;

        private final Collection<ProcessorConfigurer<?>> children = new ArrayList<>();

        protected ProcessorConfigurer<T> linkProcessor(final String outputMarker, final String plugin, final String name,
                final int version, final Map<String, String> configuration) {
            final ProcessorConfigurer configurer = new ProcessorConfigurer<>(this, outputMarker, plugin, name, version,
                    configuration);
            children.add(configurer);
            return configurer;
        }

        public T getParent() {
            return parent;
        }
    }

    @Getter
    public static class InputConfigurer extends ProcessorDefiner<InputConfigurer> {

        private final Head head;

        private Mapper mapper;

        private InputConfigurer(final Head parent, final String plugin, final String name, final int version,
                final Map<String, String> configuration) {
            super(null, plugin, name, version, configuration);
            this.head = parent;
        }

        public ProcessorConfigurer<InputConfigurer> toProcessor(final String plugin, final String name, final int version,
                final Map<String, String> configuration) {
            return linkProcessor(null, plugin, name, version, configuration);
        }
    }

    @Getter
    public static class ProcessorConfigurer<T extends ProcessorDefiner<?>> extends ProcessorDefiner<T> {

        private final String marker;

        private Processor processor;

        private ProcessorConfigurer(final T parent, final String marker, final String plugin, final String name,
                final int version, final Map<String, String> configuration) {
            super(parent, plugin, name, version, configuration);
            this.marker = ofNullable(marker).orElse(Branches.DEFAULT_BRANCH);
        }

        public ProcessorConfigurer<ProcessorConfigurer<?>> toProcessor(final String outputMarker, final String plugin,
                final String name, final int version, final Map<String, String> configuration) {
            return (ProcessorConfigurer<ProcessorConfigurer<?>>) linkProcessor(outputMarker, plugin, name, version,
                    configuration);
        }

        public Supplier<ExecutionChain> create(final ComponentManager manager, final Function<String, File> pluginFinder,
                final ExecutionChain.SuccessListener successListener, final ExecutionChain.ErrorHandler errorHandler) {
            return () -> {
                // list required plugins
                final Set<String> requiredPlugins = new HashSet<>();
                final ProcessorDefiner<?> root;
                final boolean overrideConfig;
                {
                    ProcessorDefiner<?> current = getParent();
                    while (current.getParent() != null) {
                        current = current.getParent();
                    }
                    requiredPlugins.add(current.getPlugin());
                    capturePlugins(requiredPlugins, current.children);

                    // remove already loaded plugins
                    requiredPlugins.removeIf(manager::hasPlugin);

                    root = current;
                    overrideConfig = InputConfigurer.class.cast(root).head.supportsSystemPropertiesOverrides;
                }

                // start by loading required plugins if not already done (embedded + local cases)
                // ensure eager loading for embedded case - fail fast
                try (final InputStream stream = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("TALEND-INF/plugins.properties")) {
                    if (stream != null) {
                        final Set<String> embeddedPlugins = new Properties() {

                            {
                                load(stream);
                            }
                        }.stringPropertyNames().stream().filter(requiredPlugins::contains).collect(toSet());
                        embeddedPlugins.forEach(manager::addPlugin);
                        requiredPlugins.removeIf(embeddedPlugins::contains);
                    }
                } catch (final IOException e) {
                    // no-op: let's try the file system listing
                }

                // now load missing "embedded" plugins, try locally
                requiredPlugins.stream().filter(p -> manager.find(c -> c.get(ContainerComponentRegistry.class).getComponents()
                        .values().stream().map(ComponentFamilyMeta::getName)).noneMatch(b -> b.equals(p))).forEach(plugin -> {
                            final File pluginFile = pluginFinder.apply(plugin);
                            if (pluginFile == null || !pluginFile.exists()) {
                                throw new IllegalArgumentException(plugin);
                            }
                            manager.addPlugin(plugin);
                        });

                // now start build our chain by the input
                InputConfigurer.class.cast(root).mapper = manager
                        .findMapper(root.getPlugin(), root.getName(), root.getVersion(),
                                overrideConfig ? override(root.getPlugin() + "." + root.getName(), root.getConfiguration())
                                        : root.getConfiguration())
                        .orElseThrow(() -> new IllegalStateException(
                                "Can't find the required input: " + root.getPlugin() + "#" + root.getName()));

                initProcessors(overrideConfig, root.getChildren(), manager);

                return new ExecutionChain(InputConfigurer.class.cast(root).head, errorHandler, successListener);
            };
        }

        private void initProcessors(final boolean overrideConfig, final Collection<ProcessorConfigurer<?>> children,
                final ComponentManager manager) {
            children.forEach(c -> {
                c.processor = manager
                        .findProcessor(c.getPlugin(), c.getName(), c.getVersion(),
                                overrideConfig ? override(c.getPlugin() + "." + c.getName(), c.getConfiguration())
                                        : c.getConfiguration())
                        .orElseThrow(() -> new IllegalStateException(
                                "Can't find the required processor: " + c.getPlugin() + "#" + c.getName()));
                initProcessors(overrideConfig, c.getChildren(), manager);
            });
        }

        private void capturePlugins(final Set<String> requiredPlugins, final Collection<ProcessorConfigurer<?>> children) {
            children.forEach(c -> {
                requiredPlugins.add(c.getPlugin());
                capturePlugins(requiredPlugins, c.getChildren());
            });
        }
    }
}
