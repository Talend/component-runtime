// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.dependencies.maven;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

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

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.talend.components.test.dependencies.DependenciesTxtBuilder;

public class MvnDependencyListLocalRepositoryResolverTest {

    @ClassRule
    public static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @Test
    public void nestedDependency() throws IOException {
        final File file = TEMPORARY_FOLDER.newFile(UUID.randomUUID().toString() + ".jar");
        try (final JarOutputStream enclosing = new JarOutputStream(new FileOutputStream(file))) {
            enclosing.putNextEntry(new ZipEntry("MAVEN-INF/repository/foo/bar/dummy/1.0.0/dummy-1.0.0.jar"));
            try (final JarOutputStream nested = new JarOutputStream(enclosing)) {
                nested.putNextEntry(new ZipEntry("TALEND-INF/dependencies.txt"));
                nested.write(new DependenciesTxtBuilder().withDependency("org.apache.tomee:ziplock:jar:7.0.3:runtime")
                        .withDependency("org.apache.tomee:javaee-api:jar:7.0-1:compile").build()
                        .getBytes(StandardCharsets.UTF_8));
            }
        }

        try (final URLClassLoader tempLoader = new URLClassLoader(new URL[] { file.toURI().toURL() }, getSystemClassLoader())) {
            final List<String> toResolve = new MvnDependencyListLocalRepositoryResolver("TALEND-INF/dependencies.txt")
                    .resolve(tempLoader, "foo/bar/dummy/1.0.0/dummy-1.0.0.jar").collect(toList());
            assertEquals(asList("org/apache/tomee/ziplock/7.0.3/ziplock-7.0.3.jar",
                    "org/apache/tomee/javaee-api/7.0-1/javaee-api-7.0-1.jar"), toResolve);
        }
    }
}
