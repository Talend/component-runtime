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
package org.talend.runtime.documentation;

import static lombok.AccessLevel.PRIVATE;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class StaticRedirector {

    public static final String LATEST_REDIRECT = "<!DOCTYPE html>\n" +
            "<meta charset=\"utf-8\">\n" +
            "<link rel=\"canonical\" href=\"https://talend.github.io/component-runtime/main/latest/index.html\">\n" +
            "<script>location=\"main/latest/index.html\"</script>\n" +
            "<meta http-equiv=\"refresh\" content=\"0; url=main/latest/index.html\">\n" +
            "<meta name=\"robots\" content=\"noindex\">\n" +
            "<title>Redirect Notice</title>\n" +
            "<h1>Redirect Notice</h1>\n" +
            "<p>The page you requested has been relocated to <a href=\"main/latest/index.html\">https://talend.github.io/component-runtime/main/latest/index.html</a>.</p>";

    public static void main(final String[] args) {
        log.info("Creating redirection file to latest.");
        final File siteRoot = new File(args[0]);
        final File index = new File(siteRoot, "index.html");
        try (final OutputStream output = new WriteIfDifferentStream(index)) {
            output.write(LATEST_REDIRECT.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException e) {
            log.error(e.getMessage());
            throw new IllegalStateException(e);
        }
        log.info("Created {}", index);
    }
}
