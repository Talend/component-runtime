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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * One named pane (a tab) of an object: rows of child names, each row listing its cells from left to right. Cells are
 * names relative to the object, not paths.
 */
public record GridLayout(String name, List<List<String>> rows) {

    public GridLayout {
        Preconditions.requireNonBlank(name, "name");
        Preconditions.requireNonNull(rows, "rows");
        final List<List<String>> copy = new ArrayList<>(rows.size());
        for (final List<String> row : rows) {
            final List<String> cells = Preconditions.distinctNonBlank(row, "row");
            if (cells.isEmpty()) {
                throw new IllegalArgumentException("a row must have at least one cell");
            }
            copy.add(cells);
        }
        rows = Collections.unmodifiableList(copy);
    }

    /** Every cell, row after row. */
    public List<String> cells() {
        return rows.stream().flatMap(List::stream).collect(Collectors.toUnmodifiableList());
    }
}
