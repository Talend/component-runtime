/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.di;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordConverters;

import lombok.RequiredArgsConstructor;

public abstract class BaseIOHandler {

    protected final Jsonb jsonb;

    protected final RecordBuilderFactory recordBuilderMapper;

    protected final RecordConverters converters;

    protected final Map<String, IO> connections = new TreeMap<>();

    public BaseIOHandler(final Jsonb jsonb, final Map<Class<?>, Object> servicesMapper) {
        this.jsonb = jsonb;
        this.recordBuilderMapper = (RecordBuilderFactory) servicesMapper.get(RecordBuilderFactory.class);
        this.converters = new RecordConverters();
    }

    public void init(final Collection<String> branchesOrder) {
        if (branchesOrder == null) {
            return;
        }

        final Map<String, String> mapping = new HashMap<>(); // temp structure to avoid concurrent modification
        final Iterator<String> branches = branchesOrder.iterator();
        for (final String rowStruct : connections.keySet()) {
            if (!branches.hasNext()) {
                break;
            }
            mapping.put(rowStruct, branches.next());
        }
        if (!mapping.isEmpty()) {
            mapping.forEach((row, branch) -> connections.putIfAbsent(branch, connections.get(row)));
        }
    }

    public void addConnection(final String connectorName, final Class<?> type) {
        connections.put(connectorName, new IO<>(type));
    }

    public void reset() {
        connections.values().forEach(IO::reset);
    }

    public <T> T getValue(final String connectorName) {
        return (T) connections.get(connectorName).next();
    }

    public boolean hasMoreData() {
        return connections.entrySet().stream().anyMatch(e -> e.getValue().hasNext());
    }

    protected String getActualName(final String name) {
        return "__default__".equals(name) ? "FLOW" : name;
    }

    @RequiredArgsConstructor
    static class IO<T> {

        private final Queue<T> values = new LinkedList<>();

        private final Class<T> type;

        private void reset() {
            values.clear();
        }

        boolean hasNext() {
            return values.size() != 0;
        }

        T next() {
            if (hasNext()) {
                return type.cast(values.poll());
            }

            return null;
        }

        void add(final T e) {
            values.add(e);
        }

        Class<T> getType() {
            return type;
        }
    }

}
