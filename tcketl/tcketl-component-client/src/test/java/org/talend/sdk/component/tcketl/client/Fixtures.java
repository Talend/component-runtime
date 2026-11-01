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
package org.talend.sdk.component.tcketl.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads the recorded server payloads stored under {@code src/test/resources/fixtures}. Files prefixed with
 * {@code sample-} are derived from real server responses, files prefixed with {@code real-} are answers captured from
 * a running component server (re-indented only), files prefixed with {@code synthetic-} are hand built.
 */
final class Fixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Fixtures() {
        // no-op
    }

    static String readText(final String name) {
        try (final InputStream stream = Fixtures.class.getResourceAsStream("/fixtures/" + name)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing fixture: " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static JsonNode readJson(final String name) {
        try {
            return MAPPER.readTree(readText(name));
        } catch (final IOException e) {
            throw new UncheckedIOException("Invalid JSON in fixture " + name, e);
        }
    }

    /** The fixture decoded the way the client decodes a server answer. */
    static <T> T wire(final String name, final Class<T> type) {
        return wireJson(readText(name), type);
    }

    /** A payload written inline, decoded the way the client decodes a server answer. */
    static <T> T wireJson(final String json, final Class<T> type) {
        return Wire.read(new HttpTransport.Response(200, json.getBytes(StandardCharsets.UTF_8), HttpTransport.JSON),
                type);
    }
}
