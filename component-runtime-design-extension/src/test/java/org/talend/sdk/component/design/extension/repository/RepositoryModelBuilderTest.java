/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.design.extension.repository;

import org.apache.xbean.asm6.ClassReader;
import org.apache.xbean.asm6.ClassWriter;
import org.apache.xbean.asm6.commons.Remapper;
import org.apache.xbean.asm6.commons.RemappingClassAdapter;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.design.extension.RepositoryModel;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.util.IdGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static org.apache.xbean.asm6.ClassReader.EXPAND_FRAMES;
import static org.apache.xbean.asm6.ClassWriter.COMPUTE_FRAMES;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class RepositoryModelBuilderTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Rule
    public final TestName testName = new TestName();

    @Test
    public void test() throws Exception {

        final String pluginName = testName.getMethodName() + ".jar";
        final File pluginJar = createChainPlugin(TEMPORARY_FOLDER.getRoot(), pluginName);

        try (final ComponentManager manager =
                new ComponentManager(new File("target/fake-m2"), "TALEND-INF/dependencies.txt", null)) {
            manager.addPlugin(pluginJar.getAbsolutePath());
            Container pluginContainer =
                    manager.findPlugin(pluginName).orElseThrow(() -> new Exception("test plugin don't exist"));
            assertNotNull(pluginContainer);
            RepositoryModel rm = pluginContainer.get(RepositoryModel.class);
            assertNotNull(rm);
            assertEquals(1, rm.getFamilies().size());
            Family family = rm.getFamilies().get(0);
            String ds1Id = IdGenerator.get("family1", "datastore", "dataStore1");
            Config dataStore1Config =
                    family.getConfigs().stream().filter(c -> c.getId().equals(ds1Id)).findFirst().get();
            assertNotNull(dataStore1Config);
            assertEquals(1, dataStore1Config.getChildConfigs().size());
            assertEquals("configuration1", dataStore1Config.getChildConfigs().get(0).getMeta().getName());

            String ds2Id = IdGenerator.get("family1", "datastore", "dataStore2");
            Config dataStore2Config =
                    family.getConfigs().stream().filter(c -> c.getId().equals(ds2Id)).findFirst().get();
            assertNotNull(dataStore2Config);
            assertEquals(1, dataStore2Config.getChildConfigs().size());
            assertEquals("configuration2", dataStore2Config.getChildConfigs().get(0).getMeta().getName());
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
                            reader.accept(new RemappingClassAdapter(writer, new Remapper() {

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
