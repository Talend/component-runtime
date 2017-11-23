/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.junit;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * IMPORTANT: all entries of the map but have the same "size".
 */
public class JoinInputFactory implements ControllableInputFactory {

    private final Map<String, Iterator<? extends Serializable>> data = new HashMap<>();

    public JoinInputFactory withInput(final String branch, final Collection<? extends Serializable> branchData) {
        data.put(branch, branchData.iterator());
        return this;
    }

    @Override
    public Object read(final String name) {
        final Iterator<?> iterator = data.get(name);
        return iterator != null && iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public boolean hasMoreData() {
        return !data.isEmpty() && data.entrySet().stream().allMatch(e -> e.getValue().hasNext());
    }

    @Override
    public InputFactoryIterable asInputRecords() {
        return new InputFactoryIterable(this, data);
    }
}
