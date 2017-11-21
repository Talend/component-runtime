/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit;

import java.util.Iterator;
import java.util.Map;

import lombok.Data;

/**
 * IMPORTANT: all entries of the map but have the same "size".
 */
@Data
public class JoinInputFactory implements ControllableInputFactory {
    private final Map<String, Iterator<?>> data;

    @Override
    public Object read(final String name) {
        final Iterator<?> iterator = data.get(name);
        return iterator != null && iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public boolean hasMoreData() {
        return data.entrySet().stream().allMatch(e -> e.getValue().hasNext());
    }
}
