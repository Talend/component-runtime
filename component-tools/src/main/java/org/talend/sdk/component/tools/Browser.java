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
package org.talend.sdk.component.tools;

import static lombok.AccessLevel.PRIVATE;

import java.awt.Desktop;
import java.net.URI;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
class Browser {

    public static void open(final String url, final Log log) {
        if (!Desktop.isDesktopSupported()) {
            log.info("Desktop is not supported on this JVM, go to " + url + " in your browser");
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (final Exception e) {
            log.error(
                    "Desktop is not supported on this JVM, go to " + url + " in your browser (" + e.getMessage() + ")");
        }
    }
}
