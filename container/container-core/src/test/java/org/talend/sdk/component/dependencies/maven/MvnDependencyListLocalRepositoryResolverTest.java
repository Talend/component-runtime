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
package org.talend.sdk.component.dependencies.maven;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.test.dependencies.DependenciesTxtBuilder;

class MvnDependencyListLocalRepositoryResolverTest {

    @Test
    void nestedDependency(@TempDir final File temporaryFolder) throws IOException {
        final File file = new File(temporaryFolder, UUID.randomUUID().toString() + ".jar");
        file.getParentFile().mkdirs();
        try (final JarOutputStream enclosing = new JarOutputStream(new FileOutputStream(file))) {
            enclosing.putNextEntry(new ZipEntry("MAVEN-INF/repository/foo/bar/dummy/1.0.0/dummy-1.0.0.jar"));
            try (final JarOutputStream nested = new JarOutputStream(enclosing)) {
                nested.putNextEntry(new ZipEntry("TALEND-INF/dependencies.txt"));
                nested
                        .write(new DependenciesTxtBuilder()
                                .withDependency("org.apache.tomee:ziplock:jar:8.0.14:runtime")
                                .withDependency("org.apache.tomee:javaee-api:jar:7.0-1:compile")
                                .build()
                                .getBytes(StandardCharsets.UTF_8));
            }
        }

        try (final URLClassLoader tempLoader =
                new URLClassLoader(new URL[] { file.toURI().toURL() }, getSystemClassLoader())) {
            final List<String> toResolve =
                    new MvnDependencyListLocalRepositoryResolver("TALEND-INF/dependencies.txt", d -> null)
                            .resolve(tempLoader, "MAVEN-INF/repository/foo/bar/dummy/1.0.0/dummy-1.0.0.jar")
                            .map(Artifact::toPath)
                            .collect(toList());
            assertEquals(asList("org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar",
                    "org/apache/tomee/javaee-api/7.0-1/javaee-api-7.0-1.jar"), toResolve);
        }
    }

    @Test
    void nestedDependencyWithJira(@TempDir final File temporaryFolder) throws IOException {
        final File file = new File(temporaryFolder, UUID.randomUUID().toString() + ".jar");
        file.getParentFile().mkdirs();
        try (final JarOutputStream enclosing = new JarOutputStream(new FileOutputStream(file))) {
            enclosing.putNextEntry(new ZipEntry("BOOT-INF/lib/dummy-1.0.0-TCOMP-2285.jar"));
            try (final JarOutputStream nested = new JarOutputStream(enclosing)) {
                nested.putNextEntry(new ZipEntry("TALEND-INF/dependencies.txt"));
                nested
                        .write(new DependenciesTxtBuilder()
                                .withDependency("org.apache.tomee:ziplock:jar:8.0.14:runtime")
                                .withDependency("org.apache.tomee:javaee-api:jar:7.0-1:compile")
                                .build()
                                .getBytes(StandardCharsets.UTF_8));
            }
        }

        try (final URLClassLoader tempLoader =
                new URLClassLoader(new URL[] { file.toURI().toURL() }, getSystemClassLoader());
                final ConfigurableClassLoader ccl = new ConfigurableClassLoader("test",
                        new URL[] {}, getSystemClassLoader(), name -> true, name -> true,
                        new String[] {}, new String[0])) {
            final List<String> toResolve =
                    new MvnDependencyListLocalRepositoryResolver("TALEND-INF/dependencies.txt", d -> null)
                            .resolve(tempLoader, "BOOT-INF/lib/dummy-1.0.0-TCOMP-2285.jar")
                            .map(Artifact::toPath)
                            .collect(toList());
            assertEquals(asList("org/apache/tomee/ziplock/8.0.14/ziplock-8.0.14.jar",
                    "org/apache/tomee/javaee-api/7.0-1/javaee-api-7.0-1.jar"), toResolve);
        }
    }
}
