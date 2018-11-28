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
package org.talend.sdk.component;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerListener;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.test.Constants;
import org.talend.sdk.component.test.rule.TempJars;

@ExtendWith(TempJars.class)
class ContainerManagerTest {

    // note: in this method we could keep a ref of list or finds but
    // we abuse intentionally of the manager methods
    // to ensure they are reentrant
    @Test
    void manage(final TempJars jars) {
        final ContainerManager ref;
        final Collection<Container> containers;
        try (final ContainerManager manager = createDefaultManager()) {
            ref = manager;
            final Container container = manager.builder("foo", createZiplockJar(jars).getAbsolutePath()).create();
            assertNotNull(container);
            // container is not tested here but in ContainerTest. Here we just take care
            // of the manager.

            assertEquals(1, manager.findAll().size());
            assertEquals(singletonList(container), new ArrayList<>(manager.findAll()));

            assertTrue(manager.find("foo").isPresent());
            assertEquals(container, manager.find("foo").get());

            final Container xbean = manager.builder("bar", createZiplockJar(jars).getAbsolutePath()).create();
            assertEquals(2, manager.findAll().size());
            Stream.of(container, xbean).forEach(c -> assertTrue(manager.findAll().contains(c)));

            containers = manager.findAll();
        }

        // now manager is closed so all containers are cleaned up
        assertTrue(ref.isClosed());
        containers.forEach(c -> assertTrue(c.isClosed()));
    }

    @Test
    void autoContainerId(final TempJars jars) {
        Stream
                .of("ziplock-7.00.3.jar", "ziplock-7.3.jar", "ziplock-7.3-SNAPSHOT.jar", "ziplock-7.3.0-SNAPSHOT.jar")
                .forEach(jarName -> {
                    try (final ContainerManager manager = createDefaultManager()) {
                        final File module = createZiplockJar(jars);
                        final File jar = new File(module.getParentFile(), jarName);
                        assertTrue(module.renameTo(jar));
                        final Container container = manager.builder(jar.getAbsolutePath()).create();
                        assertEquals("ziplock"/* no version, no .jar */, container.getId());
                    }
                });
    }

    @Test
    void listeners(final TempJars jars) {
        final Collection<String> states = new ArrayList<>();
        final ContainerListener listener = new ContainerListener() {

            @Override
            public void onCreate(final Container container) {
                states.add("deploy #" + container.getId());
            }

            @Override
            public void onClose(final Container container) {
                states.add("undeploy #" + container.getId());
            }
        };
        try (final ContainerManager manager = createDefaultManager().registerListener(listener)) {
            assertEquals(emptyList(), states);

            try (final Container container = manager.builder(createZiplockJar(jars).getAbsolutePath()).create()) {
                assertEquals(1, states.size());
            }
            assertEquals(2, states.size());

            manager.unregisterListener(listener);
            try (final Container container = manager.builder(createZiplockJar(jars).getAbsolutePath()).create()) {
                assertEquals(2, states.size());
            }
            assertEquals(2, states.size());
        }
        assertEquals(2, states.size());
    }

    @Test
    void close(final TempJars jars) {
        assertThrows(IllegalStateException.class, () -> {
            final ContainerManager manager = createDefaultManager();
            manager.close();
            manager.builder("foo", createZiplockJar(jars).getAbsolutePath()).create();
        });
    }

    private File createZiplockJar(final TempJars jars) {
        return jars.create("org.apache.tomee:ziplock:jar:7.0.5");
    }

    private ContainerManager createDefaultManager() {
        return new ContainerManager(ContainerManager.DependenciesResolutionConfiguration
                .builder()
                .resolver(new MvnDependencyListLocalRepositoryResolver(Constants.DEPENDENCIES_LIST_RESOURCE_PATH,
                        d -> null))
                .rootRepositoryLocation(new File(Constants.DEPENDENCIES_LOCATION))
                .create(), ContainerManager.ClassLoaderConfiguration.builder().create(), null, Level.INFO);
    }
}
