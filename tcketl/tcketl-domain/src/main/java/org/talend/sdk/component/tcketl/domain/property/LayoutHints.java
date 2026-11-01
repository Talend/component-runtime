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
package org.talend.sdk.component.tcketl.domain.property;

import java.util.List;
import java.util.Optional;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * How an object property arranges its children: named grid panes in the order the server listed them, and/or a plain
 * field order used when there is no grid.
 */
public record LayoutHints(List<GridLayout> grids, List<String> optionsOrder) {

    public static final LayoutHints NONE = new LayoutHints(List.of(), List.of());

    public LayoutHints {
        grids = Preconditions.immutableCopy(grids, "grids");
        optionsOrder = Preconditions.distinctNonBlank(optionsOrder, "optionsOrder");
    }

    /** Pane lookup ignoring case, like the reference form renderer does. */
    public Optional<GridLayout> grid(final String name) {
        return grids.stream().filter(grid -> grid.name().equalsIgnoreCase(name)).findFirst();
    }

    public boolean isEmpty() {
        return grids.isEmpty() && optionsOrder.isEmpty();
    }
}
