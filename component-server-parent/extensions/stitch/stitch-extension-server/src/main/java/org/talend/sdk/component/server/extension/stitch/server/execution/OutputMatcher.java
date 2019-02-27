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
package org.talend.sdk.component.server.extension.stitch.server.execution;

import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

import java.io.StringReader;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public enum OutputMatcher implements Predicate<String> {
    DATA("{") {

        @Override
        public Data read(final String line, final Supplier<String> readNextLine, final JsonReaderFactory readerFactory,
                final JsonObjectBuilder objectBuilder) {
            final JsonObject data = readJson(readerFactory, line);
            final JsonString type = data.getJsonString("type");
            final String sentType = (type == null ? "unknown" : type.getString()).toLowerCase(ROOT);
            final String nextLine = readNextLine.get();
            switch (sentType) {
            case "state":
                return new Data(sentType, data.getJsonObject("value"), nextLine);
            case "record":
            case "schema":
                return new Data(sentType, data.getJsonObject(sentType), nextLine);
            default:
                return new Data(sentType, data, nextLine);
            }
        }
    },
    EXCEPTION("Traceback ") {

        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {
            final StringBuilder stack = new StringBuilder(current).append('\n');
            String line;
            while ((line = readNextLine.get()) != null) {
                if (!line.startsWith("  ")) {
                    break;
                }
                stack.append('\n').append(line);
            }
            return new Data(name().toLowerCase(ROOT), objectBuilder.add("exception", stack.toString()).build(), line);
        }
    },
    METRIC("INFO METRIC: ") {

        //
        // {"type": "counter|timer", "metric": "<name>", "value": xxxx, "tags": {"xxx": "xxx"}}
        //
        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {

            final String metric = current.substring(startsWith.length()).trim();
            if (metric.startsWith("{")) {
                return new Data("metrics", objectBuilder.add("metric", readJson(readerFactory, metric)).build(),
                        readNextLine.get());
            }
            return new Data("raw_metrics", objectBuilder.add("metric", metric).build(), readNextLine.get());
        }
    },
    LOG_DEBUG("DEBUG ") {

        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {
            return readLogLine(current, readNextLine, objectBuilder);
        }
    },
    LOG_INFO("INFO ") {

        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {
            return readLogLine(current, readNextLine, objectBuilder);
        }
    },
    LOG_WARNING("WARNING ") {

        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {
            return readLogLine(current, readNextLine, objectBuilder);
        }
    },
    LOG_ERROR("ERROR ") {

        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {
            return readLogLine(current, readNextLine, objectBuilder);
        }
    },
    LOG_CRITICAL("CRITICAL ") {

        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {
            return readLogLine(current, readNextLine, objectBuilder);
        }
    },
    LOG_FATAL("FATAL ") {

        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {
            return readLogLine(current, readNextLine, objectBuilder);
        }
    },
    LOG_EXCEPTION("EXCEPTION ") {

        @Override
        public Data read(final String current, final Supplier<String> readNextLine,
                final JsonReaderFactory readerFactory, final JsonObjectBuilder objectBuilder) {
            return readLogLine(current, readNextLine, objectBuilder);
        }
    };

    protected final String startsWith;

    @Override
    public boolean test(final String line) {
        return line.startsWith(line);
    }

    public abstract Data read(String current, Supplier<String> readNextLine, JsonReaderFactory readerFactory,
            JsonObjectBuilder objectBuilder);

    public static Optional<OutputMatcher> matches(final String line) {
        return Stream.of(values()).filter(it -> line.startsWith(it.startsWith)).findFirst();
    }

    private static JsonObject readJson(final JsonReaderFactory factory, final String line) {
        try (final JsonReader jr = factory.createReader(new StringReader(line.trim()))) {
            return jr.readObject();
        }
    }

    private static Data readLogLine(final String current, final Supplier<String> readNextLine,
            final JsonObjectBuilder objectBuilder) {
        final int space = current.indexOf(' ');
        final String level = current.substring(0, space).toLowerCase(ROOT);
        final StringBuilder builder = new StringBuilder(current.substring(space + 1));
        String line;
        final String spacesPrefix = IntStream.range(0, space + 1).mapToObj(i -> " ").collect(joining());
        while ((line = readNextLine.get()) != null) {
            if (line.startsWith("\t") || line.startsWith(spacesPrefix)) {
                builder.append('\n').append(line);
            } else {
                break;
            }
        }
        return new Data("log", objectBuilder.add("level", level).add("message", builder.toString()).build(), line);
    }

    @lombok.Data
    @RequiredArgsConstructor(access = PRIVATE)
    public static class Data {

        private final String type;

        private final JsonObject object;

        private final String nextLine;
    }
}
