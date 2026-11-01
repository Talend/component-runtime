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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorRef;
import org.talend.sdk.component.tcketl.domain.connector.Port;
import org.talend.sdk.component.tcketl.domain.pipeline.Link;
import org.talend.sdk.component.tcketl.domain.pipeline.Node;
import org.talend.sdk.component.tcketl.domain.pipeline.NodeId;
import org.talend.sdk.component.tcketl.domain.pipeline.Pipeline;
import org.talend.sdk.component.tcketl.domain.pipeline.Position;

class FileSystemPipelineRepositoryTest {

    private static final String COMPONENT = "{\"id\":\"c1\",\"family\":\"F\",\"name\":\"Input\",\"version\":1}";

    private final FileSystemPipelineRepository repository = new FileSystemPipelineRepository();

    @TempDir
    Path directory;

    @Test
    void theDocumentedFileIsLoadedIntoTheExpectedPipeline() throws Exception {
        assertEquals(demo(), repository.load(golden()));
    }

    @Test
    void savingThePipelineReproducesTheDocumentedFileByteForByte() throws Exception {
        final Path file = directory.resolve("saved.tcketl.json");

        repository.save(file, demo());

        assertEquals(Files.readString(golden(), UTF_8).replace("\r\n", "\n"), Files.readString(file, UTF_8));
    }

    @Test
    void awkwardTextSurvivesARoundTrip() throws IOException {
        final Map<String, String> configuration = new HashMap<>();
        configuration.put("quote", "say \"hi\"");
        configuration.put("backslash", "C:\\temp\\file");
        configuration.put("lines", "one\ntwo\r\n\tthree\u0001");
        configuration.put("unicode", "é ü 日本語 😀");
        configuration.put("empty", "");
        configuration.put("padded", "  spaced  ");
        configuration.put("config.list[0]", "[1, 2]");
        configuration.put("$maxRecords", "10");
        final Pipeline pipeline = new Pipeline("name with \"quotes\"", List
                .of(new Node(new NodeId("n-1"), "Label ✓", ref("c1", "F", "Input", 1), new Position(-12.5, 1.0E7),
                        configuration)),
                List.of());
        final Path file = directory.resolve("awkward.tcketl.json");

        repository.save(file, pipeline);

        assertEquals(pipeline, repository.load(file));
    }

    @Test
    void anEmptyPipelineIsWrittenWithEmptyCollections() throws IOException {
        final Path file = directory.resolve("empty.tcketl.json");

        repository.save(file, Pipeline.empty());

        assertEquals("""
                     {
                       "format-version": "1.0",
                       "name": "Untitled",
                       "nodes": [],
                       "links": []
                     }
                     """, Files.readString(file, UTF_8));
        assertEquals(Pipeline.empty(), repository.load(file));
    }

    @Test
    void optionalPartsFallBackToTheirDefaults() throws IOException {
        final Path file = write(
                """
                {
                  "format-version": "1.0",
                  "nodes": [ { "id": "n-1", "component": { "id": "c1", "family": "F", "name": "Input", "version": 0 } } ]
                }
                """);

        final Pipeline pipeline = repository.load(file);

        assertEquals(Pipeline.DEFAULT_NAME, pipeline.name());
        assertTrue(pipeline.links().isEmpty());
        final Node node = pipeline.nodes().get(0);
        assertEquals("Input", node.label());
        assertEquals(new Position(0, 0), node.position());
        assertEquals(Map.of(), node.configuration());
        assertEquals("", node.connector().plugin());
    }

    @Test
    void aNewerMinorVersionIsReadAndRewrittenInTheCurrentOneWithoutItsUnknownFields() throws IOException {
        final Path file = write("""
                                {
                                  "format-version": "1.9",
                                  "name": "future",
                                  "theme": { "dark": true },
                                  "nodes": [ { "id": "n-1", "badge": "new", "component": %s } ]
                                }
                                """.formatted(COMPONENT));

        final Pipeline pipeline = repository.load(file);
        repository.save(file, pipeline);

        assertEquals("future", pipeline.name());
        final String rewritten = Files.readString(file, UTF_8);
        assertTrue(rewritten.contains("\"format-version\": \"1.0\""), rewritten);
        assertFalse(rewritten.contains("theme") || rewritten.contains("badge"), rewritten);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("refusedFiles")
    void aFileThatIsNotAValidPipelineIsRefusedWithAnExplanation(final String json, final String expected)
            throws IOException {
        final Path file = write(json);

        final FileFormatException error = assertThrows(FileFormatException.class, () -> repository.load(file));

        assertTrue(error.getMessage().contains(expected), error.getMessage());
    }

    static Stream<Arguments> refusedFiles() {
        final String link = link("n-1", "n-2");
        return Stream
                .of(Arguments.of("{\"nodes\":[]}", "The 'format-version' is missing"),
                        Arguments.of("{\"format-version\":\"2.0\",\"nodes\":[]}", "Unsupported format version 2.0"),
                        Arguments.of(document(node("n-1"), link), "Invalid pipeline: Link refers to an unknown node"),
                        Arguments.of(document(node("n-1") + "," + node("n-1"), ""),
                                "Invalid pipeline: Duplicate node id"),
                        Arguments.of(document(node("n-1") + "," + node("n-2"), link + "," + link),
                                "Invalid pipeline: Duplicate link"),
                        Arguments.of(document("{\"component\":" + COMPONENT + "}", ""),
                                "Invalid nodes[0]: node id must not be blank"),
                        Arguments.of(document("{\"id\":\"  \",\"component\":" + COMPONENT + "}", ""),
                                "Invalid nodes[0]: node id must not be blank"),
                        Arguments.of(document("{\"id\":\"n-1\"}", ""),
                                "Invalid nodes[0] (n-1): 'component' is required"),
                        Arguments.of(document(nodeOf("{\"id\":\"c1\",\"family\":\"F\",\"name\":\"I\"}"), ""),
                                "'component.version' is required"),
                        Arguments.of(
                                document(nodeOf("{\"id\":\"c1\",\"family\":\"F\",\"name\":\"I\",\"version\":-1}"), ""),
                                "version must not be negative: -1"),
                        Arguments.of(
                                document(nodeOf("{\"id\":\"c1\",\"family\":\" \",\"name\":\"I\",\"version\":1}"), ""),
                                "family must not be blank"),
                        Arguments.of(document(nodeOf("{\"family\":\"F\",\"name\":\"I\",\"version\":1}"), ""),
                                "connector id must not be blank"),
                        Arguments.of(document(nodeOf(COMPONENT, "\"configuration\":{\"k\":null}"), ""),
                                "configuration value of k must not be null"),
                        Arguments.of(document(nodeOf(COMPONENT, "\"position\":{\"x\":1}"), ""),
                                "'position.y' is required"),
                        Arguments.of(document(node("n-1"), "{\"from\":{\"node\":\"n-1\",\"flow\":\"f\"}}"),
                                "Invalid links[0]: 'to' is required"),
                        Arguments.of(
                                document(node("n-1"),
                                        "{\"from\":{\"flow\":\"f\"},\"to\":{\"node\":\"n-1\",\"flow\":\"f\"}}"),
                                "Invalid links[0]: node id must not be blank"),
                        Arguments.of(document("null", ""), "'nodes[0]' is null"),
                        Arguments.of(document(node("n-1"), "null"), "'links[0]' is null"),
                        Arguments.of("{\"format-version\":\"1.0\",\"nodes\":\"n-1\"}", "Invalid content at 'nodes'"),
                        Arguments.of(
                                document(nodeOf("{\"id\":\"c1\",\"family\":\"F\",\"name\":\"I\",\"version\":\"five\"}"),
                                        ""),
                                "Invalid content at 'nodes[0].component.version'"));
    }

    private Path write(final String content) throws IOException {
        return Files.writeString(directory.resolve("input.tcketl.json"), content, UTF_8);
    }

    private Path golden() throws URISyntaxException {
        return Path.of(getClass().getResource("/pipelines/demo.tcketl.json").toURI());
    }

    private static String node(final String id) {
        return "{\"id\":\"" + id + "\",\"component\":" + COMPONENT + "}";
    }

    private static String nodeOf(final String component, final String... extraFields) {
        return "{\"id\":\"n-1\",\"component\":" + component
                + Stream.of(extraFields).map(field -> "," + field).reduce("", String::concat) + "}";
    }

    private static String link(final String from, final String to) {
        return "{\"from\":{\"node\":\"" + from + "\",\"flow\":\"__default__\"},\"to\":{\"node\":\"" + to
                + "\",\"flow\":\"__default__\"}}";
    }

    private static String document(final String nodes, final String links) {
        return "{\"format-version\":\"1.0\",\"nodes\":[" + nodes + "],\"links\":[" + links + "]}";
    }

    private static ConnectorRef ref(final String id, final String family, final String name, final int version) {
        return new ConnectorRef(new ConnectorId(id), family, name, "", version);
    }

    private static Pipeline demo() {
        final ConnectorRef input =
                new ConnectorRef(new ConnectorId("Y29uZmlndXJhdGlvbkZvcm1TYW1wbGUjc2FtcGxlRm9ybSNJbnB1dA"),
                        "sampleForm", "Input", "configurationFormSample", 1);
        final ConnectorRef client =
                new ConnectorRef(new ConnectorId("aHR0cC1zdHVkaW8jSFRUUCNDbGllbnQ"), "HTTP", "Client", "http-studio",
                        5);
        final ConnectorRef output = new ConnectorRef(new ConnectorId("YXp1cmUtZGxzLWdlbjIjQXp1cmUjQWRsc0dlbjJPdXRwdXQ"),
                "Azure", "AdlsGen2Output", "azure-dls-gen2", 3);
        final Map<String, String> configuration = Map
                .of("$maxRecords", "100", "config.dataset.datastore.login", "john",
                        "config.dataset.datastore.password", "changeit", "config.dataset.queries[0]",
                        "select * from customers", "config.dataset.queries[1]", "select 1");
        final NodeId first = new NodeId("n-1");
        final NodeId second = new NodeId("n-2");
        final NodeId third = new NodeId("n-3");
        return new Pipeline("demo",
                List
                        .of(new Node(first, "Customers", input, new Position(120, 80), configuration),
                                new Node(second, "Enrich", client, new Position(400, 80), Map.of()),
                                new Node(third, "Données", output, new Position(700, 80.5),
                                        Map.of("configuration.dataset.container", "café"))),
                List
                        .of(Link.of(first, Port.DEFAULT_FLOW, second, Port.DEFAULT_FLOW),
                                Link.of(second, Port.DEFAULT_FLOW, third, Port.DEFAULT_FLOW)));
    }
}
