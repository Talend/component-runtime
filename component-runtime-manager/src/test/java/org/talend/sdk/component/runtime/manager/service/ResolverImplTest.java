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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.path.PathFactory;

class ResolverImplTest {

    @Test
    void createClassLoader(@TempDir final Path temporaryFolder) throws Exception {
        final File root = temporaryFolder.toFile();
        root.mkdirs();
        final String dep = "org.apache.tomee:arquillian-tomee-codi-tests:jar:8.0.9";
        final File nestedJar = new File(root, UUID.randomUUID().toString() + ".jar");
        try (final JarOutputStream out = new JarOutputStream(new FileOutputStream(nestedJar))) {
            addDepToJar(dep, out);
        }

        final Thread thread = Thread.currentThread();
        final ClassLoader contextClassLoader = thread.getContextClassLoader();
        // component loader simulation which is always the parent of that
        // here the parent of the component is the jar containing the nested repo
        final URLClassLoader appLoader =
                new URLClassLoader(new URL[] { nestedJar.toURI().toURL() }, contextClassLoader);
        final ConfigurableClassLoader componentLoader = new ConfigurableClassLoader("test", new URL[0], appLoader,
                it -> true, it -> false, new String[0], new String[0]);
        thread.setContextClassLoader(componentLoader);
        try (final Resolver.ClassLoaderDescriptor desc =
                new ResolverImpl(null, coord -> PathFactory.get("maven2").resolve(coord))
                        .mapDescriptorToClassLoader(singletonList(dep))) {
            assertNotNull(desc);
            assertNotNull(desc.asClassLoader());
            assertEquals(singletonList(dep), desc.resolvedDependencies());
            final Properties props = new Properties();
            try (final InputStream in = desc
                    .asClassLoader()
                    .getResourceAsStream(
                            "META-INF/maven/org.apache.tomee/arquillian-tomee-codi-tests/pom.properties")) {
                assertNotNull(in);
                props.load(in);
            }
            assertEquals("arquillian-tomee-codi-tests", props.getProperty("artifactId"));
        } finally {
            thread.setContextClassLoader(contextClassLoader);
            appLoader.close();
        }
    }

    @Test
    void resolvefromDescriptor() throws IOException {
        try (final InputStream stream =
                new ByteArrayInputStream("The following files have been resolved:\njunit:junit:jar:4.12:compile"
                        .getBytes(StandardCharsets.UTF_8))) {
            final Collection<File> deps = new ResolverImpl(null, coord -> PathFactory.get("maven2").resolve(coord))
                    .resolveFromDescriptor(stream);
            assertEquals(1, deps.size());
            assertEquals("maven2" + File.separator + "junit" + File.separator + "junit" + File.separator + "4.12"
                    + File.separator + "junit-4.12.jar", deps.iterator().next().getPath());
        }
    }

    private void addDepToJar(final String dep, final JarOutputStream out) {
        final String[] segments = dep.split(":");
        final String path = segments[0].replace(".", "/") + "/" + segments[1] + "/"
                + segments[3] + "/" + segments[1] + "-" + segments[3] + "." + segments[2];

        // create folders for this m2 embedded deps
        final String[] subPaths = path.split("/");
        final StringBuilder current = new StringBuilder();
        for (int i = 0; i < subPaths.length - 1; i++) {
            current.append(subPaths[i]).append("/");
            try {
                out.putNextEntry(new ZipEntry(current.toString()));
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        }
        // add the dep
        final File jar = new File("target/test-dependencies", path);
        try {
            out.putNextEntry(new ZipEntry(path));
            Files.copy(jar.toPath(), out);
        } catch (final IOException e) {
            fail(e.getMessage());
        }
    }
}
