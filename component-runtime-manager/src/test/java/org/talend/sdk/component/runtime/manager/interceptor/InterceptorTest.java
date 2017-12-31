/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.interceptor;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.talend.sdk.component.runtime.manager.test.Serializer.roundTrip;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

public class InterceptorTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Test
    public void run() throws Exception {
        final File pluginFolder = new File(TEMPORARY_FOLDER.getRoot(), "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createChainPlugin(pluginFolder, "plugin.jar");

        try (final ComponentManager manager = new ComponentManager(new File("target/test-dependencies"),
                "META-INF/test/dependencies", "org.talend.test:type=plugin,value=%s")) {
            manager.addPlugin(plugin.getAbsolutePath());
            final List<Object> collect = manager
                    .find(c -> c.get(ComponentManager.AllServices.class).getServices().values().stream())
                    .filter(c -> c.getClass().getName().endsWith("SuperService$$TalendServiceProxy"))
                    .collect(toList());
            assertEquals(1, collect.size());

            final Object instance = collect.iterator().next();
            final Method method = instance.getClass().getMethod("canBeLong", int.class);
            assertEquals("exec_1/1", method.invoke(instance, 1));
            assertEquals("exec_1/1", method.invoke(instance, 1));
            sleep(450);
            assertEquals("exec_1/2", method.invoke(instance, 1));
            assertEquals("exec_3/3", method.invoke(instance, 3));
            assertEquals("exec_4/4", method.invoke(instance, 4));

            final LightContainer container =
                    manager.find(c -> Stream.of(c.get(LightContainer.class))).findFirst().get();
            DynamicContainerFinder.LOADERS.put("plugin", container.classloader());
            DynamicContainerFinder.SERVICES.put(instance.getClass().getSuperclass(), instance);
            final Object roundTrip = roundTrip(instance);
            assertEquals(roundTrip, instance);
        } finally { // clean temp files
            DynamicContainerFinder.LOADERS.clear();
            DynamicContainerFinder.SERVICES.clear();
        }
    }
}
