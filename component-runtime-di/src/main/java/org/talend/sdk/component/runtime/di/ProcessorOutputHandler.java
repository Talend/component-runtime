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
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonValue;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.runtime.output.OutputFactory;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessorOutputHandler {

    private final Jsonb jsonb;

    private final Map<String, Output> outputs = new HashMap<>();

    public void addOutput(final String name, final Class<?> type) {
        outputs.put(name, new Output<>(new AtomicReference<>(), type));
    }

    public void reset() {
        outputs.values().forEach(r -> r.value.set(null));
    }

    public <T> T getValue(final String name, final Class<T> type) {
        return type.cast(outputs.get(name).value.get());
    }

    public OutputFactory asOutputFactory() {
        return name -> value -> {
            final Output ref = outputs.get(name);
            if (ref != null) {
                final String jsonValue = JsonValue.class.isInstance(value) ? JsonValue.class.cast(value).toString()
                        : jsonb.toJson(value);
                ref.value.set(jsonb.fromJson(jsonValue, ref.type));
            }
        };
    }

    @AllArgsConstructor
    private static class Output<T> {

        private final AtomicReference<T> value;

        private final Class<T> type;
    }
}
