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
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;

class LocalConfigurationServiceTest {

    private final LocalConfiguration systemProperties = new LocalConfiguration() {

        @Override
        public String get(final String key) {
            return System.getProperty(key);
        }

        @Override
        public Set<String> keys() {
            return System.getProperties().stringPropertyNames();
        }
    };

    @Test
    void nullDoesntFail() {
        assertNull(new LocalConfigurationService(singletonList(systemProperties), "LocalConfigurationServiceTest")
                .get("test.foo.missing"));
    }

    @Test
    void read() {
        System.setProperty("LocalConfigurationServiceTest.test.foo", "1");
        try {
            assertEquals("1",
                    new LocalConfigurationService(singletonList(systemProperties), "LocalConfigurationServiceTest")
                            .get("test.foo"));
        } finally {
            System.clearProperty("LocalConfigurationServiceTest.test.foo");
        }
    }

    @Test
    void readGlobal() {
        System.setProperty("test.foo.LocalConfigurationServiceTest", "1");
        try {
            assertEquals("1",
                    new LocalConfigurationService(singletonList(systemProperties), "LocalConfigurationServiceTest")
                            .get("test.foo.LocalConfigurationServiceTest"));
        } finally {
            System.clearProperty("test.foo.LocalConfigurationServiceTest");
        }
    }

    @Test
    void keys() {
        System.setProperty("LocalConfigurationServiceTest.test.foo", "1");
        System.setProperty("LocalConfigurationServiceTest.test.bar", "1");
        try {
            assertEquals(Stream.of("test.foo", "test.bar").collect(toSet()),
                    new LocalConfigurationService(singletonList(systemProperties), "LocalConfigurationServiceTest")
                            .keys()
                            .stream()
                            .filter(it -> it.startsWith("test"))
                            .collect(toSet()));
        } finally {
            System.clearProperty("LocalConfigurationServiceTest.test.foo");
            System.clearProperty("LocalConfigurationServiceTest.test.bar");
        }
    }
}
