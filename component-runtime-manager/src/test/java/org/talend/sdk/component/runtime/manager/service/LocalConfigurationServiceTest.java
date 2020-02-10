/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
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
import static java.util.Locale.ROOT;
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

    private final LocalConfiguration environmentProperties = new LocalConfiguration() {

        @Override
        public String get(final String key) {
            String val = System.getenv(key);
            if (val != null) {
                return val;
            }
            String k = key.replaceAll("[^A-Za-z0-9]", "_");
            val = System.getenv(k);
            if (val != null) {
                return val;
            }
            val = System.getenv(k.toUpperCase(ROOT));
            if (val != null) {
                return val;
            }
            return null;
        }

        @Override
        public Set<String> keys() {
            return System.getenv().keySet();
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

    @Test
    void environmentPropertiesGet() {
        assertEquals("/home/user", environmentProperties.get("USER_PATH"));
        assertEquals("/home/user", environmentProperties.get("user_path"));
        assertEquals("/home/user", environmentProperties.get("user_PATH"));
        assertEquals("/home/user", environmentProperties.get("talend_localconfig_user_home"));
        assertEquals("/home/user", environmentProperties.get("talend.localconfig.user.home"));
        assertEquals("true", environmentProperties.get("talend.LOCALCONFIG.test.0"));
        assertEquals("true", environmentProperties.get("talend.localconfig.test_0"));
        assertEquals("true", environmentProperties.get("talend.localconfig.TEST.0"));
        assertEquals("true", environmentProperties.get("talend.localconfig.test#0"));
        assertEquals("true", environmentProperties.get("talend$localconfig.test+0"));
    }

    @Test
    void environmentPropertiesGetAbsentKey() {
        assertNull(environmentProperties.get("talend.compmgr.exists"));
        assertNull(environmentProperties.get("HOMER"));
        assertNull(environmentProperties.get("TALEND_LOCALCONFIG_USER_HOME"));
    }
}
