/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordConverters;

/**
 * An input factory which joins multiple distinct sources reading them in "parallel".
 *
 * IMPORTANT: all entries of the map but have the same "size".
 */
public class JoinInputFactory implements ControllableInputFactory {

    private final Map<String, Iterator<?>> data = new HashMap<>();

    private volatile Jsonb jsonb;

    private volatile RecordConverters.MappingMetaRegistry registry;

    public JoinInputFactory withInput(final String branch, final Collection<?> branchData) {
        data.put(branch, branchData.iterator());
        return this;
    }

    @Override
    public Object read(final String name) {
        final Iterator<?> iterator = data.get(name);
        if (iterator != null && iterator.hasNext()) {
            return map(iterator.next());
        }
        return null;
    }

    @Override
    public boolean hasMoreData() {
        final boolean hasMore = !data.isEmpty() && data.entrySet().stream().allMatch(e -> e.getValue().hasNext());
        if (!hasMore && jsonb != null) {
            synchronized (this) {
                if (jsonb != null) {
                    try {
                        jsonb.close();
                    } catch (final Exception e) {
                        // no-op: not important here
                    }
                }
            }
        }
        return hasMore;
    }

    @Override
    public InputFactoryIterable asInputRecords() {
        return new InputFactoryIterable(this, data);
    }

    private Object map(final Object next) {
        if (next == null || Record.class.isInstance(next)) { // directly ok
            return next;
        }

        if (String.class.isInstance(next) || next.getClass().isPrimitive()) { // primitives
            return next;
        }

        if (jsonb == null) {
            synchronized (this) {
                if (jsonb == null) {
                    jsonb = JsonbBuilder.create(new JsonbConfig().setProperty("johnzon.cdi.activated", false));
                    registry = new RecordConverters.MappingMetaRegistry();
                }
            }
        }

        return new RecordConverters()
                .toRecord(registry, next, () -> jsonb,
                        () -> ComponentManager.instance().getRecordBuilderFactoryProvider().apply(null));
    }
}
