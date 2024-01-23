/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam;

import static java.util.Optional.ofNullable;
import static org.apache.xbean.asm9.ClassReader.EXPAND_FRAMES;
import static org.apache.xbean.asm9.ClassWriter.COMPUTE_FRAMES;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.commons.ClassRemapper;
import org.apache.xbean.asm9.commons.Remapper;

public class PluginGenerator {

    public String toPackage(final String container) {
        return "org.talend.test.generated." + container.replace(".jar", "");
    }

    public File createChainPlugin(final File dir, final String plugin) {
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
