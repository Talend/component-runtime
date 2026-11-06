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
package org.talend.sdk.component.starter.server.service.template;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class TemplateRendererTest {

    private static final String TEMPLATE = "TemplateRendererTest/entries.mustache";

    private static final String EXPECTED =
            "<trimStackTrace>false</trimStackTrace><runOrder>alphabetical</runOrder>(a)(b)";

    @Test
    void renderJdkInternalCollections() {
        assertEquals(EXPECTED, new TemplateRenderer().render(TEMPLATE, new Model(entries(), items())));
    }

    /**
     * The test JVM is started with {@code --add-opens java.base/java.util=ALL-UNNAMED} so reflection on JDK internal
     * entries (HashMap$Node) works in-process even without the fix. Run the rendering in a JVM without any
     * {@code --add-opens} to ensure JDK internal map entries and immutable lists are resolved through their public API.
     */
    @Test
    void renderJdkInternalCollectionsWithoutAddOpens() throws Exception {
        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final Process process = new ProcessBuilder(javaBin, "-cp", System.getProperty("java.class.path"),
                RenderMain.class.getName()).redirectErrorStream(true).start();
        final String output;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), UTF_8))) {
            output = reader.lines().collect(joining("\n"));
        }
        assertEquals(0, process.waitFor(), output);
        assertEquals(EXPECTED, output);
    }

    private static Collection<Map.Entry<String, String>> entries() {
        final Map<String, String> configuration = new LinkedHashMap<>();
        configuration.put("trimStackTrace", "false");
        configuration.put("runOrder", "alphabetical");
        return configuration.entrySet();
    }

    private static List<String> items() {
        // immutable JDK internal implementation (ImmutableCollections$ListN)
        return Stream.of("a", "b").toList();
    }

    public static class Model {

        private final Collection<Map.Entry<String, String>> configuration;

        private final List<String> items;

        public Model(final Collection<Map.Entry<String, String>> configuration, final List<String> items) {
            this.configuration = configuration;
            this.items = items;
        }

        public List<String> getItems() {
            return items;
        }

        public Collection<Map.Entry<String, String>> getConfiguration() {
            return configuration;
        }
    }

    public static class RenderMain {

        public static void main(final String[] args) {
            System.out.print(new TemplateRenderer().render(TEMPLATE, new Model(entries(), items())));
        }
    }
}
