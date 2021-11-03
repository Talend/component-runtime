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
package org.talend.sdk.component.runtime.manager.service.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager.ComponentType;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

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

    @RequiredArgsConstructor
    class BuilderDefault implements Builder {

        private final Supplier<ContainerComponentRegistry> registrySupplier;

        @Override
        public ComponentInstantiator build(final String familyName, final MetaFinder finder,
                final ComponentType componentType) {
            return Optional
                    .ofNullable(this.registrySupplier.get())
                    .map((ContainerComponentRegistry registry) -> registry.findComponentFamily(familyName))
                    .map(componentType::findMeta)
                    .flatMap((Map<String, ? extends ComponentFamilyMeta.BaseMeta> map) -> finder.filter(map))
                    .map((ComponentFamilyMeta.BaseMeta c) -> (ComponentInstantiator) c::instantiate)
                    .orElse(null);
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    class ComponentNameFinder implements MetaFinder {

        private final String componentName;

        @Override
        public Optional<? extends ComponentFamilyMeta.BaseMeta>
                filter(final Map<String, ? extends ComponentFamilyMeta.BaseMeta> source) {
            if (!source.containsKey(this.componentName)) {
                log.warn("Can't find component name {}", this.componentName);
            }
            return Optional.ofNullable(source.get(this.componentName));
        }
    }

    @Slf4j
    @RequiredArgsConstructor
    class ComponentMetaFinder implements MetaFinder {

        private final String inputName;

        @Override
        public Optional<? extends ComponentFamilyMeta.BaseMeta>
                filter(final Map<String, ? extends ComponentFamilyMeta.BaseMeta> source) {
            final Optional<? extends ComponentFamilyMeta.BaseMeta> result =
                    source.values().stream().filter(this::isSearchedMeta).findFirst();
            if (!result.isPresent()) {
                log.warn("Can't find input name {}", this.inputName);
            }
            return result;
        }

        private boolean isSearchedMeta(final ComponentFamilyMeta.BaseMeta meta) {
            if (!(meta instanceof ComponentFamilyMeta.PartitionMapperMeta)) {
                return false;
            }
            final Supplier<List<ParameterMeta>> parameterMetas = meta.getParameterMetas();
            final List<ParameterMeta> metas = parameterMetas.get();

            return metas.parallelStream().anyMatch(this::containsDataset);
        }

        private boolean containsDataset(final ParameterMeta parameters) {
            return this.isDataset(parameters)
                    || parameters.getNestedParameters().parallelStream().anyMatch(this::containsDataset);
        }

        private boolean isDataset(final ParameterMeta parameters) {
            final Map<String, String> metadata = parameters.getMetadata();
            return this.inputName.equals(metadata.get("tcomp::configuration::name"));
        }
    }
}
