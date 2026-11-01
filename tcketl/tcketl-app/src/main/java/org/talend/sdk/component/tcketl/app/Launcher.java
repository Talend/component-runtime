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
package org.talend.sdk.component.tcketl.app;

import java.util.List;

import javafx.application.Application;

import org.talend.sdk.component.tcketl.config.ConfigLocator;

/**
 * Entry point of the application. It deliberately does not extend {@link Application}: the JDK launcher refuses to
 * start an {@code Application} subclass when JavaFX is not on the module path, which would prevent a plain classpath
 * launch.
 */
public final class Launcher {

    private static final int USAGE_ERROR = 2;

    private Launcher() {
        // no-op
    }

    public static void main(final String[] args) {
        // the icons are drawn through AWT without ever opening a window, nothing should try to reach a display
        System.setProperty("java.awt.headless", "true");
        try {
            ConfigLocator.locate(List.of(args));
        } catch (final IllegalArgumentException e) {
            System.err.println("tcketl: " + e.getMessage());
            System.exit(USAGE_ERROR);
        }
        Application.launch(TcketlApp.class, args);
    }
}
