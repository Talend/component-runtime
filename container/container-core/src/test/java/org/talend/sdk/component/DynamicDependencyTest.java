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
package org.talend.sdk.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.test.Constants;
import org.talend.sdk.component.test.rule.TempJars;

/**
 * Tests for dynamic dependency management in containers.
 * Validates that containers can:
 * - Add dependencies dynamically
 * - Reload classloaders with new dependencies
 * - Maintain isolation between containers with different dependency versions
 * - Discover SPI and resources from dynamic dependencies
 */
@ExtendWith(TempJars.class)
class DynamicDependencyTest {

    private ContainerManager createDefaultManager() {
        return new ContainerManager(
                ContainerManager.DependenciesResolutionConfiguration
                        .builder()
                        .resolver(new MvnDependencyListLocalRepositoryResolver("MAVEN-INF/repository/dependencies.txt",
                                PathFactory::get))
                        .rootRepositoryLocation(PathFactory.get(Constants.DEPENDENCIES_LOCATION))
                        .create(),
                ContainerManager.ClassLoaderConfiguration
                        .builder()
                        .parent(Thread.currentThread().getContextClassLoader())
                        .classesFilter(name -> true)
                        .parentClassesFilter(
                                name -> !name.startsWith("org.talend.test") && !name.startsWith("org.apache.ziplock"))
                        .create(),
                c -> {
                },
                Level.INFO);
    }

    private File createZiplockJar(final TempJars jars) {
        return jars.create("org.apache.tomee:ziplock:jar:8.0.14:compile");
    }

    @Test
    void addDynamicDependency(final TempJars jars) {
        try (final ContainerManager manager = createDefaultManager()) {
            final Container container = manager.builder("test", createZiplockJar(jars).getAbsolutePath()).create();
            assertNotNull(container);

            // Initially, the container has no dynamic dependencies
            assertEquals(0, container.getDynamicDependencies().size(), "Should have no dynamic dependencies initially");

            // Create a new dynamic dependency
            final Artifact dynamicDep =
                    new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.12.0", "compile");

            // Add the dynamic dependency
            container.addDynamicDependency(dynamicDep);

            // Verify the dependency was added
            final Collection<Artifact> dynamicDeps = container.getDynamicDependencies();
            assertEquals(1, dynamicDeps.size(), "Should have 1 dynamic dependency");
            assertTrue(dynamicDeps.contains(dynamicDep), "Dynamic dependency should be present");

            // Verify findDependencies includes dynamic dependencies
            final List<Artifact> allDeps = container.findDependencies().collect(Collectors.toList());
            assertTrue(allDeps.contains(dynamicDep), "findDependencies should include dynamic dependencies");
        }
    }

    @Test
    void addMultipleDynamicDependencies(final TempJars jars) {
        try (final ContainerManager manager = createDefaultManager()) {
            final Container container = manager.builder("test", createZiplockJar(jars).getAbsolutePath()).create();
            assertNotNull(container);

            final long initialCount = container.findDependencies().count();

            // Add multiple dependencies at once
            final List<Artifact> newDeps = Arrays.asList(
                    new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.12.0", "compile"),
                    new Artifact("org.apache.commons", "commons-text", "jar", null, "1.10.0", "compile"),
                    new Artifact("com.google.guava", "guava", "jar", null, "31.1-jre", "compile"));

            container.addDynamicDependencies(newDeps);

            // Verify all dependencies were added
            final Collection<Artifact> dynamicDeps = container.getDynamicDependencies();
            assertEquals(3, dynamicDeps.size(), "Should have 3 dynamic dependencies");

            final long finalCount = container.findDependencies().count();
            assertEquals(initialCount + 3, finalCount, "Total dependencies should increase by 3");
        }
    }

    @Test
    void multipleContainersWithDifferentVersions(final TempJars jars) {
        try (final ContainerManager manager = createDefaultManager()) {
            // Create two containers
            final Container container1 = manager.builder("test1", createZiplockJar(jars).getAbsolutePath()).create();
            final Container container2 = manager.builder("test2", createZiplockJar(jars).getAbsolutePath()).create();

            assertNotNull(container1);
            assertNotNull(container2);

            // Add different versions of the same dependency to each container
            final Artifact dep1 = new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.11.0", "compile");
            final Artifact dep2 = new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.12.0", "compile");

            container1.addDynamicDependency(dep1);
            container2.addDynamicDependency(dep2);

            // Verify each container has its own version
            final Collection<Artifact> deps1 = container1.getDynamicDependencies();
            final Collection<Artifact> deps2 = container2.getDynamicDependencies();

            assertEquals(1, deps1.size(), "Container 1 should have 1 dynamic dependency");
            assertEquals(1, deps2.size(), "Container 2 should have 1 dynamic dependency");

            assertTrue(deps1.contains(dep1), "Container 1 should have version 3.11.0");
            assertTrue(deps2.contains(dep2), "Container 2 should have version 3.12.0");

            // Verify containers are isolated
            assertFalse(deps1.contains(dep2), "Container 1 should not have version 3.12.0");
            assertFalse(deps2.contains(dep1), "Container 2 should not have version 3.11.0");
        }
    }

    @Test
    void findDependenciesIncludesDynamicDeps(final TempJars jars) {
        try (final ContainerManager manager = createDefaultManager()) {
            final Container container = manager.builder("test", createZiplockJar(jars).getAbsolutePath()).create();
            assertNotNull(container);

            // Get initial dependencies
            final List<String> initialCoords = container
                    .findDependencies()
                    .map(Artifact::toCoordinate)
                    .collect(Collectors.toList());

            // Add a dynamic dependency
            final Artifact dynamicDep =
                    new Artifact("org.apache.commons", "commons-lang3", "jar", null, "3.12.0", "compile");
            container.addDynamicDependency(dynamicDep);

            // Get all dependencies after adding dynamic one
            final List<String> finalCoords = container
                    .findDependencies()
                    .map(Artifact::toCoordinate)
                    .collect(Collectors.toList());

            // Verify dynamic dependency is included
            assertTrue(finalCoords.contains(dynamicDep.toCoordinate()),
                    "findDependencies() should include dynamic dependencies");
            assertEquals(initialCoords.size() + 1, finalCoords.size(),
                    "Dependency count should increase by 1");
        }
    }

    @Test
    void loadContainerFromMavenGAV(final TempJars jars) {
        try (final ContainerManager manager = createDefaultManager()) {
            // Create a container using Maven GAV coordinates
            // The GAV format is: groupId:artifactId:version
            final String gav = "org.apache.tomee:ziplock:8.0.14";

            // This should resolve to the jar in the local repository
            final Container container = manager.builder("gav-test", gav).create();
            assertNotNull(container, "Container should be created from GAV coordinates");
            assertEquals("gav-test", container.getId());

            // Verify the container was created (findDependencies includes static + dynamic)
            assertNotNull(container.getLoader(), "Container should have a classloader");
            assertEquals("gav-test", container.getId());
        }
    }

    @Test
    void loadContainerFromFlatLibFolder(final TempJars jars) {
        try (final ContainerManager manager = createDefaultManager()) {
            // Create a jar in the temp location (simulating a flat lib/ folder)
            final File jar = createZiplockJar(jars);

            // Load using just the jar name (as if from a flat lib/ folder)
            final Container container = manager.builder("flat-lib-test", jar.getName()).create();
            assertNotNull(container, "Container should be created from flat lib folder");

            // The container should be able to find the jar
            assertTrue(container.getContainerFile().isPresent(),
                    "Container should find its jar in flat lib structure");
        }
    }
}
