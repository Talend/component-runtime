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

import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static org.apache.xbean.asm9.ClassReader.EXPAND_FRAMES;
import static org.apache.xbean.asm9.ClassWriter.COMPUTE_FRAMES;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import org.apache.xbean.asm9.ClassReader;
import org.apache.xbean.asm9.ClassWriter;
import org.apache.xbean.asm9.commons.ClassRemapper;
import org.apache.xbean.asm9.commons.Remapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.spi.component.GenericComponentExtension;
import org.talend.test.generic.MyGenericImpl;
import sun.net.www.protocol.jar.Handler;

class GenericComponentExtensionSupportTest {

    public static class CustomURLStreamHandler extends Handler {

        @Override
        protected URLConnection openConnection(final URL u) throws IOException {
            URLConnection connection = super.openConnection(u);
            connection.setUseCaches(false);
            return connection;
        }
    }

    public static class CustomURLStreamHandlerFactory implements URLStreamHandlerFactory {

        @Override
        public URLStreamHandler createURLStreamHandler(String protocol) {
            if ("jar".equals(protocol)) {
                return new CustomURLStreamHandler();
            }
            return null;
        }

    }

    @BeforeAll
    public static void setup() {
        URL.setURLStreamHandlerFactory(new CustomURLStreamHandlerFactory());
    }

    @Test
    void run(@TempDir final Path path) throws IOException {
        final Path pluginFolder = path.resolve("test-plugins_" + UUID.randomUUID().toString());
        Files.createDirectories(pluginFolder);

        final Path target = pluginFolder.resolve("test-compo.jar");
        try (final JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(target))) {
            final String packageName = "org.talend.generated.test." + getClass().getSimpleName();
            final String sourcePackage = "org/talend/test/generic";
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
                            outputStream.closeEntry();
                        } catch (final IOException e) {
                            fail(e.getMessage());
                        }
                    });
            outputStream.putNextEntry(new JarEntry("META-INF/services/" + GenericComponentExtension.class.getName()));
            outputStream
                    .write((packageName + '.' + MyGenericImpl.class.getSimpleName()).getBytes(StandardCharsets.UTF_8));
            outputStream.closeEntry();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        try (final ComponentManager manager =
                new ComponentManager(new File("target/test-dependencies"), "META-INF/test/dependencies", null)) {
            manager.addPlugin(target.toAbsolutePath().toString());
            final Mapper mapper = manager
                    .findMapper("my-generic", "the-first", 1, singletonMap("a", "a->1"))
                    .orElseThrow(IllegalStateException::new);
            final Input input = mapper.create(); // we bypass a bit the lifecycle cause we know the test component we
                                                 // use
            final Record next = Record.class.cast(input.next());
            assertEquals("my-generic", next.getString("plugin"));
            assertEquals("the-first", next.getString("name"));
            assertEquals("a", next.getString("key"));
            assertEquals("a->1", next.getString("value"));
            assertNull(input.next());
        }
    }
}
