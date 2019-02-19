/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.configuration.converter.secured;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CipheredStringConverterTest {

    @ParameterizedTest
    @ValueSource(strings = { "simple", "v@lue", "numb3r", "153654", "dmorjezf(t_\"yà(_\"àè" })
    void roundTrip(final String value) {
        final byte[] secret = "test-secret".getBytes(StandardCharsets.UTF_8);
        final CipheredStringConverter converter = new CipheredStringConverter(secret);
        final String cipher = new PBECipher().encrypt64(value, secret);
        assertNotEquals(value, cipher);
        assertEquals(value, converter.convert("secure:" + cipher));
    }

    @Test
    void configCiphered(final TestInfo testInfo, @TempDir final Path masterKeyDir) throws IOException {
        Files.createDirectories(masterKeyDir.getParent());
        final Path masterKey = masterKeyDir.resolve("masterKey").toAbsolutePath();
        MasterKey.write(masterKey.toString(), "test-master-key");
        final String oldKey = System.getProperty("talend.component.server.configuration.master_key.location");
        System.setProperty("talend.component.server.configuration.master_key.location", masterKey.toString());
        final ConfigProviderResolver resolver = ConfigProviderResolver.instance();
        final ClassLoader loader = new ClassLoader(Thread.currentThread().getContextClassLoader()) {
        };
        final String propertyName = testInfo.getTestClass().orElseThrow(IllegalArgumentException::new).getName() + '.'
                + testInfo.getTestMethod().orElseThrow(IllegalArgumentException::new).getName();
        final Config config =
                resolver.getBuilder().forClassLoader(loader).addDiscoveredConverters().withSources(new ConfigSource() {

                    @Override
                    public Map<String, String> getProperties() {
                        return singletonMap(propertyName, "secure:s7umRoarnqMHLza1fgY3L7eGz1A9saTijM4htHTy1x0=");
                    }

                    @Override
                    public String getValue(final String propertyName) {
                        return getProperties().get(propertyName);
                    }

                    @Override
                    public String getName() {
                        return "test";
                    }
                }).build();
        try {
            final String value =
                    config.getOptionalValue(propertyName, String.class).orElseThrow(IllegalArgumentException::new);
            assertEquals("a11 cle@r man!", value);
        } finally {
            resolver.releaseConfig(config);
            if (oldKey != null) {
                System.setProperty("talend.component.server.configuration.master_key.location", oldKey);
            } else {
                System.clearProperty("talend.component.server.configuration.master_key.location");
            }
        }
    }
}
