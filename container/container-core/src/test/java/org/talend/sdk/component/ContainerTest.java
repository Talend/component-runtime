/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.test.rule.ContainerProviderRule;

@ExtendWith(ContainerProviderRule.class)
public class ContainerTest {

    @Test
    void lastTimestamp(
            @ContainerProviderRule.Instance("org.apache.xbean:xbean-finder:jar:4.9:runtime") final Container container) {
        final Date lastModifiedTimestamp = container.getLastModifiedTimestamp();
        assertTrue(lastModifiedTimestamp.getTime() > new Date(0).getTime());
        assertTrue(lastModifiedTimestamp.compareTo(container.getCreated()) <= 0);
    }

    @Test
    void findDependencies(
            @ContainerProviderRule.Instance("org.apache.xbean:xbean-finder:jar:4.9:runtime") final Container xbeanFinder) {
        assertEquals(singletonList("org.apache.xbean:xbean-finder:jar:4.9"),
                xbeanFinder.findDependencies().map(Artifact::toCoordinate).collect(toList()));
    }

    @Test
    void ensureContainerCanLoadSpecificClasses(
            @ContainerProviderRule.Instance("org.apache.tomee:ziplock:jar:8.0.14:compile") final Container ziplock) {
        ziplock.execute(() -> {
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            assertNotSame(contextClassLoader, ContainerTest.class.getClassLoader());
            try {
                assertNotNull(contextClassLoader.loadClass("org.apache.ziplock.JarLocation"));
            } catch (final ClassNotFoundException e) {
                fail(e.getMessage());
            }
            return null;
        });
    }

    @Test
    void closedContainerCantBeUsed(
            @ContainerProviderRule.Instance("org.apache.tomee:ziplock:jar:8.0.14:compile") final Container ziplock) {
        assertThrows(IllegalStateException.class, () -> {
            ziplock.close();
            ziplock.execute(() -> null);
        });
    }

    @Test
    void proxying(
            @ContainerProviderRule.Instance("org.apache.xbean:xbean-finder:jar:4.9:runtime") final Container xbeanFinder) {
        final Supplier<Filter> supplier = () -> xbeanFinder.executeAndContextualize(() -> {
            try {
                return Thread
                        .currentThread()
                        .getContextClassLoader()
                        .loadClass("org.apache.xbean.finder.filter.PrefixFilter")
                        .getConstructor(String.class)
                        .newInstance("valid.");
            } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException
                    | ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                final Throwable targetException = e.getTargetException();
                if (RuntimeException.class.isInstance(targetException)) {
                    throw RuntimeException.class.cast(targetException);
                }
                throw new IllegalStateException(targetException);
            }
        }, Filter.class);
        final Filter filter = supplier.get();

        // ensure it is usable
        assertTrue(filter.accept("valid.yes"));
        assertFalse(filter.accept("invalid.no"));

        // ensure hashcode/equals are functional (hash[map|set] friendly)
        assertEquals(filter, filter);
        assertEquals(filter.hashCode(), filter.hashCode());

        final Filter otherInstance = supplier.get();
        assertNotSame(otherInstance, filter);
        assertNotEquals(otherInstance, filter);

        final Set<Filter> set = Stream.of(filter, otherInstance).collect(toSet());
        assertEquals(2, set.size());
        assertTrue(set.contains(filter));
        assertTrue(set.contains(otherInstance));
        assertFalse(set.contains(supplier.get()));
    }

    // xbean API extracted to simulate a shared API
    public interface Filter {

        boolean accept(String name);
    }
}
