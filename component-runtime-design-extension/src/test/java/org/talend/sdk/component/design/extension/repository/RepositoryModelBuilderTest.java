/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.design.extension.repository;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.xbean.asm9.ClassReader.EXPAND_FRAMES;
import static org.apache.xbean.asm9.ClassWriter.COMPUTE_FRAMES;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.commons.ClassRemapper;
import org.apache.xbean.asm9.commons.Remapper;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.reflect.MigrationHandlerFactory;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;
import org.talend.test.DataStore1;
import org.talend.test.PartitionMapper1;
import org.talend.test.WrappingStore;

class RepositoryModelBuilderTest {

    @Test
    void complexTree() {
        try (final ComponentManager manager = new ComponentManager(new File("target/foo"), "deps.txt", null)) {
            final String plugin = manager.addPlugin(jarLocation(RepositoryModelBuilderTest.class).getAbsolutePath());
            final RepositoryModel model =
                    manager.findPlugin(plugin).orElseThrow(IllegalArgumentException::new).get(RepositoryModel.class);

            final Family theTestFamily = model
                    .getFamilies()
                    .stream()
                    .filter(f -> f.getMeta().getName().equals("TheTestFamily"))
                    .findFirst()
                    .orElseThrow(IllegalArgumentException::new);
            assertEquals(3, theTestFamily.getConfigs().get().size(),
                    theTestFamily
                            .getConfigs()
                            .get()
                            .stream()
                            .map(c -> c.getKey().getConfigName())
                            .collect(joining(", ")));
            theTestFamily
                    .getConfigs()
                    .get()
                    .forEach(it -> assertTrue(it.getKey().getConfigName().startsWith("Connection-"),
                            it.getKey().getConfigName()));
        }
    }

    @Test
    void notRootConfig() {
        final PropertyEditorRegistry registry = new PropertyEditorRegistry();
        final RepositoryModel model = new RepositoryModelBuilder()
                .create(new ComponentManager.AllServices(emptyMap()),
                        singleton(new ComponentFamilyMeta("test", emptyList(), "noicon", "test", "test") {

                            {
                                final ParameterMeta store = new ParameterMeta(null, DataStore1.class,
                                        ParameterMeta.Type.OBJECT, "config.store", "store", new String[0], emptyList(),
                                        emptyList(), new HashMap<String, String>() {

                                            {
                                                put("tcomp::configurationtype::type", "datastore");
                                                put("tcomp::configurationtype::name", "testDatastore");
                                            }
                                        }, false);
                                final ParameterMeta wrapper = new ParameterMeta(null, WrappingStore.class,
                                        ParameterMeta.Type.OBJECT, "config", "config", new String[0],
                                        singletonList(store), emptyList(), emptyMap(), false);
                                getPartitionMappers()
                                        .put("test",
                                                new PartitionMapperMeta(this, "mapper", "noicon", 1,
                                                        PartitionMapper1.class, () -> singletonList(wrapper), m -> null,
                                                        () -> (a, b) -> null, true, Collections.emptyMap()) {
                                                });
                            }
                        }), new MigrationHandlerFactory(
                                new ReflectionService(new ParameterModelService(registry), registry)));
        final List<Config> configs =
                model.getFamilies().stream().flatMap(f -> f.getConfigs().get().stream()).collect(toList());
        assertEquals(1, configs.size());
    }

    @Test
    void test(@TempDir final Path temporaryFolder, final TestInfo testInfo) throws Exception {

        final String pluginName = testInfo.getTestMethod().get().getName() + ".jar";
        final File pluginJar = createChainPlugin(temporaryFolder.toFile(), pluginName);
        final String testLocation = temporaryFolder.getParent().getFileName().toString();

        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null)) {
            final String location = pluginJar.getAbsolutePath();
            manager.addPlugin(location);
            final Container pluginContainer =
                    manager.findPlugin(pluginName).orElseThrow(() -> new Exception("test plugin don't exist"));
            assertNotNull(pluginContainer);
            final RepositoryModel rm = pluginContainer.get(RepositoryModel.class);
            assertNotNull(rm);
            assertEquals(2, rm.getFamilies().size());
            final Family family =
                    rm.getFamilies().stream().filter(it -> it.getMeta().getName().equals("family1")).findFirst().get();
            final String ds1Id = IdGenerator.get("test", "family1", "datastore", "dataStore1");
            final Config dataStore1Config =
                    family.getConfigs().get().stream().filter(c -> c.getId().equals(ds1Id)).findFirst().get();
            assertNotNull(dataStore1Config);
            assertEquals(1, dataStore1Config.getChildConfigs().size());
            assertEquals("configuration", dataStore1Config.getChildConfigs().get(0).getMeta().getName());
            assertEquals("class org.talend.test.generated." + testLocation + ".DataSet1",
                    dataStore1Config.getChildConfigs().get(0).getMeta().getJavaType().toString());

            final String ds2Id = IdGenerator.get("test", "family1", "datastore", "dataStore2");
            final Config dataStore2Config =
                    family.getConfigs().get().stream().filter(c -> c.getId().equals(ds2Id)).findFirst().get();
            assertNotNull(dataStore2Config);
            assertEquals(1, dataStore2Config.getChildConfigs().size());
            assertEquals("configuration", dataStore2Config.getChildConfigs().get(0).getMeta().getName());
            assertEquals("class org.talend.test.generated." + testLocation + ".DataSet2",
                    dataStore2Config.getChildConfigs().get(0).getMeta().getJavaType().toString());
        }

    }

    private String toPackage(final String container) {
        return "org.talend.test.generated." + container.replace(".jar", "");
    }

    private File createChainPlugin(final File dir, final String plugin) {
        final File target = new File(dir, plugin);
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(target))) {
            final String packageName = toPackage(target.getParentFile().getParentFile().getName()).replace(".", "/");
            final String sourcePackage = "org/talend/test";
            final String fromPack = sourcePackage.replace('/', '.');
            final String toPack = packageName.replace('.', '/');
            final File root = new File(jarLocation(getClass()), sourcePackage);
            ofNullable(root.listFiles())
                    .map(Stream::of)
                    .orElseGet(Stream::empty)
                    .filter(c -> c.getName().endsWith(".class"))
                    .forEach(clazz -> {
                        try (final InputStream is = new FileInputStream(clazz)) {
                            final ClassReader reader = new ClassReader(is);
                            final ClassWriter writer = new ClassWriter(COMPUTE_FRAMES);
                            reader.accept(new ClassRemapper(writer, new Remapper() {

                                @Override
                                public String map(final String key) {
                                    return key.replace(sourcePackage, toPack).replace(fromPack, packageName);
                                }
                            }), EXPAND_FRAMES);
                            outputStream.putNextEntry(new JarEntry(toPack + '/' + clazz.getName()));
                            outputStream.write(writer.toByteArray());
                        } catch (final IOException e) {
                            fail(e.getMessage());
                        }
                    });
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return target;
    }

}
