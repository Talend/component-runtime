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
package org.talend.sdk.component.tcketl.persistence;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JsonFilesTest {

    private static final FormatVersion SUPPORTED = new FormatVersion(1, 0);

    @TempDir
    Path directory;

    @Test
    void aDocumentIsWrittenInAStableDiffFriendlyLayout() throws IOException {
        final Map<String, String> values = new HashMap<>();
        values.put("b", "2");
        values.put("a", "1");
        final Path file = directory.resolve("doc.json");

        JsonFiles.write(file, new Doc("1.0", "Title", List.of("x", "y"), values));

        assertEquals("""
                     {
                       "format-version": "1.0",
                       "title": "Title",
                       "items": [
                         "x",
                         "y"
                       ],
                       "values": {
                         "a": "1",
                         "b": "2"
                       }
                     }
                     """, Files.readString(file, UTF_8));
    }

    @Test
    void emptyCollectionsStayOnOneLineAndTextIsNotEscaped() throws IOException {
        final Path file = directory.resolve("doc.json");

        JsonFiles.write(file, new Doc("1.0", "café 😀", List.of(), Map.of()));

        assertEquals("""
                     {
                       "format-version": "1.0",
                       "title": "café 😀",
                       "items": [],
                       "values": {}
                     }
                     """, Files.readString(file, UTF_8));
    }

    @Test
    void whatIsWrittenIsReadBack() throws IOException {
        final Doc doc = new Doc("1.0", "Title", List.of("x"), Map.of("k", "v"));
        final Path file = directory.resolve("doc.json");

        JsonFiles.write(file, doc);

        assertEquals(doc, JsonFiles.read(file, Doc.class, SUPPORTED));
    }

    @Test
    void unknownFieldsAreIgnoredSoANewerMinorVersionStaysReadable() throws IOException {
        final Path file = write("{\"format-version\":\"1.4\",\"title\":\"T\",\"future\":{\"a\":[1,2]}}");

        assertEquals("T", JsonFiles.read(file, Doc.class, SUPPORTED).title());
    }

    @Test
    void aDuplicatedKeyIsRejectedInsteadOfSilentlyKeepingTheLastOne() throws IOException {
        final Path file = write("{\"format-version\":\"1.0\",\"title\":\"a\",\"title\":\"b\"}");

        final FileFormatException error =
                assertThrows(FileFormatException.class, () -> JsonFiles.read(file, Doc.class, SUPPORTED));

        assertTrue(error.getMessage().contains("Duplicate field 'title'"), error.getMessage());
    }

    @Test
    void contentAfterTheDocumentIsRejected() throws IOException {
        final Path file = write("{\"format-version\":\"1.0\"}\n{\"format-version\":\"1.0\"}");

        assertThrows(FileFormatException.class, () -> JsonFiles.read(file, Doc.class, SUPPORTED));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "  \n", "[1, 2]", "\"text\"", "null", "42" })
    void aFileThatIsNotAJsonObjectIsRejected(final String content) throws IOException {
        final Path file = write(content);

        assertThrows(FileFormatException.class, () -> JsonFiles.read(file, Doc.class, SUPPORTED));
    }

    @Test
    void aSyntaxErrorTellsWhereItIs() throws IOException {
        final Path file = write("{\n  \"format-version\": \"1.0\",\n  \"title\": oops\n}");

        final FileFormatException error =
                assertThrows(FileFormatException.class, () -> JsonFiles.read(file, Doc.class, SUPPORTED));

        assertTrue(error.getMessage().startsWith("Invalid content at line 3, column "), error.getMessage());
    }

    @Test
    void aStructureErrorTellsWhichFieldIsWrong() throws IOException {
        final Path file = write("{\"format-version\":\"1.0\",\"items\":[\"a\",{\"b\":1}]}");

        final FileFormatException error =
                assertThrows(FileFormatException.class, () -> JsonFiles.read(file, Doc.class, SUPPORTED));

        assertTrue(error.getMessage().startsWith("Invalid content at 'items[1]': "), error.getMessage());
    }

    @Test
    void aFractionIsNotAnInteger() throws IOException {
        final Path file = write("{\"format-version\":\"1.0\",\"count\":1.5}");

        assertThrows(FileFormatException.class, () -> JsonFiles.read(file, Counted.class, SUPPORTED));
    }

    @Test
    void theVersionIsCheckedBeforeTheStructureSoANewerFormatIsExplained() throws IOException {
        final Path file = write("{\"format-version\":\"2.0\",\"items\":\"no longer a list\"}");

        final FileFormatException error =
                assertThrows(FileFormatException.class, () -> JsonFiles.read(file, Doc.class, SUPPORTED));

        assertTrue(error.getMessage().startsWith("Unsupported format version 2.0"), error.getMessage());
    }

    @Test
    void theVersionIsRequiredByDefaultAndOptionalOnDemand() throws IOException {
        final Path file = write("{\"title\":\"T\"}");

        assertEquals("The 'format-version' is missing",
                assertThrows(FileFormatException.class, () -> JsonFiles.read(file, Doc.class, SUPPORTED))
                        .getMessage());
        assertEquals("T", JsonFiles.readOptionalVersion(file, Doc.class, SUPPORTED).title());
    }

    @Test
    void anOptionalVersionIsStillCheckedWhenPresent() throws IOException {
        final Path file = write("{\"format-version\":\"3.1\",\"title\":\"T\"}");

        assertThrows(FileFormatException.class, () -> JsonFiles.readOptionalVersion(file, Doc.class, SUPPORTED));
    }

    @Test
    void aMissingFileIsAnIoProblemNotAFormatProblem() {
        assertThrows(NoSuchFileException.class,
                () -> JsonFiles.read(directory.resolve("absent.json"), Doc.class, SUPPORTED));
    }

    @Test
    void writingCreatesTheMissingDirectoriesAndReplacesTheFile() throws IOException {
        final Path file = directory.resolve("a").resolve("b").resolve("doc.json");

        JsonFiles.write(file, new Doc("1.0", "first", List.of(), Map.of()));
        JsonFiles.write(file, new Doc("1.0", "second", List.of(), Map.of()));

        assertEquals("second", JsonFiles.read(file, Doc.class, SUPPORTED).title());
        assertEquals(List.of("doc.json"), names(file.getParent()));
    }

    @Test
    void aFailedWriteLeavesNeitherTheTargetNorATemporaryFileBehind() throws IOException {
        final Path target = directory.resolve("occupied");
        Files.createDirectories(target.resolve("child"));

        assertThrows(IOException.class, () -> JsonFiles.write(target, new Doc("1.0", "T", List.of(), Map.of())));

        assertTrue(Files.isDirectory(target.resolve("child")));
        assertEquals(List.of("occupied"), names(directory));
    }

    @Test
    void aPathWithoutAParentDirectoryIsRefused() {
        final Path root = directory.getRoot();

        assertThrows(IOException.class, () -> JsonFiles.write(root, new Doc("1.0", "T", List.of(), Map.of())));
    }

    private Path write(final String content) throws IOException {
        return Files.writeString(directory.resolve("input.json"), content, UTF_8);
    }

    private static List<String> names(final Path folder) throws IOException {
        try (Stream<Path> children = Files.list(folder)) {
            return children.map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    record Doc(
            @JsonProperty(JsonFiles.VERSION_FIELD) String formatVersion,
            @JsonProperty("title") String title,
            @JsonProperty("items") List<String> items,
            @JsonProperty("values") Map<String, String> values
    ) {
    }

    record Counted(@JsonProperty(JsonFiles.VERSION_FIELD) String formatVersion, @JsonProperty("count") Integer count) {
    }
}
