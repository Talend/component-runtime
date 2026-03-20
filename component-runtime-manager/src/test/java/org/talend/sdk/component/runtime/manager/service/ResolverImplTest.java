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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.api.service.dependency.ClassLoaderDefinition;
import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.path.PathFactory;

class ResolverImplTest {

    public final String ARTIFACT_ID = "artifactId";

    public final String EXPECTED_ARTIFACT_ID = "arquillian-tomee-codi-tests";

    public final String GAV_CODI_TESTS = "org.apache.tomee:arquillian-tomee-codi-tests:jar:8.0.9";

    public final String GAV_OPENEJB = "org.apache.tomee:openejb-itests-beans:jar:8.0.14";

    public final String POM_PROPS_TOMEE = "META-INF/maven/org.apache.tomee/arquillian-tomee-codi-tests/pom.properties";

    public final String CLASS_OPENEJB = "org.apache.openejb.test.ApplicationException";

    public final String CLASS_POIXML = "org.apache.poi.ooxml.POIXMLException";

    public final String POI_PATH = "target/test-dependencies/org/apache/poi/poi-ooxml/5.4.1/poi-ooxml-5.4.1.jar";

    @Test
    void createClassLoader(@TempDir final Path temporaryFolder) throws Exception {
        final File root = temporaryFolder.toFile();
        root.mkdirs();
        final String dep = GAV_CODI_TESTS;
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
            try (final InputStream in = desc.asClassLoader().getResourceAsStream(POM_PROPS_TOMEE)) {
                assertNotNull(in);
                props.load(in);
            }
            assertEquals(EXPECTED_ARTIFACT_ID, props.getProperty(ARTIFACT_ID));
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

    @Test
    void createBareConfigurableClassLoader(@TempDir final Path temporaryFolder) throws Exception {
        createConfigurableClassLoader(temporaryFolder, true, false);
    }

    @Test
    void createConfigurableClassLoaderWithParentLoading(@TempDir final Path temporaryFolder) throws Exception {
        createConfigurableClassLoader(temporaryFolder, false, true);
    }

    @Test
    void createConfigurableClassLoaderWithoutParentLoading(@TempDir final Path temporaryFolder) throws Exception {
        createConfigurableClassLoader(temporaryFolder, false, false);
    }

    private void createConfigurableClassLoader(final Path temporaryFolder, final boolean bare,
            final boolean parentLoading)
            throws Exception {
        final File root = temporaryFolder.toFile();
        root.mkdirs();
        final List<String> deps = Arrays.asList(GAV_CODI_TESTS, GAV_OPENEJB);
        final File nestedJar = new File(root, UUID.randomUUID().toString() + ".jar");
        try (final JarOutputStream out = new JarOutputStream(new FileOutputStream(nestedJar))) {
            deps.forEach(d -> addDepToJar(d, out));
        }

        final Thread thread = Thread.currentThread();
        final ClassLoader contextCl = thread.getContextClassLoader();
        // component loader simulation which is always the parent of that
        // here the parent of the component is the jar containing the nested repo
        final URLClassLoader appLoader = new URLClassLoader(new URL[] { nestedJar.toURI().toURL() }, contextCl);
        final File poiJar = new File(POI_PATH);
        assertTrue(Files.exists(poiJar.toPath()));
        final ConfigurableClassLoader componentLoader =
                new ConfigurableClassLoader("test", new URL[] { poiJar.toURI().toURL() }, appLoader,
                        it -> true, it -> true, new String[0], new String[0]);
        // force loading of a class from the future parent for the created classloader with no parent loading allowed.
        final Class poiClazz = componentLoader.loadClass(CLASS_POIXML, true);
        assertEquals(componentLoader, poiClazz.getClassLoader());

        thread.setContextClassLoader(componentLoader);
        final ClassLoaderDefinition classLoaderDefinition = ContainerManager.ClassLoaderConfiguration.builder()
                .parent(componentLoader)
                .classesFilter(it -> true)
                .parentClassesFilter(it -> parentLoading)
                .supportsResourceDependencies(true)
                .parentResourcesFilter(it -> true)
                .create();

        final Function<String, Path> fileResolver = coord -> PathFactory.get("maven2").resolve(coord);
        try (final Resolver.ClassLoaderDescriptor desc = bare
                ? new ResolverImpl(null, fileResolver).mapDescriptorToClassLoader(deps)
                : new ResolverImpl(null, fileResolver).mapDescriptorToClassLoader(deps, classLoaderDefinition)) {
            assertNotNull(desc);
            final ClassLoader dumbCl = desc.asClassLoader();
            assertNotNull(dumbCl);
            assertTrue(dumbCl.getParent() == (bare ? appLoader : componentLoader));
            // the classloader should be a child of the component loader and a ConfigurableClassLoader
            assertTrue(dumbCl instanceof ConfigurableClassLoader);
            final ConfigurableClassLoader ccl = (ConfigurableClassLoader) dumbCl;
            // class loading should work and be done by the created classloader
            Class clazz = ccl.loadClass(CLASS_OPENEJB, true);
            assertNotNull(clazz);
            assertEquals(ccl, clazz.getClassLoader());
            // check that parent loading is or not allowed
            if (parentLoading) {
                Class clzz = ccl.loadClass(CLASS_POIXML, true);
                assertNotNull(clzz);
                assertEquals(poiClazz, clzz);
            } else {
                assertThrows(ClassNotFoundException.class, () -> ccl.loadClass(CLASS_POIXML, true));
            }
            // checks dependencies and resources
            assertEquals(deps, desc.resolvedDependencies());
            final Properties props = new Properties();
            try (final InputStream in = desc.asClassLoader().getResourceAsStream(POM_PROPS_TOMEE)) {
                assertNotNull(in);
                props.load(in);
            }
            assertEquals(EXPECTED_ARTIFACT_ID, props.getProperty(ARTIFACT_ID));
            //
            // TODO: check parent loading of resources is allowed or not depending on the configuration.
            //
        } finally {
            thread.setContextClassLoader(contextCl);
            appLoader.close(); // cascade close the classloaders
        }
    }

    private void addDepToJar(final String dep, final JarOutputStream out) {
        final String[] segments = dep.split(":");
        final String path = "MAVEN-INF/repository/" + segments[0].replace(".", "/") + "/" + segments[1] + "/"
                + segments[3] + "/" + segments[1] + "-" + segments[3] + "." + segments[2];

        // create folders for this m2 embedded deps
        final String[] subPaths = path.split("/");
        final StringBuilder current = new StringBuilder();
        for (int i = 0; i < subPaths.length - 1; i++) {
            current.append(subPaths[i]).append("/");
            try {
                out.putNextEntry(new ZipEntry(current.toString()));
            } catch (final IOException e) {
                if (!e.getMessage().contains("duplicate entry")) {
                    fail(e.getMessage());
                }
            }
        }
        // add the dep
        final File jar = new File("target/test-dependencies", path.substring("MAVEN-INF/repository/".length()));
        try {
            out.putNextEntry(new ZipEntry(path));
            Files.copy(jar.toPath(), out);
        } catch (final IOException e) {
            fail(e.getMessage());
        }
    }
}
