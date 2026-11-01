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
package org.talend.sdk.component.tcketl.domain.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.talend.sdk.component.tcketl.domain.property.Constraints;
import org.talend.sdk.component.tcketl.domain.property.GridLayout;
import org.talend.sdk.component.tcketl.domain.property.LayoutHints;
import org.talend.sdk.component.tcketl.domain.property.PropertyDefinition;
import org.talend.sdk.component.tcketl.domain.property.PropertyType;

/** Terse builders of hand-made property definitions for the form engine tests. */
final class Props {

    private Props() {
        // test helper
    }

    static PropertyDefinition.Builder object(final String path) {
        return PropertyDefinition.builder(path, PropertyType.OBJECT);
    }

    static PropertyDefinition.Builder array(final String path) {
        return PropertyDefinition.builder(path, PropertyType.ARRAY);
    }

    static PropertyDefinition.Builder string(final String path) {
        return PropertyDefinition.builder(path, PropertyType.STRING);
    }

    static PropertyDefinition.Builder number(final String path) {
        return PropertyDefinition.builder(path, PropertyType.NUMBER);
    }

    static PropertyDefinition.Builder bool(final String path) {
        return PropertyDefinition.builder(path, PropertyType.BOOLEAN);
    }

    static PropertyDefinition.Builder choice(final String path, final String... values) {
        return PropertyDefinition
                .builder(path, PropertyType.ENUM)
                .constraints(Constraints.NONE.withEnumValues(List.of(values)));
    }

    /** Panes written {@code "Main:login,password|ok"}: the name, then the rows separated by {@code |}. */
    static LayoutHints layout(final String... panes) {
        final List<GridLayout> grids = new ArrayList<>();
        for (final String pane : panes) {
            final int colon = pane.indexOf(':');
            final List<List<String>> rows = new ArrayList<>();
            for (final String row : pane.substring(colon + 1).split("\\|")) {
                rows.add(Arrays.asList(row.split(",")));
            }
            grids.add(new GridLayout(pane.substring(0, colon), rows));
        }
        return new LayoutHints(grids, List.of());
    }

    static PropertyTree tree(final PropertyDefinition.Builder... definitions) {
        return PropertyTree.of(Arrays.stream(definitions).map(PropertyDefinition.Builder::build).toList());
    }

    static Map<String, String> map(final String... keysAndValues) {
        final Map<String, String> map = new TreeMap<>();
        for (int index = 0; index < keysAndValues.length; index += 2) {
            map.put(keysAndValues[index], keysAndValues[index + 1]);
        }
        return map;
    }
}
