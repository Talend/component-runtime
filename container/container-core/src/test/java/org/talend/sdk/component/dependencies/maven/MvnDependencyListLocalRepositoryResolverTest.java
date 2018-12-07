/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
import org.talend.sdk.component.junit.base.junit5.TemporaryFolder;
import org.talend.sdk.component.junit.base.junit5.WithTemporaryFolder;
import org.talend.sdk.component.test.dependencies.DependenciesTxtBuilder;

@WithTemporaryFolder
class MvnDependencyListLocalRepositoryResolverTest {

    @Test
    void nestedDependency(final TemporaryFolder temporaryFolder) throws IOException {
        final File file = temporaryFolder.newFile(UUID.randomUUID().toString() + ".jar");
        try (final JarOutputStream enclosing = new JarOutputStream(new FileOutputStream(file))) {
            enclosing.putNextEntry(new ZipEntry("MAVEN-INF/repository/foo/bar/dummy/1.0.0/dummy-1.0.0.jar"));
            try (final JarOutputStream nested = new JarOutputStream(enclosing)) {
                nested.putNextEntry(new ZipEntry("TALEND-INF/dependencies.txt"));
                nested
                        .write(new DependenciesTxtBuilder()
                                .withDependency("org.apache.tomee:ziplock:jar:7.0.5:runtime")
                                .withDependency("org.apache.tomee:javaee-api:jar:7.0-1:compile")
                                .build()
                                .getBytes(StandardCharsets.UTF_8));
            }
        }

        try (final URLClassLoader tempLoader =
                new URLClassLoader(new URL[] { file.toURI().toURL() }, getSystemClassLoader())) {
            final List<String> toResolve =
                    new MvnDependencyListLocalRepositoryResolver("TALEND-INF/dependencies.txt", d -> null)
                            .resolve(tempLoader, "foo/bar/dummy/1.0.0/dummy-1.0.0.jar")
                            .map(Artifact::toPath)
                            .collect(toList());
            assertEquals(asList("org/apache/tomee/ziplock/7.0.5/ziplock-7.0.5.jar",
                    "org/apache/tomee/javaee-api/7.0-1/javaee-api-7.0-1.jar"), toResolve);
        }
    }
}
