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
package org.talend.sdk.component.tcketl.app.ui;

import javafx.scene.layout.Region;

import org.talend.sdk.component.tcketl.domain.connector.ConnectorRole;

/**
 * A small shape that tells the role of a connector apart without relying on a colour only: a triangle for an input,
 * a diamond for a processor, a square for an output and a circle for a standalone connector. The look comes from
 * the style classes {@code role-glyph}, {@code input}, {@code processor}, {@code output} and {@code standalone};
 * {@code provisional} marks a role that is only a guess of the index.
 */
public final class RoleGlyph extends Region {

    private static final String BASE_STYLE = "role-glyph";

    private static final String PROVISIONAL_STYLE = "provisional";

    public RoleGlyph() {
        getStyleClass().add(BASE_STYLE);
    }

    public static String label(final ConnectorRole role) {
        switch (role) {
            case INPUT:
                return "Input";
            case PROCESSOR:
                return "Processor";
            case OUTPUT:
                return "Output";
            default:
                return "Standalone";
        }
    }

    public void show(final ConnectorRole role, final boolean provisional) {
        getStyleClass().setAll(BASE_STYLE, styleOf(role));
        if (provisional) {
            getStyleClass().add(PROVISIONAL_STYLE);
        }
    }

    private static String styleOf(final ConnectorRole role) {
        switch (role) {
            case INPUT:
                return "input";
            case PROCESSOR:
                return "processor";
            case OUTPUT:
                return "output";
            default:
                return "standalone";
        }
    }
}
