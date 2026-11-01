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
package org.talend.sdk.component.tcketl.app.palette;

import javafx.scene.input.DataFormat;

/** What a palette entry carries while it is dragged to the canvas: the id of its connector, as text. */
public final class PaletteTransfer {

    // a format can be registered only once per JVM, so there is exactly one instance
    public static final DataFormat CONNECTOR = new DataFormat("application/x-tcketl-connector");

    private PaletteTransfer() {
    }
}
