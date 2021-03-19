/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.ziplock.IO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Slf4j
class DitaDocumentationGeneratorTest extends GeneratorBase {

    @Test
    void generateDita(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".zip");
        new DitaDocumentationGenerator(
                new File[] {
                        copyBinaries("org.talend.test.valid", temporaryFolder, info.getTestMethod().get().getName()) },
                Locale.ROOT, log, output, true, true).run();
        assertTrue(output.exists());
        final Map<String, String> files = new HashMap<>();
        try (final ZipInputStream zip = new ZipInputStream(new FileInputStream(output))) {
            ZipEntry nextEntry;
            while ((nextEntry = zip.getNextEntry()) != null) {
                files.put(nextEntry.getName(), IO.slurp(zip));
            }
        }
        try (final BufferedReader reader = resource("generateDita1.xml")) {
            assertEquals(reader.lines().collect(joining(System.lineSeparator())),
                    files.get("generateDita/test/my.dita").trim());
        }
        try (final BufferedReader reader = resource("generateDita2.xml")) {
            assertEquals(reader.lines().collect(joining(System.lineSeparator())),
                    files.get("generateDita/test/my2.dita").trim());
        }
        assertEquals(4, files.size());
        // folders
        assertEquals("", files.get("generateDita/test/"));
        assertEquals("", files.get("generateDita/"));
    }

    @Test
    void generateDitaConds(@TempDir final File temporaryFolder, final TestInfo info) throws IOException {
        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".zip");
        new DitaDocumentationGenerator(new File[] {
                copyBinaries("org.talend.test.activeif", temporaryFolder, info.getTestMethod().get().getName()) },
                Locale.ROOT, log, output, true, true).run();
        assertTrue(output.exists());
        final Map<String, String> files = new HashMap<>();
        try (final ZipInputStream zip = new ZipInputStream(new FileInputStream(output))) {
            ZipEntry nextEntry;
            while ((nextEntry = zip.getNextEntry()) != null) {
                files.put(nextEntry.getName(), IO.slurp(zip));
            }
        }

        try (final BufferedReader reader = resource("generateDitaConds_activeif.xml")) {
            assertEquals(formatXml(reader.lines().collect(joining(System.lineSeparator()))),
                    formatXml(files.get("generateDitaConds/test/activeif.dita")));
        }
    }

    @ParameterizedTest
    @CsvSource(
            value = { "nolayout,NoLayout.dita", "nodatasetdatastore,NoDatasetDatastore.dita", "allbasic,AllBasic.dita",
                    "alladvanced,AllAdvanced.dita", "simplemixed,SimpleMixed.dita", "hidden,Hidden.dita" })
    void generateDitaAdvanced(final String _package, final String expectedFile, @TempDir final File temporaryFolder,
            final TestInfo info) throws IOException {

        final File output = new File(temporaryFolder, info.getTestMethod().get().getName() + ".zip");
        new DitaDocumentationGenerator(new File[] { copyBinaries("org.talend.test.dita." + _package, temporaryFolder,
                info.getTestMethod().get().getName()) }, Locale.ROOT, log, output, true, true).run();
        assertTrue(output.exists());
        final Map<String, String> files = new HashMap<>();
        try (final ZipInputStream zip = new ZipInputStream(new FileInputStream(output))) {
            ZipEntry nextEntry;
            while ((nextEntry = zip.getNextEntry()) != null) {
                files.put(nextEntry.getName(), IO.slurp(zip));
            }
        }

        try (final BufferedReader reader = resource(expectedFile)) {
            assertEquals(formatXml(reader.lines().collect(joining(System.lineSeparator()))),
                    formatXml(files.get("generateDitaAdvanced/dita/" + expectedFile).trim()));
        }

        assertEquals(3, files.size());
        // folders
        assertEquals("", files.get("generateDitaAdvanced/dita/"));
        assertEquals("", files.get("generateDitaAdvanced/"));

    }

    private String formatXml(String xml) {
        return xml
                .replace("\n", "")
                .replaceAll(" +", " ")
                .replace(": ", ":")
                .replace("> ", ">")
                .replace(" <", "<")
                .trim();
    }

    private BufferedReader resource(final String name) {
        return new BufferedReader(new InputStreamReader(Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream(getClass().getSimpleName() + '/' + name)));
    }
}
