/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Singer {

    private final IO runIo;

    private final Supplier<ZonedDateTime> dateTimeSupplier;

    private final JsonBuilderFactory builderFactory = Json.createBuilderFactory(emptyMap());

    private final DateTimeFormatter rfc339 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));

    public Singer() {
        this(new IO(), ZonedDateTime::now);
    }

    public String formatDate(final ZonedDateTime dateTime) {
        return rfc339.format(dateTime);
    }

    public synchronized void writeState(final JsonObject state) {
        final JsonObject json = builderFactory.createObjectBuilder().add("type", "STATE").add("value", state).build();
        runIo.getStdout().println(json);
    }

    public synchronized void writeSchema(final String stream, final JsonObject schema, final JsonArray keys,
            final JsonArray bookmarks) {
        final JsonObject json = builderFactory
                .createObjectBuilder()
                .add("type", "SCHEMA")
                .add("stream", requireNonNull(stream, "stream can't be null"))
                .add("schema", schema)
                .add("key_properties", keys)
                .add("bookmark_properties", bookmarks)
                .build();
        runIo.getStdout().println(json);
    }

    public synchronized void writeRecord(final String stream, final JsonObject record) {
        final JsonObject json = builderFactory
                .createObjectBuilder()
                .add("type", "RECORD")
                .add("stream", requireNonNull(stream, "stream can't be null"))
                .add("time_extracted", formatDate(dateTimeSupplier.get()))
                .add("record", record)
                .build();
        runIo.getStdout().println(json);
    }

    public synchronized void stdout(final String message) {
        runIo.getStdout().println(message);
    }

    public synchronized void stderr(final String message) {
        runIo.getStderr().println(message);
    }
}
