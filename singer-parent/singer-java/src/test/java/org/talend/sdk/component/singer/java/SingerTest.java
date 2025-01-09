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
package org.talend.sdk.component.singer.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import javax.json.Json;

import org.junit.jupiter.api.Test;

class SingerTest {

    @Test
    void writeRecord() throws UnsupportedEncodingException {
        write(s -> s.writeRecord("test_stream", Json.createObjectBuilder().add("id", 1).add("name", "Test").build()),
                "{\"type\":\"RECORD\",\"stream\":\"test_stream\",\"time_extracted\":\"2019-08-23T11:26:00.000000Z\",\"record\":{\"id\":1,\"name\":\"Test\"}}");
    }

    @Test
    void writeShema() throws UnsupportedEncodingException {
        write(s -> s
                .writeSchema("test_stream", Json.createObjectBuilder().add("id", 1).add("name", "Test").build(),
                        Json.createArrayBuilder().add("foo").build(), Json.createArrayBuilder().add("bar").build()),
                "{\"type\":\"SCHEMA\",\"stream\":\"test_stream\",\"schema\":{\"id\":1,\"name\":\"Test\"},\"key_properties\":[\"foo\"],\"bookmark_properties\":[\"bar\"]}");
    }

    @Test
    void writeState() throws UnsupportedEncodingException {
        write(s -> s.writeState(Json.createObjectBuilder().add("offset", 1).build()),
                "{\"type\":\"STATE\",\"value\":{\"offset\":1}}");
    }

    private void write(final Consumer<Singer> singerConsumer, final String expected)
            throws UnsupportedEncodingException {
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final PrintStream stdoutPs = new PrintStream(stdout);
        final Singer singer = new Singer(new IO(System.in, stdoutPs, faillingPrintStream()),
                () -> ZonedDateTime.of(2019, 8, 23, 11, 26, 0, 0, ZoneId.of("UTC")));
        singerConsumer.accept(singer);
        stdoutPs.flush();
        assertEquals(expected, stdout.toString("UTF-8").trim());
    }

    private PrintStream faillingPrintStream() {
        return new PrintStream(new OutputStream() {

            @Override
            public void write(int b) {
                fail();
            }
        });
    }
}
