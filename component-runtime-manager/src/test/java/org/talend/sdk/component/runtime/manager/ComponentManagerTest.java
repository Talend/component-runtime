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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.xbean.finder.util.Files;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager.AllServices;
import org.talend.sdk.component.runtime.manager.asm.PluginGenerator;
import org.talend.sdk.component.runtime.manager.chain.Job;
import org.talend.sdk.component.runtime.manager.serialization.DynamicContainerFinder;
import org.talend.sdk.component.runtime.output.Processor;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.serialization.EnhancedObjectInputStream;
import sun.net.www.protocol.jar.Handler;

class ComponentManagerTest {

    static class NoCacheJarUrlStreamHandler extends Handler {

        public NoCacheJarUrlStreamHandler() {
            super();
        }

        protected URLConnection openConnection(URL var1) throws IOException {
            URLConnection connection = super.openConnection(var1);
            connection.setUseCaches(false);
            return connection;
        }
    }

    static class NoCacheUrlStreamHandlerFactory implements URLStreamHandlerFactory {

        @Override
        public URLStreamHandler createURLStreamHandler(final String protocol) {
            if (protocol.equalsIgnoreCase("jar")) {
                return new NoCacheJarUrlStreamHandler();
            }
            return null;
        }
    }

    @BeforeAll
    public static void setup() {
        URL.setURLStreamHandlerFactory(new NoCacheUrlStreamHandlerFactory());
    }

    private final PluginGenerator pluginGenerator = new PluginGenerator();

    private ComponentManager newManager(final File m2) {
        return new ComponentManager(m2, "META-INF/test/dependencies", "org.talend.test:type=plugin,value=%s");
    }

    private ComponentManager newManager() {
        return newManager(new File("target/test-dependencies"));
    }

    @Test
    void doubleClose() {
        final ComponentManager instance = ComponentManager.instance();
        final ComponentManager instanceCopy = ComponentManager.instance();
        Assertions.assertSame(instance, instanceCopy);
        instance.close();
        Assertions.assertTrue(instance.getContainer().isClosed());
        final ComponentManager instance2 = ComponentManager.instance();
        Assertions.assertNotNull(instance2);
        Assertions.assertNotSame(instance, instance2);
        Assertions.assertFalse(instance2.getContainer().isClosed());
    }

    @Test
    void doubleCloseOnThread() throws InterruptedException {
        final ComponentManager instance = ComponentManager.instance();
        final Runnable r1 = () -> instance.close();

        final AtomicReference<AssertionFailedError> failure = new AtomicReference<>();
        final Runnable r2 = () -> {
            try {
                ComponentManager instance2 = ComponentManager.instance();
                Assertions.assertNotNull(instance2, "second instance is NULL");
                while (instance2 == instance) {
                    instance2 = ComponentManager.instance();
                }
                Assertions.assertNotNull(instance2, "second instance is NULL");
                Assertions.assertFalse(instance2.getContainer().isClosed(), "second instance is closed");
            } catch (AssertionFailedError ex) {
                failure.set(ex);
            }
        };
        final Thread t1 = new Thread(r1);
        final Thread t2 = new Thread(r2);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        if (failure.get() != null) {
            throw failure.get();
        }
    }

    private static void pause() {
        try {
            Thread.sleep(10L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void configInstantiation(@TempDir final File temporaryFolder) {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createChainPlugin(pluginFolder, "plugin.jar");
        DynamicContainerFinder.SERVICES.put(RecordBuilderFactory.class, new RecordBuilderFactoryImpl("plugin"));
        final String jvd = System.getProperty("java.version.date"); // java 11
        System.clearProperty("java.version.date");
        try (final ComponentManager manager =
                new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
            manager.addPlugin(plugin.getAbsolutePath());
            final Mapper mapper =
                    manager.findMapper("config", "injected", 1, emptyMap()).orElseThrow(IllegalStateException::new);
            final Record next = Record.class.cast(mapper.create().next());
            assertEquals(System.getProperty("java.version", "notset-on-jvm"), next.get(String.class, "value"));
        } finally { // clean temp files
            DynamicContainerFinder.SERVICES.clear();
            doCleanup(pluginFolder);
            if (jvd != null) {
                System.setProperty("java.version.date", jvd);
            }
        }
    }

    @Test
    void testInstance() throws InterruptedException {
        final ComponentManager[] managers = new ComponentManager[60];
        Thread[] th = new Thread[managers.length];
        for (int ind = 0; ind < th.length; ind++) {
            final int indice = ind;
            th[ind] = new Thread(() -> {
                managers[indice] = ComponentManager.instance();
            });
            th[ind].start();
        }
        for (int ind = 0; ind < th.length; ind++) {
            th[ind].join();
        }
        Assertions.assertNotNull(managers[0]);
        for (int i = 1; i < managers.length; i++) {
            Assertions.assertSame(managers[0], managers[i], "manager " + i + " is another instance");
        }
    }

    @Test
    void addPluginMultiThread(@TempDir final File temporaryFolder) throws InterruptedException {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createChainPlugin(pluginFolder, "plugin.jar");
        DynamicContainerFinder.SERVICES.put(RecordBuilderFactory.class, new RecordBuilderFactoryImpl("plugin"));
        final String jvd = System.getProperty("java.version.date"); // java 11
        System.clearProperty("java.version.date");
        try (final ComponentManager manager =
                new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
            final String pluginPath = plugin.getAbsolutePath();
            Thread[] th = new Thread[5];
            for (int ind = 0; ind < th.length; ind++) {
                final int indice = ind;
                th[ind] = new Thread(() -> {
                    manager.addPlugin(pluginPath);
                });
            }
            for (int ind = 0; ind < th.length; ind++) {
                th[ind].start();
            }
            for (int ind = 0; ind < th.length; ind++) {
                th[ind].join();
            }
        } finally { // clean temp files
            DynamicContainerFinder.SERVICES.clear();
            doCleanup(pluginFolder);
            if (jvd != null) {
                System.setProperty("java.version.date", jvd);
            }
        }
    }

    @Test
    void run(@TempDir final File temporaryFolder) throws Exception {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();

        // just some jars with classes we can scan
        final File plugin1 = pluginGenerator
                .createPlugin(pluginFolder, "plugin1.jar", "org.apache.tomee:openejb-itests-beans:jar:7.0.5:runtime");
        final File plugin2 = pluginGenerator
                .createPlugin(pluginFolder, "plugin2.jar",
                        "org.apache.tomee:arquillian-tomee-codi-tests:jar:7.0.5:runtime");

        // ensure jmx value is free and we don't get a test luck
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        assertFalse(mBeanServer.isRegistered(new ObjectName("org.talend.test:type=plugin,value=plugin1")));

        try (final ComponentManager manager = newManager()) {
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
            doCleanup(pluginFolder);
        }

        // unregistered
        assertFalse(mBeanServer.isRegistered(new ObjectName("org.talend.test:type=plugin,value=plugin1")));
    }

    @Test
    void extendFamily(@TempDir final File temporaryFolder) throws Exception {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();

        // just some jars with classes we can scan
        final File transitive = pluginGenerator
                .createPluginAt(new File(new File("target/test-dependencies"),
                        "org/talend/test/transitive/1.0.0/transitive-1.0.0.jar"), jar -> {
                            try {
                                jar.write(pluginGenerator.createProcessor(jar, "org/test2", "second"));
                                jar.write(pluginGenerator.createModel(jar, "org/test2"));
                            } catch (final IOException e) {
                                fail(e.getMessage());
                            }
                        },
                        // must be ignored, if needed it will be in main dependencies.txt
                        "org.apache.tomee:openejb-itests-beans:jar:7.0.5:runtime");
        final File plugin2 = pluginGenerator.createPlugin(pluginFolder, "main.jar", "org.talend.test:transitive:1.0.0");

        try (final ComponentManager manager = newManager()) {
            try {
                manager.addPlugin(plugin2.getAbsolutePath());

                final Container container = validateTransitiveComponent(manager);

                final String[] dependencies = Stream
                        .of(container.getLoader().getURLs())
                        .map(Files::toFile)
                        .map(File::getName)
                        .sorted()
                        .toArray(String[]::new);
                assertEquals(2, dependencies.length); // ignored transitive deps, enables the new root to control it
                assertEquals("main.jar", dependencies[0]);
                assertEquals("transitive-1.0.0.jar", dependencies[1]);
            } finally {
                if (!transitive.delete()) {
                    transitive.deleteOnExit();
                }
            }
        } finally { // clean temp files
            doCleanup(pluginFolder);
        }
    }

    @Test
    void extendFamilyInNestedRepo(@TempDir final File temporaryFolder) throws Exception {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();

        // just some jars with classes we can scan
        final File transitive = pluginGenerator.createPluginAt(new File(pluginFolder, "transitive-1.0.0.jar"), jar -> {
            try {
                jar.write(pluginGenerator.createProcessor(jar, "org/test2", "second"));
                jar.write(pluginGenerator.createModel(jar, "org/test2"));
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        },
                // must be ignored, if needed it will be in main dependencies.txt
                "org.apache.tomee:openejb-itests-beans:jar:7.0.5:runtime");
        final File plugin2 = pluginGenerator
                .createPluginAt(new File(pluginFolder, "main.jar"),
                        jar -> pluginGenerator.createComponent("comp", jar, "org/test"),
                        "org.talend.test:transitive:1.0.0");

        final File fatJar = new File(pluginFolder, "fatjar.jar");
        try (final JarOutputStream jar = new JarOutputStream(new FileOutputStream(fatJar))) {
            try {
                jar.putNextEntry(new JarEntry("MAVEN-INF/repository/"));
                jar.closeEntry();

                jar
                        .putNextEntry(new JarEntry(
                                "MAVEN-INF/repository/org/talend/test/transitive/1.0.0/transitive-1.0.0.jar"));
                java.nio.file.Files.copy(transitive.toPath(), jar);
                jar.closeEntry();
            } catch (final IOException ioe) {
                fail(ioe.getMessage());
            }
        }
        final Thread thread = Thread.currentThread();
        final URLClassLoader parentLoader =
                new URLClassLoader(new URL[] { fatJar.toURI().toURL() }, thread.getContextClassLoader());
        thread.setContextClassLoader(parentLoader);
        try (final ComponentManager manager = newManager(new File("target/missing_" + UUID.randomUUID().toString()))) {
            try {
                manager.addPlugin(plugin2.getAbsolutePath());

                final Container container = validateTransitiveComponent(manager);

                final String[] dependencies = Stream
                        .of(container.getLoader().getURLs())
                        .map(Files::toFile)
                        .map(File::getName)
                        .sorted()
                        .toArray(String[]::new);
                assertEquals(1, dependencies.length); // ignored transitive deps, enables the new root to control it
                assertEquals("main.jar", dependencies[0]); // transitive-1.0.0.jar is nested
            } finally {
                if (!transitive.delete()) {
                    transitive.deleteOnExit();
                }
            }
        } finally { // clean temp files
            thread.setContextClassLoader(parentLoader.getParent());
            parentLoader.close();
            doCleanup(pluginFolder);
        }
    }

    private Container validateTransitiveComponent(ComponentManager manager) {
        final Collection<Container> containers = manager.getContainer().findAll();
        assertEquals(1, containers.size());

        final Container container = containers.iterator().next();
        final List<ComponentFamilyMeta.ProcessorMeta> processors = Stream
                .of(container.get(ContainerComponentRegistry.class))
                .map(ContainerComponentRegistry::getComponents)
                .flatMap(comps -> comps.values().stream())
                .flatMap(family -> family.getProcessors().values().stream())
                .collect(toList());
        assertEquals(asList("proc", "second"),
                processors.stream().map(ComponentFamilyMeta.ProcessorMeta::getName).sorted().collect(toList()));
        return container;
    }

    private void doCleanup(final File pluginFolder) {
        DynamicContainerFinder.LOADERS.clear();
        Stream.of(pluginFolder.listFiles()).forEach(File::delete);
        if (ofNullable(pluginFolder.listFiles()).map(f -> f.length == 0).orElse(true)) {
            pluginFolder.delete();
        }
    }

    private Date doCheckJmx(final MBeanServer mBeanServer) throws Exception {
        final ObjectName name = new ObjectName("org.talend.test:value=plugin1,type=plugin");
        assertTrue(mBeanServer.isRegistered(name));
        assertFalse(Boolean.class.cast(mBeanServer.getAttribute(name, "closed")));
        assertTrue(() -> {
            try {
                return Date.class.isInstance(mBeanServer.getAttribute(name, "created"));
            } catch (final MBeanException | AttributeNotFoundException | ReflectionException
                    | InstanceNotFoundException e) {
                return false;
            }
        });
        // ensure date is stable until reloading
        assertEquals(mBeanServer.getAttribute(name, "created"), mBeanServer.getAttribute(name, "created"));
        return Date.class.cast(mBeanServer.getAttribute(name, "created"));
    }

    private void doCheckRegistry(final File plugin1, final File plugin2, final ComponentManager manager)
            throws Exception {
        Stream.of(plugin1, plugin2).map(File::getAbsolutePath).forEach(manager::addPlugin);
        final List<ContainerComponentRegistry> registries =
                manager.find(c -> Stream.of(c.get(ContainerComponentRegistry.class))).collect(toList());
        assertEquals(2, registries.size()); // we saw both plugin

        registries.forEach(registry -> {
            final Container container = manager
                    .find(c -> registry == c.get(ContainerComponentRegistry.class) ? Stream.of(c) : Stream.empty())
                    .findFirst()
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

        // now try to execute the processor outside the correct TCCL and ensure it still
        // works
        final Container container = manager.find(Stream::of).findFirst().get();
        final String packageName = pluginGenerator.toPackage(container.getId());
        final ContainerComponentRegistry registry = container.get(ContainerComponentRegistry.class);
        final ComponentFamilyMeta componentFamilyMeta = registry.getComponents().values().iterator().next();
        final ComponentFamilyMeta.ProcessorMeta processorMeta =
                componentFamilyMeta.getProcessors().values().iterator().next();
        final Processor processor = processorMeta.getInstantiator().apply(emptyMap());
        final Object aModel = container.getLoader().loadClass(packageName + ".AModel").getConstructor().newInstance();
        runProcessorLifecycle(aModel, processor);

        // finally ensure it is serializable
        DynamicContainerFinder.LOADERS.clear();
        manager.find(Stream::of).forEach(c -> DynamicContainerFinder.LOADERS.put(c.getId(), c.getLoader()));
        runProcessorLifecycle(aModel, copy(processor, container.getLoader()));
    }

    private org.talend.sdk.component.runtime.output.Processor
            copy(final org.talend.sdk.component.runtime.output.Processor processor, final ClassLoader loader)
                    throws IOException, ClassNotFoundException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(processor);
        }
        try (final ObjectInputStream ois =
                new EnhancedObjectInputStream(new ByteArrayInputStream(baos.toByteArray()), loader)) {
            return org.talend.sdk.component.runtime.output.Processor.class.cast(ois.readObject());
        }
    }

    private void runProcessorLifecycle(final Object model,
            final org.talend.sdk.component.runtime.output.Processor proc) {
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

    @Test
    void testLocalConfigurationFromEnvironment(@TempDir final File temporaryFolder) throws Exception {
        final File pluginFolder = new File(temporaryFolder, "test-plugins_" + UUID.randomUUID().toString());
        pluginFolder.mkdirs();
        final File plugin = pluginGenerator.createPlugin(pluginFolder, "plugin.jar");
        try (final ComponentManager manager =
                new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
            manager.addPlugin(plugin.getAbsolutePath());
            final Container container = manager.getContainer().findAll().stream().findFirst().orElse(null);
            assertNotNull(container);
            final LocalConfiguration envConf = LocalConfiguration.class
                    .cast(container.get(AllServices.class).getServices().get(LocalConfiguration.class));
            // check translated env vars
            assertEquals("/home/user", envConf.get("USER_PATH"));
            assertEquals("/home/user", envConf.get("USER.PATH"));
            assertEquals("/home/user", envConf.get("user_path"));
            assertEquals("/home/user", envConf.get("user_PATH"));
            assertEquals("/home/user", envConf.get("talend_localconfig_user_home"));
            assertEquals("/home/user", envConf.get("talend.localconfig.user.home"));
            assertEquals("true", envConf.get("talend.LOCALCONFIG.test.0"));
            assertEquals("true", envConf.get("talend.localconfig.test_0"));
            assertEquals("true", envConf.get("talend.localconfig.TEST.0"));
            assertEquals("true", envConf.get("talend.localconfig.test#0"));
            assertEquals("true", envConf.get("talend$localconfig.test+0"));
            // check for non existing values
            assertNull(envConf.get("talend.compmgr.exists"));
            assertNull(envConf.get("HOMER"));
            // Not running correctly on Windows machines. Do we need this test at all?
            if(!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
                assertNull(envConf.get("TALEND_LOCALCONFIG_USER_HOME"));
            }
        } finally { // clean temp files
            doCleanup(pluginFolder);
        }
    }

}
