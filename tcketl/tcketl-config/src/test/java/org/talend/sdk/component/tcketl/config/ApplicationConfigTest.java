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
package org.talend.sdk.component.tcketl.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;

class ApplicationConfigTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t " })
    void aMissingLanguageFallsBackToEnglish(final String language) {
        assertEquals("en", ApplicationConfig.defaults().withLanguage(language).language());
    }

    @Test
    void theLanguageIsStripped() {
        assertEquals("zh_CN", ApplicationConfig.defaults().withLanguage(" zh_CN ").language());
    }

    @Test
    void eachPartIsReplacedWithoutTouchingTheOthers() {
        final ServerEndpoint server = new ServerEndpoint("https", "etl.example.com", 8443, "/tck");
        final ServerTimeouts timeouts =
                new ServerTimeouts(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3));

        final ApplicationConfig config =
                ApplicationConfig.defaults().withServer(server).withLanguage("fr").withTimeouts(timeouts);

        assertEquals(new ApplicationConfig(server, "fr", timeouts), config);
    }

    @Test
    void theServerAndTheTimeoutsAreRequired() {
        assertThrows(NullPointerException.class, () -> new ApplicationConfig(null, "en", ServerTimeouts.defaults()));
        assertThrows(NullPointerException.class, () -> new ApplicationConfig(ServerEndpoint.defaults(), "en", null));
    }
}
