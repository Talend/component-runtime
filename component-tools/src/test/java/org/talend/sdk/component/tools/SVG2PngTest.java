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
package org.talend.sdk.component.tools;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class SVG2PngTest {

    @Test
    void convert() throws IOException {
        final Path resolved = jarLocation(SVG2PngTest.class).toPath().resolve("test/icons");
        final Path expected = resolved.resolve("logo_icon32.png");
        final Path darkExpected = resolved.resolve("dark/logo_icon32.png");
        final Path lightExpected = resolved.resolve("light/logo_icon32.png");
        if (Files.exists(expected)) {
            Files.delete(expected);
        }
        if (Files.exists(darkExpected)) {
            Files.delete(darkExpected);
        }
        if (Files.exists(lightExpected)) {
            Files.delete(lightExpected);
        }
        new SVG2Png(resolved, true, log).run();
        assertTrue(Files.exists(expected));
        assertTrue(Files.exists(darkExpected));
        assertTrue(Files.exists(lightExpected));
    }

    @Test
    void exceptionWithNotExistPath() {
        String path = "not/existing/path";
        Assertions.assertThrows(java.lang.IllegalStateException.class, () -> {
            new SVG2Png(Paths.get(path), true, log).run();
        });
    }

    @Test
    void exceptionWithWrongMethod() {
        String object = "wrong log object";
        Assertions.assertThrows(java.lang.IllegalArgumentException.class, () -> {
            new SVG2Png(Paths.get(""), true, object).run();
        });
    }
}
