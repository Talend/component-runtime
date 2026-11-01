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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConfigLocatorTest {

    private static final String FILE_NAME = "config.json";

    @TempDir
    Path directory;

    @Test
    void theOptionBeatsTheEnvironmentAndTheUserDefault() {
        final Path file = directory.resolve("option.json");

        assertEquals(file, locate(List.of("--config", file.toString()), Map.of("TCKETL_CONFIG", "ignored.json")));
    }

    @Test
    void theOptionCanBeWrittenWithAnEqualsSign() {
        final Path file = directory.resolve("option.json");

        assertEquals(file, locate(List.of("--config=" + file), Map.of()));
    }

    @Test
    void theLastOptionWinsAndOtherArgumentsAreIgnored() {
        final Path first = directory.resolve("first.json");
        final Path last = directory.resolve("last.json");

        assertEquals(last, locate(List.of("--verbose", "--config", first.toString(), "--other=1", "--config=" + last),
                Map.of()));
    }

    @Test
    void aRelativeFileIsResolvedAgainstTheWorkingDirectory() {
        assertEquals(Path.of("settings.json").toAbsolutePath().normalize(),
                locate(List.of("--config=conf/../settings.json"), Map.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "--config=", "--config= " })
    void anOptionWithAnEmptyValueIsRefused(final String argument) {
        assertThrows(IllegalArgumentException.class, () -> locate(List.of(argument), Map.of()));
    }

    @Test
    void anOptionWithoutAValueIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> locate(List.of("--config"), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> locate(List.of("--config", " "), Map.of()));
    }

    @Test
    void theEnvironmentVariableIsUsedWhenThereIsNoOption() {
        final Path file = directory.resolve("environment.json");

        assertEquals(file, locate(List.of(), Map.of("TCKETL_CONFIG", file.toString())));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  " })
    void aBlankEnvironmentVariableIsIgnored(final String blank) {
        assertEquals(dotConfig(), locate(List.of(), Map.of("TCKETL_CONFIG", blank)));
    }

    @Test
    void anInvalidPathNamesItsSource() {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> locate(List.of(), Map.of("TCKETL_CONFIG", "bad\u0000name")));

        assertTrue(error.getMessage().contains("TCKETL_CONFIG"), error.getMessage());
    }

    @Test
    void windowsUsesTheRoamingApplicationDataFolder() {
        final Path appData = directory.resolve("roaming");

        assertEquals(appData.resolve("tcketl").resolve(FILE_NAME),
                ConfigLocator.locate(List.of(), Map.of("APPDATA", appData.toString()), "Windows 11", directory));
    }

    @Test
    void windowsWithoutApplicationDataFallsBackToTheProfileFolder() {
        assertEquals(directory.resolve("AppData").resolve("Roaming").resolve("tcketl").resolve(FILE_NAME),
                ConfigLocator.locate(List.of(), Map.of(), "Windows 10", directory));
    }

    @Test
    void otherSystemsUseTheXdgConfigurationFolderWhenItIsAbsolute() {
        final Path xdg = directory.resolve("xdg");

        assertEquals(xdg.resolve("tcketl").resolve(FILE_NAME), ConfigLocator
                .locate(List.of(), Map.of("XDG_CONFIG_HOME", xdg.toString()), "Linux", directory.resolve("home")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  ", "relative/xdg" })
    void otherSystemsFallBackToDotConfigWhenXdgIsUnusable(final String xdg) {
        assertEquals(dotConfig(), locate(List.of(), Map.of("XDG_CONFIG_HOME", xdg)));
    }

    @Test
    void thePublicEntryPointHonoursTheOption() {
        final Path file = directory.resolve("public.json");

        assertEquals(file, ConfigLocator.locate(List.of("--config", file.toString())));
    }

    private Path dotConfig() {
        return directory.resolve("home").resolve(".config").resolve("tcketl").resolve(FILE_NAME);
    }

    private Path locate(final List<String> arguments, final Map<String, String> environment) {
        return ConfigLocator.locate(arguments, environment, "Mac OS X", directory.resolve("home"));
    }
}
