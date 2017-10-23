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
package org.talend.components.classloader;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.UUID;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.talend.components.test.Constants;
import org.talend.components.test.dependencies.DependenciesTxtBuilder;

public class ConfigurableClassLoaderTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Test
    public void childLoading() throws IOException {
        Stream.of(true, false).forEach(parentFirst -> {
            final ClassLoader parent = ConfigurableClassLoaderTest.class.getClassLoader();
            try (final ConfigurableClassLoader loader = new ConfigurableClassLoader(
                    new URL[] { new File(Constants.DEPENDENCIES_LOCATION, "org/apache/tomee/ziplock/7.0.3/ziplock-7.0.3.jar")
                            .toURI().toURL() },
                    parent, name -> true, name -> parentFirst, null)) {
                try {
                    loader.loadClass("org.apache.ziplock.JarLocation");
                } catch (final ClassNotFoundException e) {
                    fail("JarLocation can't be found: " + e.getMessage());
                }
                try {
                    parent.loadClass("org.apache.ziplock.JarLocation");
                    fail("Parent shouldn't be able to load JarLocation otherwise this test is pointless");
                } catch (final ClassNotFoundException e) {
                    // ok
                }
            } catch (final IOException e) {
                fail(e.getMessage());
            }
        });
    }

    @Test
    public void parentFiltering() throws IOException {
        final ClassLoader parent = ConfigurableClassLoaderTest.class.getClassLoader();
        try (final ConfigurableClassLoader loader = new ConfigurableClassLoader(new URL[0], parent,
                name -> !ConfigurableClassLoaderTest.class.getName().equals(name), name -> true, null)) {
            try {
                loader.loadClass("org.talend.components.classloader.ConfigurableClassLoaderTest");
                fail("ConfigurableClassLoaderTest shouldn't pass to parent");
            } catch (final ClassNotFoundException e) {
                // filtered so we are good
            }
            try {
                parent.loadClass("org.talend.components.classloader.ConfigurableClassLoaderTest");
            } catch (final ClassNotFoundException e) {
                fail("Parent should be able to load ConfigurableClassLoaderTest otherwise this test is pointless");
            }
        } catch (final IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void nestedJars() throws IOException {
        final File nestedJar = createNestedJar("org.apache.tomee:ziplock:jar:7.0.3");
        try (final URLClassLoader parent = new URLClassLoader(new URL[] { nestedJar.toURI().toURL() },
                Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader = new ConfigurableClassLoader(new URL[0], parent, name -> true, name -> true,
                        new String[] { "org/apache/tomee/ziplock/7.0.3/ziplock-7.0.3.jar" })) {
            try { // classes
                final Class<?> aClass = loader.loadClass("org.apache.ziplock.JarLocation");
                final Object jarLocation = aClass.getMethod("jarLocation", Class.class).invoke(null,
                        ConfigurableClassLoaderTest.class);
                assertNotNull(jarLocation);
                assertThat(jarLocation, instanceOf(File.class));
            } catch (final Exception e) {
                fail("JarLocation should be loaded from the nested jar");
            }

            // resources
            final String resource = "org/apache/ziplock/JarLocation.class";
            final int mavenJarSizeMargin = 256;
            { // through URL
                final URL url = loader.getResource(resource);
                assertNotNull(url);
                assertEquals("nested", url.getProtocol());
                assertEquals(
                        "MAVEN-INF/repository/org/apache/tomee/ziplock/7.0.3/ziplock-7.0.3.jar!/org/apache/ziplock/JarLocation.class",
                        url.getFile());
                final byte[] bytes = slurp(url.openStream());
                assertEquals(4394, bytes.length, mavenJarSizeMargin);
            }
            { // direct as stream
                assertEquals(4394, slurp(loader.getResourceAsStream(resource)).length, mavenJarSizeMargin);
            }
            { // in collection
                final Enumeration<URL> resources = loader.getResources(resource);
                assertTrue(resources.hasMoreElements());
                assertEquals(4394, slurp(resources.nextElement().openStream()).length, mavenJarSizeMargin);
            }
            { // missing
                assertNull(loader.getResource(resource + ".missing"));
                assertNull(loader.getResourceAsStream(resource + ".missing"));
            }
        } finally {
            if (!nestedJar.delete()) {
                nestedJar.deleteOnExit();
            }
        }
    }

    @Test
    public void noNestedJarsMissingResources() throws IOException {
        try (final URLClassLoader parent = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader = new ConfigurableClassLoader(new URL[0], parent, name -> true, name -> true,
                        new String[0])) {

            { // missing
                assertNull(loader.getResource("a.missing"));
                assertNull(loader.getResourceAsStream("b.missing"));
            }
        }
    }

    private byte[] slurp(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (final InputStream stream = inputStream) {
            int read;
            byte[] buffer = new byte[1024];
            while ((read = stream.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        }
        return out.toByteArray();
    }

    // super light packaging of a nested jar, this is 100% for test purposes
    private File createNestedJar(final String... deps) throws IOException {
        final File tmp = new File(TEMPORARY_FOLDER.getRoot(), UUID.randomUUID().toString() + ".jar");
        try (final JarOutputStream out = new JarOutputStream(new FileOutputStream(tmp))) {
            {
                out.putNextEntry(new ZipEntry(Constants.DEPENDENCIES_LIST_RESOURCE_PATH));
                final DependenciesTxtBuilder dependenciesTxtBuilder = new DependenciesTxtBuilder();
                Stream.of(deps).forEach(dependenciesTxtBuilder::withDependency);
                out.write(dependenciesTxtBuilder.build().getBytes(StandardCharsets.UTF_8));
            }

            Stream.of(deps).forEach(s -> {
                final String[] segments = s.split(":");
                final String path = "MAVEN-INF/repository/" + segments[0].replace(".", "/") + "/" + segments[1] + "/"
                        + segments[3] + "/" + segments[1] + "-" + segments[3] + "." + segments[2];

                { // create folders for this m2 embedded deps
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
                }
                { // add the dep
                    final File jar = new File(Constants.DEPENDENCIES_LOCATION, path.substring("MAVEN-INF/repository/".length()));
                    try {
                        out.putNextEntry(new ZipEntry(path));
                        Files.copy(jar.toPath(), out);
                    } catch (final IOException e) {
                        fail(e.getMessage());
                    }
                }
            });
        }
        return tmp;
    }
}
