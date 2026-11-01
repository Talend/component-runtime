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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;

class FileConfigStoreTest {

    @TempDir
    Path directory;

    @Test
    void aMissingFileMeansTheDefaultsWithoutWarningAndIsNotCreated() {
        final FileConfigStore store = new FileConfigStore(file());

        final ConfigLoadResult result = store.load();

        assertEquals(ApplicationConfig.defaults(), result.config());
        assertTrue(result.warning().isEmpty());
        assertFalse(Files.exists(file()));
    }

    @Test
    void savedSettingsAreLoadedBack() throws IOException {
        final FileConfigStore store = new FileConfigStore(file());

        store.save(custom());
        final ConfigLoadResult result = store.load();

        assertEquals(custom(), result.config());
        assertTrue(result.warning().isEmpty());
    }

    @Test
    void theFileFormatIsTheDocumentedOne() throws IOException {
        new FileConfigStore(file()).save(custom());

        assertEquals("""
                     {
                       "format-version": "1.0",
                       "server": {
                         "scheme": "https",
                         "host": "etl.example.com",
                         "port": 8443,
                         "base-path": "/tck/api/v1"
                       },
                       "language": "fr",
                       "timeouts": {
                         "connect-ms": 1000,
                         "request-ms": 2000,
                         "action-ms": 3000
                       }
                     }
                     """, Files.readString(file(), UTF_8));
    }

    @Test
    void everyPartIsOptionalAndFallsBackToItsDefault() throws IOException {
        write("{\"server\":{\"host\":\"etl.example.com\"},\"timeouts\":{\"request-ms\":1500}}");

        final ApplicationConfig config = new FileConfigStore(file()).load().config();

        assertEquals(new ServerEndpoint("http", "etl.example.com", 8080, "/api/v1"), config.server());
        assertEquals("en", config.language());
        assertEquals(new ServerTimeouts(Duration.ofSeconds(5), Duration.ofMillis(1500), Duration.ofSeconds(60)),
                config.timeouts());
    }

    @Test
    void anEmptyBasePathMeansTheServerIsAtTheRoot() throws IOException {
        write("{\"server\":{\"base-path\":\"\"}}");

        assertEquals("", new FileConfigStore(file()).load().config().server().basePath());
    }

    @Test
    void unknownKeysAndANewerMinorVersionAreTolerated() throws IOException {
        write("{\"format-version\":\"1.3\",\"language\":\"de\",\"theme\":\"dark\",\"server\":{\"proxy\":\"x\"}}");

        final ConfigLoadResult result = new FileConfigStore(file()).load();

        assertEquals("de", result.config().language());
        assertTrue(result.warning().isEmpty());
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("unusableFiles")
    void aFileThatCannotBeUsedGivesTheDefaultsAndAWarningAndIsLeftUntouched(final String content,
            final String reason) throws IOException {
        write(content);

        final ConfigLoadResult result = new FileConfigStore(file()).load();

        assertEquals(ApplicationConfig.defaults(), result.config());
        final String warning = result.warning().orElseThrow();
        assertTrue(warning.contains(file().toString()), warning);
        assertTrue(warning.contains(reason), warning);
        assertEquals(content, Files.readString(file(), UTF_8));
    }

    static Stream<Arguments> unusableFiles() {
        return Stream
                .of(Arguments.of("{", "Invalid content at line 1"), Arguments.of("", "The file is empty"),
                        Arguments.of("[]", "does not contain a JSON object"),
                        Arguments.of("{\"format-version\":\"2.0\"}", "Unsupported format version 2.0"),
                        Arguments.of("{\"format-version\":7}", "<major>.<minor>"),
                        Arguments.of("{\"server\":{\"port\":0}}", "port must be between 1 and 65535: 0"),
                        Arguments.of("{\"server\":{\"scheme\":\"ftp\"}}", "scheme must be http or https: ftp"),
                        Arguments.of("{\"server\":{\"host\":\"a/b\"}}", "host must be a plain host name or address"),
                        Arguments.of("{\"timeouts\":{\"connect-ms\":0}}", "connect timeout must be positive"),
                        Arguments.of("{\"timeouts\":{\"action-ms\":-5}}", "action timeout must be positive"),
                        Arguments.of("{\"server\":{\"port\":\"abc\"}}", "Invalid content at 'server.port'"),
                        Arguments.of("{\"server\":{\"port\":80.5}}", "Invalid content at 'server.port'"),
                        Arguments.of("{\"language\":{\"a\":1}}", "Invalid content at 'language'"));
    }

    @Test
    void aDirectoryInPlaceOfTheFileIsReportedNotThrown() throws IOException {
        Files.createDirectories(file());

        final ConfigLoadResult result = new FileConfigStore(file()).load();

        assertEquals(ApplicationConfig.defaults(), result.config());
        assertTrue(result.warning().isPresent());
    }

    @Test
    void theLocationIsAbsoluteAndNormalized() {
        assertEquals(Path.of("config.json").toAbsolutePath(),
                new FileConfigStore(Path.of("a", "..", "config.json")).file());
    }

    private Path file() {
        return directory.resolve("tcketl").resolve("config.json");
    }

    private void write(final String content) throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), content, UTF_8);
    }

    private static ApplicationConfig custom() {
        return new ApplicationConfig(new ServerEndpoint("https", "etl.example.com", 8443, "/tck/api/v1"), "fr",
                new ServerTimeouts(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(3)));
    }
}
