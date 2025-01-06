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

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SingerArgs {

    @Getter
    private final JsonObject config; // JSON containing any configuration parameters the Tap needs

    private final JsonObject state; // (optional) where the stream stopped and can restart from

    private final JsonObject catalog; // (optional) JSON that the Tap can use to filter which streams should be synced

    @Getter
    private final boolean discover;

    private final String componentFamily;

    @Getter
    private final Map<String, String> otherArgs;

    public SingerArgs(final String... args) {
        String config = null;
        String state = null;
        String catalog = null;
        String family = null;
        boolean discover = false;
        Map<String, String> otherArgs = new HashMap<>();

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("No --config");
        }
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg == null) {
                continue;
            }
            switch (arg) {
                case "--config":
                    config = args[i + 1];
                    i++;
                    break;
                case "--state":
                    state = args[i + 1];
                    i++;
                    break;
                case "--catalog":
                    catalog = args[i + 1];
                    i++;
                    break;
                case "--component-family":
                    family = args[i + 1];
                    i++;
                    break;
                case "--discover":
                    discover = true;
                    break;
                default:
                    if (arg.startsWith("--")) {
                        otherArgs.put(arg, i + 1 < args.length ? args[i + 1] : "true");
                        i++;
                    } // else fail?
            }
        }
        if (config == null) {
            throw new IllegalArgumentException("No --config");
        }

        final JsonReaderFactory readerFactory = Json.createReaderFactory(emptyMap());
        this.config = readObject(readerFactory, config);
        this.state = ofNullable(state).map(it -> readObject(readerFactory, it)).orElse(null);
        this.catalog = ofNullable(catalog).map(it -> readObject(readerFactory, it)).orElse(null);
        this.discover = discover;
        this.componentFamily = family;
        this.otherArgs = otherArgs;
    }

    public Optional<JsonObject> getState() {
        return ofNullable(state);
    }

    public Optional<JsonObject> getCatalog() {
        return ofNullable(catalog);
    }

    public Optional<String> getComponentFamily() {
        return ofNullable(componentFamily);
    }

    private JsonObject readObject(final JsonReaderFactory factory, final String path) {
        final Path source = get(path);
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("No file available at '" + source + "'");
        }
        try (final JsonReader jsonReader = factory.createReader(Files.newInputStream(source))) {
            return jsonReader.readObject();
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static Path get(final String path) {
        String p = path;
        if (p.startsWith("/") && p.indexOf(':') == 2) { // windows
            p = p.substring(1);
        }
        return Paths.get(p);
    }
}
