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
package org.talend.sdk.component.runtime.manager;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;

public class ComponentManagerTest {

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Test
    public void run() throws Exception {
        final File pluginFolder = new File(TEMPORARY_FOLDER.getRoot(), "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();

        // just some jars with classes we can scan
        final File plugin1 = pluginGenerator.createPlugin(pluginFolder, "plugin1.jar",
                "org.apache.tomee:openejb-itests-beans:jar:7.0.3:runtime");
        final File plugin2 = pluginGenerator.createPlugin(pluginFolder, "plugin2.jar",
                "org.apache.tomee:arquillian-tomee-codi-tests:jar:7.0.3:runtime");

        // ensure jmx value is free and we don't get a test luck
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        assertFalse(mBeanServer.isRegistered(new ObjectName("org.talend.test:type=plugin,value=plugin1")));

        try (final ComponentManager manager = new ComponentManager(new File("target/test-dependencies"),
                "META-INF/test/dependencies", "org.talend.test:type=plugin,value=%s")) {
            doCheckRegistry(plugin1, plugin2, manager);
            final Date plugin1CreatedDate = doCheckJmx(mBeanServer);

            // now remove a plugin and check the meta disappeared
            manager.removePlugin(plugin1.getName().replace(".jar", ""));
            assertEquals(1, manager.find(Stream::of).count());
            assertEquals("plugin2", manager.find(Stream::of).findFirst().get().getId());

            manager.addPlugin(plugin1.getAbsolutePath());
            final Date plugin1RecreatedDate = doCheckJmx(mBeanServer);
            assertTrue(plugin1CreatedDate.getTime() <= plugin1RecreatedDate.getTime());
        } finally { // clean temp files
            DynamicContainerFinder.LOADERS.clear();

            Stream.of(pluginFolder.listFiles()).forEach(File::delete);
            if (ofNullable(pluginFolder.listFiles()).map(f -> f.length == 0).orElse(true)) {
                pluginFolder.delete();
            }
        }

        // unregistered
        assertFalse(mBeanServer.isRegistered(new ObjectName("org.talend.test:type=plugin,value=plugin1")));
    }

    private Date doCheckJmx(final MBeanServer mBeanServer) throws Exception {
        final ObjectName name = new ObjectName("org.talend.test:value=plugin1,type=plugin");
        assertTrue(mBeanServer.isRegistered(name));
        assertFalse(Boolean.class.cast(mBeanServer.getAttribute(name, "closed")));
        assertThat(mBeanServer.getAttribute(name, "created"), instanceOf(Date.class));
        // ensure date is stable until reloading
        assertEquals(mBeanServer.getAttribute(name, "created"), mBeanServer.getAttribute(name, "created"));
        return Date.class.cast(mBeanServer.getAttribute(name, "created"));
    }

    private void doCheckRegistry(final File plugin1, final File plugin2, final ComponentManager manager) throws Exception {
        Stream.of(plugin1, plugin2).map(File::getAbsolutePath).forEach(manager::addPlugin);
        final List<ContainerComponentRegistry> registries = manager.find(c -> Stream.of(c.get(ContainerComponentRegistry.class)))
                .collect(toList());
        assertEquals(2, registries.size()); // we saw both plugin

        registries.forEach(registry -> {
            final Container container = manager
                    .find(c -> registry == c.get(ContainerComponentRegistry.class) ? Stream.of(c) : Stream.empty()).findFirst()
                    .get();

            assertEquals(1, registry.getServices().size());
            assertNotNull(registry.getServices().iterator().next().getInstance());

            final Collection<ServiceMeta.ActionMeta> actions = registry.getServices().iterator().next().getActions();
            assertEquals(1, actions.size());
            assertEquals(pluginGenerator.toPackage(container.getId()) + ".AModel",
                    actions.iterator().next().getInvoker().apply(null).getClass().getName());

            assertEquals(1, registry.getComponents().size());
            registry.getComponents().forEach((name, component) -> {
                assertEquals("comp", name);
                assertEquals(name, component.getName());
                assertEquals(singletonList("Misc"), component.getCategories());
                assertEquals(1, component.getProcessors().size());
                component.getProcessors().forEach((procName, processorMeta) -> {
                    assertEquals("proc", procName);
                    assertEquals("default", processorMeta.getIcon());

                    final String packageName = pluginGenerator.toPackage(container.getId());

                    final Processor processor = processorMeta.getInstantiator().apply(emptyMap());
                    assertNotNull(processor);

                    final Object model;
                    try {
                        final String className = packageName + ".AModel";
                        model = container.getLoader().loadClass(className).getConstructor().newInstance();
                    } catch (final Exception e) {
                        fail(e.getMessage());
                        throw new IllegalArgumentException(e);
                    }

                    runProcessorLifecycle(model, processor);
                });
            });
        });

        // now try to execute the processor outside the correct TCCL and ensure it still works
        final Container container = manager.find(Stream::of).findFirst().get();
        final String packageName = pluginGenerator.toPackage(container.getId());
        final ContainerComponentRegistry registry = container.get(ContainerComponentRegistry.class);
        final ComponentFamilyMeta componentFamilyMeta = registry.getComponents().values().iterator().next();
        final ComponentFamilyMeta.ProcessorMeta processorMeta = componentFamilyMeta.getProcessors().values().iterator().next();
        final Processor processor = processorMeta.getInstantiator().apply(emptyMap());
        final Object aModel = container.getLoader().loadClass(packageName + ".AModel").getConstructor().newInstance();
        runProcessorLifecycle(aModel, processor);

        // finally ensure it is serializable
        DynamicContainerFinder.LOADERS.clear();
        manager.find(Stream::of).forEach(c -> DynamicContainerFinder.LOADERS.put(c.getId(), c.getLoader()));
        runProcessorLifecycle(aModel, copy(processor, container.getLoader()));
    }

    private org.talend.sdk.component.runtime.output.Processor copy(
            final org.talend.sdk.component.runtime.output.Processor processor, final ClassLoader loader)
            throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(processor);
        }
        try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(baos.toByteArray()), loader)) {
            return org.talend.sdk.component.runtime.output.Processor.class.cast(ois.readObject());
        }
    }

    private void runProcessorLifecycle(final Object model, final org.talend.sdk.component.runtime.output.Processor proc) {
        proc.start();
        proc.beforeGroup();
        final AtomicReference<Object> transformedRef = new AtomicReference<>();
        proc.onNext(name -> {
            assertEquals("__default__", name);
            return model;
        }, name -> {
            assertEquals("__default__", name);
            return value -> assertTrue(transformedRef.compareAndSet(null, value));
        });
        final Object transformed = transformedRef.get();
        assertNotNull(transformed);
        assertNotSame(transformed, model);
        assertEquals(model.getClass(), transformed.getClass());
        proc.afterGroup(name -> {
            assertEquals("__default__", name);
            return value -> assertTrue(transformedRef.compareAndSet(null, value));
        });
        proc.stop();
    }
}
