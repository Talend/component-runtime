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
package org.talend.sdk.component.runtime.di;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.bind.Jsonb;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class BaseIOHandler {

    protected final Jsonb jsonb;

    protected final Map<String, IO> connections = new HashMap<>();

    public void addConnection(final String name, final Class<?> type) {
        connections.put(getActualName(name), new IO(new AtomicReference<>(), type));
    }

    public void reset() {
        connections.values().forEach(r -> {
            try {
                r.value.set(r.type.getConstructor().newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("Can't create an instance of " + r.type, e);
            }
        });
    }

    public <T> T getValue(final String name, final Class<T> type) {
        final String actualName = getActualName(name);
        return type.cast(connections.get(actualName).value.get());
    }

    protected final String getActualName(final String name) {
        return "__default__".equalsIgnoreCase(name) ? "flow" : name.toLowerCase(Locale.ROOT);
    }

    @AllArgsConstructor
    @Data
    static class IO<T> {

        private final AtomicReference<T> value;

        private final Class<T> type;

    }

}
