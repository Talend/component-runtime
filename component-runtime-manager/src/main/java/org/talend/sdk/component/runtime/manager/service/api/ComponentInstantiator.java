/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service.api;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager.ComponentType;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@FunctionalInterface
public interface ComponentInstantiator {

    Lifecycle instantiate(final Map<String, String> configuration, final int configVersion);

    interface MetaFinder {

        Optional<? extends ComponentFamilyMeta.BaseMeta>
                filter(final Map<String, ? extends ComponentFamilyMeta.BaseMeta> source);

        static MetaFinder ofComponent(final String name) {
            return new ComponentNameFinder(name);
        }
    }

    @FunctionalInterface
    interface Builder {

        ComponentInstantiator build(final String familyName, final MetaFinder finder,
                final ComponentType componentType);
    }

    @Slf4j
    @RequiredArgsConstructor
    class BuilderDefault implements Builder {

        private final Supplier<Stream<ContainerComponentRegistry>> registrySupplier;

        @Override
        public ComponentInstantiator build(final String familyName, final MetaFinder finder,
                final ComponentType componentType) {
            final Stream<ContainerComponentRegistry> registries = this.registrySupplier.get();
            if (registries == null) {
                return null;
            }
            final String sanitizedFamilyName = Optional.ofNullable(familyName).map(String::trim).orElse("");
            return registries
                    .map((ContainerComponentRegistry registry) -> registry.findComponentFamily(sanitizedFamilyName))
                    .filter(Objects::nonNull)
                    .peek((ComponentFamilyMeta cm) -> log.debug("Family found {} plugin {}", sanitizedFamilyName,
                            cm.getPlugin()))
                    .map(componentType::findMeta)
                    .map((Map<String, ? extends ComponentFamilyMeta.BaseMeta> map) -> finder.filter(map))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .map((ComponentFamilyMeta.BaseMeta c) -> (ComponentInstantiator) c::instantiate)
                    .orElse(null);
        }
    }

    @Slf4j
    class ComponentNameFinder implements MetaFinder {

        private final String componentName;

        public ComponentNameFinder(final String componentName) {
            this.componentName = Optional.ofNullable(componentName).map(String::trim).orElse("");
        }

        @Override
        public Optional<? extends ComponentFamilyMeta.BaseMeta>
                filter(final Map<String, ? extends ComponentFamilyMeta.BaseMeta> source) {
            if (!source.containsKey(this.componentName)) {
                log.debug("Can't find component name {}", this.componentName);
            }
            return Optional.ofNullable(source.get(this.componentName));
        }
    }
}
