/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.service;

import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.empty;
import static org.apache.webbeans.util.Asserts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;
import org.talend.sdk.component.server.dao.ComponentActionDao;
import org.talend.sdk.component.server.dao.ComponentDao;
import org.talend.sdk.component.server.dao.ComponentFamilyDao;

@MonoMeecrowaveConfig
class ComponentManagerServiceTest {

    @Inject
    private ComponentManagerService componentManagerService;

    @Inject
    private ComponentDao componentDao;

    @Inject
    private ComponentFamilyDao componentFamilyDao;

    @Inject
    private ComponentActionDao componentActionDao;

    @Test
    void deployExistingPlugin() {
        try {
            componentManagerService.deploy("org.talend.test1:the-test-component:jar:1.2.6:compile");
        } catch (final IllegalArgumentException iae) {
            assertTrue(iae.getMessage().contains("Container 'the-test-component' already exists"));
        }
    }

    @Test // undeploy => deploy
    void reloadExistingPlugin() {
        // check that the plugin is deployed
        final String gav = "org.talend.test1:the-test-component:jar:1.2.6:compile";
        String pluginID = getPluginId(gav);
        assertNotNull(pluginID);
        Optional<Container> plugin = componentManagerService.manager().findPlugin(pluginID);
        assertTrue(plugin.isPresent());
        final Set<String> componentIds = plugin
                .get()
                .get(ContainerComponentRegistry.class)
                .getComponents()
                .values()
                .stream()
                .flatMap(c -> Stream
                        .concat(c.getPartitionMappers().values().stream(), c.getProcessors().values().stream()))
                .map(ComponentFamilyMeta.BaseMeta::getId)
                .collect(toSet());
        final Set<String> familiesIds = plugin
                .get()
                .get(ContainerComponentRegistry.class)
                .getComponents()
                .values()
                .stream()
                .map(f -> IdGenerator.get(f.getPlugin(), f.getName()))
                .collect(toSet());

        componentIds.forEach(id -> assertNotNull(componentDao.findById(id)));
        familiesIds.forEach(id -> assertNotNull(componentFamilyDao.findById(id)));

        // undeploy the existing plugin
        componentManagerService.undeploy("org.talend.test1:the-test-component:jar:1.2.6:compile");
        plugin = componentManagerService.manager().findPlugin(pluginID);
        assertFalse(plugin.isPresent());
        componentIds.forEach(id -> assertNull(componentDao.findById(id)));
        familiesIds.forEach(id -> assertNull(componentFamilyDao.findById(id)));

        // deploy
        componentManagerService.deploy(gav);
        pluginID = getPluginId(gav);
        assertNotNull(pluginID);
        plugin = componentManagerService.manager().findPlugin(pluginID);
        assertTrue(plugin.isPresent());
        componentIds.forEach(id -> assertNotNull(componentDao.findById(id)));
        familiesIds.forEach(id -> assertNotNull(componentFamilyDao.findById(id)));
    }

    @Test
    void undeployNonExistingPlugin() {
        final String gav = "org.talend:non-existing-component:jar:0.0.0:compile";
        try {
            componentManagerService.undeploy(gav);
        } catch (final RuntimeException re) {
            assertTrue(re.getMessage().contains("No plugin found using maven GAV: " + gav));
        }
    }

    private String getPluginId(final String gav) {
        return componentManagerService
                .manager()
                .find(c -> gav.equals(c.get(ComponentManager.OriginalId.class).getValue()) ? Stream.of(c.getId())
                        : empty())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No plugin found using maven GAV: " + gav));
    }

}
