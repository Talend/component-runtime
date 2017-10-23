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
package org.talend.components;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.talend.components.container.Container;
import org.talend.components.test.rule.ContainerProviderRule;

public class ContainerTest {

    @Rule
    public final TestRule containerManager = new ContainerProviderRule(this);

    @ContainerProviderRule.Instance("org.apache.tomee:ziplock:jar:7.0.3:compile")
    private Container ziplock;

    @ContainerProviderRule.Instance("org.apache.xbean:xbean-finder:jar:4.5:runtime")
    private Container xbeanFinder;

    @Test
    public void ensureContainerCanLoadSpecificClasses() {
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

    @Test(expected = IllegalStateException.class)
    public void closedContainerCantBeUsed() {
        ziplock.close();
        ziplock.execute(() -> null);
    }

    @Test
    public void proxying() {
        final Supplier<Filter> supplier = () -> xbeanFinder.executeAndContextualize(() -> {
            try {
                return Thread.currentThread().getContextClassLoader().loadClass("org.apache.xbean.finder.filter.PrefixFilter")
                        .getConstructor(String.class).newInstance("valid.");
            } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | ClassNotFoundException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
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
        assertFalse(otherInstance == filter);
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
