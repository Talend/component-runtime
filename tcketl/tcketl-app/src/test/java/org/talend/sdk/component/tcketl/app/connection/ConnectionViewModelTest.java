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
package org.talend.sdk.component.tcketl.app.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.talend.sdk.component.tcketl.app.testing.TestAsync;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerStatus;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;
import org.talend.sdk.component.tcketl.domain.server.ServerUnavailableException;

class ConnectionViewModelTest {

    private final ApplicationConfig current = new ApplicationConfig(
            new ServerEndpoint("http", "localhost", 8080, "/api/v1"), "en",
            new ServerTimeouts(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));

    private final TestAsync async = new TestAsync();

    private final List<ApplicationConfig> tested = new ArrayList<>();

    private RuntimeException unreachable;

    private final ConnectionViewModel model = new ConnectionViewModel(current, settings -> {
        tested.add(settings);
        if (unreachable != null) {
            throw unreachable;
        }
        return new ServerStatus("1.2611.0", "abc123", "stamp");
    }, async.runner());

    @Test
    void startsFromTheCurrentSettings() {
        assertEquals("http", model.scheme().get());
        assertEquals("localhost", model.host().get());
        assertEquals("8080", model.port().get());
        assertEquals("/api/v1", model.basePath().get());
        assertEquals("en", model.language().get());
        assertTrue(model.valid().get());
        assertEquals("http://localhost:8080/api/v1", model.url().get());
        assertEquals(Optional.of(current), model.result());
    }

    @Test
    void theResultKeepsTheSettingsTheDialogDoesNotEdit() {
        model.scheme().set("https");
        model.host().set("tck.example.org");
        model.port().set(" 9443 ");
        model.basePath().set("/tck/api/v1/");
        model.language().set("fr");

        final ApplicationConfig result = model.result().orElseThrow();
        assertEquals("https://tck.example.org:9443/tck/api/v1", result.server().toString());
        assertEquals("fr", result.language());
        assertEquals(current.timeouts(), result.timeouts());
    }

    @ParameterizedTest(name = "host=\"{0}\" port=\"{1}\"")
    @CsvSource(delimiter = '|', value = { "''|8080|The host is required.", "'   '|8080|The host is required.",
            "localhost|''|The port is required.", "localhost|abc|The port must be a number between 1 and 65535.",
            "localhost|0|Port must be between 1 and 65535: 0.",
            "localhost|70000|Port must be between 1 and 65535: 70000.",
            "a/b|8080|Host must be a plain host name or address: a/b." })
    void explainsWhatMakesTheSettingsUnusable(final String host, final String port, final String message) {
        model.host().set(host);
        model.port().set(port);

        assertEquals(message, model.validationMessage().get());
        assertFalse(model.valid().get());
        assertEquals(Optional.empty(), model.result());
        assertEquals("", model.url().get());
    }

    @Test
    void testingTellsWhichServerAnswered() {
        model.test();
        assertTrue(model.testing().get());
        assertEquals("Testing\u2026", model.testMessage().get());
        assertEquals(List.of(), tested);

        async.drain();

        assertFalse(model.testing().get());
        assertTrue(model.testSucceeded().get());
        assertEquals("Connected to component server 1.2611.0 (commit abc123)", model.testMessage().get());
        assertEquals(List.of(current), tested);
    }

    @Test
    void testingTellsWhyTheServerCannotBeReached() {
        unreachable = new ServerUnavailableException("Cannot reach http://localhost:8080/api/v1", null);

        model.test();
        async.drain();

        assertFalse(model.testing().get());
        assertFalse(model.testSucceeded().get());
        assertEquals("Cannot reach http://localhost:8080/api/v1", model.testMessage().get());
    }

    @Test
    void settingsThatAreNotUsableAreNotTested() {
        model.host().set("");

        model.test();
        async.drain();

        assertFalse(model.testing().get());
        assertEquals(List.of(), tested);
    }

    @Test
    void editingAFieldForgetsWhatTheTestSaidAndIgnoresATestInFlight() {
        model.test();
        async.drain();
        model.port().set("8081");
        assertFalse(model.testSucceeded().get());
        assertEquals("", model.testMessage().get());

        model.test();
        async.io().runNext();
        model.host().set("elsewhere");
        async.drain();

        assertFalse(model.testing().get());
        assertEquals("", model.testMessage().get());
    }
}
