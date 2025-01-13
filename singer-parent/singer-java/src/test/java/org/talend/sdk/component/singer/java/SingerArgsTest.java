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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingerArgsTest {

    @Test
    void loadArgs(@TempDir final Path dir) throws IOException {
        final Path configJson = dir.resolve("config.json");
        Files
                .write(configJson, "{\"test\":true}".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        final SingerArgs args = new SingerArgs("--config", configJson.toAbsolutePath().toString());
        final JsonObject config = args.getConfig();
        assertNotNull(config);
        assertFalse(args.getCatalog().isPresent());
        assertFalse(args.getState().isPresent());
        assertTrue(config.getBoolean("test"));
        assertEquals(1, config.size());
    }
}
