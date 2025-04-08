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

import java.util.*;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;

class ComponentInstantiatorTest {

    @Test
    void instantiate() {
        final ContainerComponentRegistry registry = new ContainerComponentRegistry();

        final ComponentFamilyMeta familyMeta =
                new ComponentFamilyMeta("pluginId", Arrays.asList("cat1", "cat2"), "theIcon", "name", "packageName",
                        "");
        ComponentFamilyMeta.PartitionMapperMeta meta = new FakeMapperMeta(familyMeta);
        familyMeta.getPartitionMappers().put("name", meta);

        registry.getComponents().put("pluginId", familyMeta);

        ComponentInstantiator.BuilderDefault builder =
                new ComponentInstantiator.BuilderDefault(() -> Stream.of(registry));
        ComponentInstantiator.MetaFinder finder = new ComponentInstantiator.ComponentNameFinder("name");

        final ComponentInstantiator instanciator =
                builder.build("pluginId", finder, ComponentManager.ComponentType.MAPPER);
        final Lifecycle lifecycle = instanciator.instantiate(Collections.emptyMap(), 2);

        Assertions.assertNotNull(lifecycle, "lifecycle is null");
        Assertions.assertTrue(lifecycle instanceof FakeMapper, lifecycle.getClass().getName());
    }

    @Test
    void instantiateWhenEqualFamilyName() {
        final ComponentFamilyMeta familyMeta =
                new ComponentFamilyMeta("pluginId", Arrays.asList("cat1", "cat2"), "theIcon", "name", "packageName",
                        "");
        ComponentFamilyMeta.PartitionMapperMeta meta = new FakeMapperMeta(familyMeta);
        familyMeta.getPartitionMappers().put("name", meta);

        final ComponentFamilyMeta familyMeta2 =
                new ComponentFamilyMeta("pluginId", Arrays.asList("dog1", "dog2"), "theIcon", "foo", "packageName", "");
        ComponentFamilyMeta.PartitionMapperMeta meta2 = new FakeMapperMeta(familyMeta2);
        familyMeta2.getPartitionMappers().put("foo", meta2);

        final ContainerComponentRegistry registry = new ContainerComponentRegistry();
        registry.getComponents().put("pluginId", familyMeta);

        final ContainerComponentRegistry registry2 = new ContainerComponentRegistry();
        registry2.getComponents().put("pluginId", familyMeta2);

        ComponentInstantiator.BuilderDefault builder =
                new ComponentInstantiator.BuilderDefault(() -> Stream.of(registry, registry2));

        ComponentInstantiator.MetaFinder finder = new ComponentInstantiator.ComponentNameFinder("name");

        final ComponentInstantiator instanciator =
                builder.build("pluginId", finder, ComponentManager.ComponentType.MAPPER);
        Assertions.assertNotNull(instanciator, "lifecycle is null");
        final Lifecycle lifecycle = instanciator.instantiate(Collections.emptyMap(), 2);

        Assertions.assertNotNull(lifecycle, "lifecycle is null");
        Assertions.assertTrue(lifecycle instanceof FakeMapper, lifecycle.getClass().getName());

        ComponentInstantiator.MetaFinder finder2 = new ComponentInstantiator.ComponentNameFinder("foo");

        final ComponentInstantiator instanciator2 =
                builder.build("pluginId", finder2, ComponentManager.ComponentType.MAPPER);
        Assertions.assertNotNull(instanciator2, "lifecycle is null");
        final Lifecycle lifecycle2 = instanciator2.instantiate(Collections.emptyMap(), 2);

        Assertions.assertNotNull(lifecycle2, "lifecycle is null");
        Assertions.assertTrue(lifecycle2 instanceof FakeMapper, lifecycle2.getClass().getName());
    }

    static class FakeMapperMeta extends ComponentFamilyMeta.PartitionMapperMeta {

        public FakeMapperMeta(final ComponentFamilyMeta familyMeta) {
            super(familyMeta, "name", "icon", 1, FakeMapper.class, ComponentInstantiatorTest::getMetas,
                    (Map<String, String> cfg) -> new FakeMapper(),
                    () -> (int incomingVersion, Map<String, String> incomingData) -> incomingData, true, false);
        }
    }

    @Test
    void testFinder() {
        final ComponentInstantiator.MetaFinder dataSet = new ComponentInstantiator.ComponentNameFinder("meta1");
        Assertions.assertFalse(dataSet.filter(Collections.emptyMap()).isPresent());

        final ComponentFamilyMeta familyMeta =
                new ComponentFamilyMeta("pluginId", Arrays.asList("cat1", "cat2"), "theIcon", "meta1", "packageName",
                        "");

        final ComponentFamilyMeta.PartitionMapperMeta meta1 = new FakeMapperMeta(familyMeta);
        final Map<String, ComponentFamilyMeta.BaseMeta> meta = new HashMap<>();
        meta.put("meta1", meta1);
        final Optional<? extends ComponentFamilyMeta.BaseMeta> baseMeta = dataSet.filter(meta);
        Assertions.assertTrue(baseMeta.isPresent());
        Assertions.assertSame(meta1, baseMeta.get());

    }

    static List<ParameterMeta> getMetas() {
        final ParameterMeta.Source source = new ParameterMeta.Source() {

            @Override
            public String name() {
                return null;
            }

            @Override
            public Class<?> declaringClass() {
                return null;
            }
        };
        Map<String, String> goodMap = new HashMap<>();
        goodMap.put("tcomp::configuration::type", "dataset");
        goodMap.put("tcomp::configuration::name", "TheDataSet");
        final ParameterMeta meta1 = new ParameterMeta(source, Object.class, ParameterMeta.Type.OBJECT, "thePath",
                "name", null, Collections.emptyList(), Collections.emptyList(), goodMap, true);
        ParameterMeta meta2 = new ParameterMeta(source, Object.class, ParameterMeta.Type.OBJECT, "thePath", "name",
                null, Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), true);
        ParameterMeta meta3 = new ParameterMeta(source, Object.class, ParameterMeta.Type.OBJECT, "thePath", "name",
                null, Arrays.asList(meta1, meta2), Collections.emptyList(), Collections.emptyMap(), true);
        return Arrays.asList(meta2, meta3);
    }

    static class FakeMapper implements Mapper {

        @Override
        public String plugin() {
            return null;
        }

        @Override
        public String rootName() {
            return null;
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public long assess() {
            return 0;
        }

        @Override
        public List<Mapper> split(long desiredSize) {
            return null;
        }

        @Override
        public Input create() {
            return null;
        }

        @Override
        public boolean isStream() {
            return false;
        }
    }
}