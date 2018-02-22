/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio.model.parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@RequiredArgsConstructor
@ToString(exclude = "levels")
public class Layout {

    private final String path;

    private int position;

    private int height;

    private final List<Level> levels = new ArrayList<>();

    public boolean isLeaf() {
        return levels.isEmpty();
    }

    /**
     * Returns child Layout
     * 
     * @param path child Layout path
     * @return child Layout
     */
    Layout getChildLayout(final String path) {
        Objects.requireNonNull(path);
        return levels
                .stream()
                .flatMap(l -> l.getColumns().stream())
                .filter(c -> path.equals(c.getPath()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no Layout for path " + path));
    }

}
