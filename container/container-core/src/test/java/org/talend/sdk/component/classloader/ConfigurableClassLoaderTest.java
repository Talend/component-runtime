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
package org.talend.sdk.component.classloader;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.spi.FileSystemProvider;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;

import javax.xml.stream.XMLOutputFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestEngine;
import org.talend.sdk.component.test.Constants;
import org.talend.sdk.component.test.dependencies.DependenciesTxtBuilder;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

class ConfigurableClassLoaderTest {

    @Test
    void packageDefinition() {
        final ClassLoader parent = ConfigurableClassLoaderTest.class.getClassLoader();
        try (final ConfigurableClassLoader loader =
                new ConfigurableClassLoader("",
                        new URL[] { new File(Constants.DEPENDENCIES_LOCATION,
                                "org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar").toURI().toURL() },
                        parent, name -> true, name -> false, null, new String[0])) {
            final Package pck = loader.loadClass("org.apache.ziplock.JarLocation").getPackage();
            assertNotNull(pck);
            assertEquals("org.apache.ziplock", pck.getName());
            assertEquals(pck, loader.loadClass("org.apache.ziplock.Archive").getPackage());
        } catch (final IOException | ClassNotFoundException e) {
            fail(e.getMessage(), e);
        }
    }

    @Test
    void childLoading() {
        Stream.of(true, false).forEach(parentFirst -> {
            final ClassLoader parent = ConfigurableClassLoaderTest.class.getClassLoader();
            try (final ConfigurableClassLoader loader = new ConfigurableClassLoader("",
                    new URL[] { new File(Constants.DEPENDENCIES_LOCATION,
                            "org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar").toURI().toURL() },
                    parent, name -> true, name -> parentFirst, null, new String[0])) {
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
    void parentFiltering() {
        final ClassLoader parent = ConfigurableClassLoaderTest.class.getClassLoader();
        try (final ConfigurableClassLoader loader = new ConfigurableClassLoader("", new URL[0], parent,
                name -> !ConfigurableClassLoaderTest.class.getName().equals(name), name -> true, null, new String[0])) {
            try {
                loader.loadClass("org.talend.sdk.component.classloader.ConfigurableClassLoaderTest");
            } catch (final ClassNotFoundException e) {
                fail("ConfigurableClassLoaderTest should find this class");
            }
            try {
                parent.loadClass("org.talend.sdk.component.classloader.ConfigurableClassLoaderTest");
            } catch (final ClassNotFoundException e) {
                fail("Parent should be able to load ConfigurableClassLoaderTest otherwise this test is pointless");
            }
        } catch (final IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void nestedPackageDefinition(@TempDir final File temporaryFolder) throws Exception {
        final File nestedJar = createNestedJar(temporaryFolder, "org.apache.tomee:ziplock:jar:8.0.14");
        try (final URLClassLoader parent = new URLClassLoader(new URL[] { nestedJar.toURI().toURL() },
                Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader =
                        new ConfigurableClassLoader("", new URL[0], parent, name -> true, name -> true,
                                new String[] { "org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar" }, new String[0])) {
            final Package pck = loader.loadClass("org.apache.ziplock.JarLocation").getPackage();
            assertNotNull(pck);
            assertEquals("org.apache.ziplock", pck.getName());
            assertEquals(pck, loader.loadClass("org.apache.ziplock.Archive").getPackage());
        } finally {
            if (!nestedJar.delete()) {
                nestedJar.deleteOnExit();
            }
        }
    }

    @Test
    void findContainedResources(@TempDir final File temporaryFolder) throws Exception {
        final File nestedJar = createNestedJar(temporaryFolder, "org.apache.tomee:ziplock:jar:8.0.14");
        try (final URLClassLoader parent = new URLClassLoader(new URL[] { nestedJar.toURI().toURL() },
                Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader =
                        new ConfigurableClassLoader("", new URL[0], parent, name -> true, name -> true,
                                new String[] { "org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar" }, new String[0])) {
            final List<InputStream> containedResources =
                    loader.findContainedResources("org/apache/ziplock/ClassLoaders.class");
            containedResources.forEach(it -> {
                try {
                    it.close();
                } catch (final IOException e) {
                    fail(e.getMessage());
                }
            });
            assertEquals(1, containedResources.size());
        } finally {
            if (!nestedJar.delete()) {
                nestedJar.deleteOnExit();
            }
        }
    }

    @Test
    void nestedJars(@TempDir final File temporaryFolder) throws IOException {
        final File nestedJar = createNestedJar(temporaryFolder, "org.apache.tomee:ziplock:jar:8.0.14");
        try (final URLClassLoader parent = new URLClassLoader(new URL[] { nestedJar.toURI().toURL() },
                Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader =
                        new ConfigurableClassLoader("", new URL[0], parent, name -> true, name -> true,
                                new String[] { "org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar" }, new String[0])) {
            try { // classes
                final Class<?> aClass = loader.loadClass("org.apache.ziplock.JarLocation");
                final Object jarLocation =
                        aClass.getMethod("jarLocation", Class.class).invoke(null, ConfigurableClassLoaderTest.class);
                assertNotNull(jarLocation);
                assertTrue(File.class.isInstance(jarLocation));
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
                assertEquals("org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar!/org/apache/ziplock/JarLocation.class",
                        url.getFile());
                final byte[] bytes = slurp(url.openStream());
                assertEquals(4666, bytes.length, mavenJarSizeMargin);
            }
            { // direct as stream
                assertEquals(4666, slurp(loader.getResourceAsStream(resource)).length, mavenJarSizeMargin);
            }
            { // in collection
                final Enumeration<URL> resources = loader.getResources(resource);
                assertTrue(resources.hasMoreElements());
                assertEquals(4666, slurp(resources.nextElement().openStream()).length, mavenJarSizeMargin);
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
    void noNestedJarsMissingResources() throws IOException {
        try (final URLClassLoader parent =
                new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader = new ConfigurableClassLoader("", new URL[0], parent, name -> true,
                        name -> true, new String[0], new String[0])) {

            { // missing
                assertNull(loader.getResource("a.missing"));
                assertNull(loader.getResourceAsStream("b.missing"));
            }
        }
    }

    @Test
    void getResourceAsStreamChildFirst(@TempDir final File temporaryFolder) throws IOException {
        final File jar = new File(temporaryFolder, "getResourceAsStreamChildFirst.jar");
        temporaryFolder.mkdirs();
        try (final JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(jar))) {
            outputStream.putNextEntry(new JarEntry("found.in.child.and.parent"));
            outputStream.write("child".getBytes(StandardCharsets.UTF_8));
            outputStream.closeEntry();
        }
        try (final URLClassLoader parent =
                new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader =
                        new ConfigurableClassLoader("", new URL[] { jar.toURI().toURL() }, parent, name -> true,
                                name -> true, new String[0], new String[0])) {

            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(loader.getResourceAsStream("found.in.child.and.parent")))) {
                assertEquals("child", reader.lines().collect(joining()).trim());
            }
        }
    }

    @Test
    void spi() throws IOException {
        final Predicate<String> parentClasses = name -> true;
        final File staxApi =
                new File(Constants.DEPENDENCIES_LOCATION, "org/codehaus/woodstox/stax2-api/4.2.1/stax2-api-4.2.1.jar");
        assertTrue(staxApi.isFile());
        final File woodstox = new File(Constants.DEPENDENCIES_LOCATION,
                "com/fasterxml/woodstox/woodstox-core/6.5.0/woodstox-core-6.5.0.jar");
        assertTrue(woodstox.isFile());
        try (final URLClassLoader parent =
                new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader = new ConfigurableClassLoader("test",
                        new URL[] { woodstox.toURI().toURL(), staxApi.toURI().toURL() }, parent, parentClasses,
                        parentClasses.negate(), new String[0], new String[0])) {

            final List<XMLOutputFactory> factories = StreamSupport
                    .stream(ServiceLoader.load(XMLOutputFactory.class, loader).spliterator(), false)
                    .collect(toList());
            assertEquals(1, factories.size());
            final Class<? extends XMLOutputFactory> firstClass = factories.iterator().next().getClass();
            assertEquals("com.ctc.wstx.stax.WstxOutputFactory", firstClass.getName());
            assertEquals(loader, firstClass.getClassLoader());
            assertNull(XMLOutputFactory.class.getClassLoader());
            assertTrue(XMLOutputFactory.class.isAssignableFrom(firstClass));
        }
    }

    @Test
    void jvmOnlyInParentSpi() throws IOException {
        final Predicate<String> parentClasses = name -> true;
        try (final URLClassLoader parent =
                new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader = new ConfigurableClassLoader("test", new URL[0], parent,
                        parentClasses, parentClasses.negate(), new String[0], new String[] {
                                new File(System.getProperty("java.home")).toPath().toAbsolutePath().toString() })) {

            // can be loaded cause in the JVM
            assertTrue(ServiceLoader.load(FileSystemProvider.class, loader).iterator().hasNext());

            // this is in the (test) classloader but not available to the classloader
            final List<TestEngine> junitEngines = StreamSupport
                    .stream(ServiceLoader.load(TestEngine.class, loader).spliterator(), false)
                    .collect(toList());
            assertTrue(junitEngines.isEmpty());
        }
    }

    @Test
    void excludedSpiResources() throws Exception {
        final Predicate<String> parentClasses = name -> true;
        final File xerces = new File(Constants.DEPENDENCIES_LOCATION, "xerces/xercesImpl/2.12.2/xercesImpl-2.12.2.jar");
        assertTrue(xerces.exists());
        try (final URLClassLoader parent =
                new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
                final ConfigurableClassLoader loader = new ConfigurableClassLoader("test",
                        new URL[] { xerces.toURI().toURL() }, parent, parentClasses, parentClasses.negate(),
                        new String[0], new String[] {
                                new File(System.getProperty("java.home")).toPath().toAbsolutePath().toString() })) {

            final Thread thread = Thread.currentThread();
            final ClassLoader old = thread.getContextClassLoader();
            thread.setContextClassLoader(loader);
            try {
                assertXmlReader();
            } finally {
                thread.setContextClassLoader(old);
            }
            assertXmlReader();
        }
    }

    @Test
    void cacheableClasses(@TempDir final File temporaryFolder) throws Exception {
        System.setProperty("talend.tccl.cacheable.classes", "org.apache.ziplock.JarLocation,com.ctc.wstx.stax");
        final ClassLoader parent = ConfigurableClassLoaderTest.class.getClassLoader();
        try (final ConfigurableClassLoader loader =
                new ConfigurableClassLoader("",
                        new URL[] { new File(Constants.DEPENDENCIES_LOCATION,
                                "org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar").toURI().toURL() },
                        parent, name -> true, name -> false, null, new String[0])) {
            final Package pck = loader.loadClass("org.apache.ziplock.JarLocation").getPackage();
            assertNotNull(pck);
            assertEquals("org.apache.ziplock", pck.getName());
            assertEquals(pck, loader.loadClass("org.apache.ziplock.Archive").getPackage());
            assertEquals(asList("org/apache/ziplock/JarLocation", "com/ctc/wstx/stax"), loader.getCacheableClasses());
        } catch (final IOException | ClassNotFoundException e) {
            fail(e.getMessage(), e);
        }
    }

    private void assertXmlReader() throws SAXException {
        final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        final Class<? extends XMLReader> clazz = xmlReader.getClass();
        assertNotEquals("org.apache.xerces.parsers.SAXParser", clazz.getName());
        assertTrue(asList(getSystemClassLoader(), null).contains(clazz.getClassLoader()));
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
    private File createNestedJar(final File temporaryFolder, final String... deps) throws IOException {
        final File tmp = new File(temporaryFolder, UUID.randomUUID().toString() + ".jar");
        tmp.getParentFile().mkdirs();
        try (final JarOutputStream out = new JarOutputStream(new FileOutputStream(tmp))) {
            {
                out.putNextEntry(new ZipEntry(Constants.DEPENDENCIES_LIST_RESOURCE_PATH));
                final DependenciesTxtBuilder dependenciesTxtBuilder = new DependenciesTxtBuilder();
                Stream.of(deps).forEach(dependenciesTxtBuilder::withDependency);
                out.write(dependenciesTxtBuilder.build().getBytes(StandardCharsets.UTF_8));
            }

            Stream.of(deps).forEach(s -> {
                final String[] segments = s.split(":");
                final String path = segments[0].replace(".", "/") + "/" + segments[1] + "/"
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
                    final File jar = new File(Constants.DEPENDENCIES_LOCATION, path);
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
