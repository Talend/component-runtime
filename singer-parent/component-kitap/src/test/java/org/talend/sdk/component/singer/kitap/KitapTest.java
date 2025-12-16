/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.singer.kitap;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.talend.sdk.component.singer.java.IO;
import org.talend.sdk.component.singer.java.Singer;
import org.talend.sdk.component.singer.java.SingerArgs;

@TestInstance(PER_CLASS)
class KitapTest {

    private static final ZonedDateTime CONSTANT_DATE = ZonedDateTime.of(2019, 8, 23, 15, 11, 0, 0, ZoneId.of("UTC"));

    private static Path config;

    private static IO byteArrayOutputStreamIO;

    private static ByteArrayOutputStream stdout;

    private static ByteArrayOutputStream stderr;

    private static Runnable flushIO;

    private static PrintStream stdoutBackup;

    private static PrintStream stderrBackup;

    @BeforeAll
    static void init(@TempDir final Path tempDir) throws IOException {
        EnvironmentSetup.init();
        stdoutBackup = System.out;
        stderrBackup = System.err;

        config = tempDir.resolve("config.json");
        Files
                .write(config,
                        "{\"component\":{\"family\":\"kitaptest\",\"name\":\"kitapsource\",\"version\":1,\"configuration\":{\"configuration.recordCount\":10}}}"
                                .getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        stdout = new ByteArrayOutputStream();
        stderr = new ByteArrayOutputStream();
        byteArrayOutputStreamIO = new IO(new InputStream() {

            @Override
            public int read() {
                return -1;
            }
        }, new PrintStream(stdout), new PrintStream(stderr));
        flushIO = () -> {
            try {
                stdout.flush();
                stderr.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @AfterEach
    void resetOutputs() {
        stdout.reset();
        stderr.reset();
    }

    @AfterAll
    static void resetSystemStd() {
        System.setOut(stdoutBackup);
        System.setErr(stderrBackup);
    }

    @Test
    void discover() throws IOException {
        final Kitap kitap = new Kitap(new SingerArgs("--config", config.toAbsolutePath().toString(), "--discover"),
                new Singer(byteArrayOutputStreamIO, () -> CONSTANT_DATE));
        kitap.run();
        flushIO.run();
        assertEquals("{\"streams\":[" + "{\"tap_stream_id\":\"default\",\"stream\":\"default\","
                + "\"schema\":{\"type\":[\"null\",\"object\"],\"additionalProperties\":false,\"properties\":{\"record_number\":{\"type\":[\"integer\"]}}},"
                + "\"metadata\":[{\"metadata\":{\"inclusion\":\"automatic\",\"selected-by-default\":true},\"breadcrumb\":[\"properties\",\"record_number\"]}]}]}"
                + System.lineSeparator(),
                stdout.toString(StandardCharsets.UTF_8));
    }

    @Test
    void readAll() throws IOException {
        final Kitap kitap = new Kitap(new SingerArgs("--config", config.toAbsolutePath().toString()),
                new Singer(byteArrayOutputStreamIO, () -> CONSTANT_DATE));
        kitap.run();
        flushIO.run();

        try (final BufferedReader reader =
                new BufferedReader(new StringReader(stdout.toString(StandardCharsets.UTF_8)))) {
            final List<String> actuals = reader.lines().collect(toList());
            assertLinesMatch(asList(
                    "{\"type\":\"SCHEMA\",\"stream\":\"default\",\"schema\":{\"type\":[\"null\",\"object\"],\"additionalProperties\":false,\"properties\":{\"record_number\":{\"type\":[\"integer\"]}}},\"key_properties\":[],\"bookmark_properties\":[]}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":1}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":2}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":3}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":4}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":5}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":6}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":7}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":8}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":9}}",
                    "{\"type\":\"RECORD\",\"stream\":\"default\",\"time_extracted\":\"2019-08-23T15:11:00.000000Z\",\"record\":{\"record_number\":10}}"),
                    actuals, actuals::toString);
        }
        try (final BufferedReader reader =
                new BufferedReader(new StringReader(stderr.toString(StandardCharsets.UTF_8)))) {
            List<String> expected = Arrays.asList("log4j error", "logback error", "jul error");
            final List<String> actuals = reader.lines().collect(toList());
            for (String end : expected) {
                Optional<String> any = actuals.stream().filter(l -> l.endsWith(end)).findAny();
                Assertions.assertTrue(any.isPresent(), String.format("Can't find '%s' in error logs.", end));
            }
        }
    }
}