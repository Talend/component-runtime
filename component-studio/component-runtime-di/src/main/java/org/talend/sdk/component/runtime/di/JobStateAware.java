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
package org.talend.sdk.component.runtime.di;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;

// way for beam integration - and more generally di integration
// to access shared state between components
public interface JobStateAware {

    // for dependency management mainly, should be part of State directly otherwise
    enum IndirectInstances {
        Pipeline
    }

    void setState(final State state);

    class State {

        @Getter
        private final AtomicBoolean pipelineStarted = new AtomicBoolean(false);

        @Getter
        private final CompletableFuture<Boolean> pipelineDone = new CompletableFuture<>();

        // for loose typing on beam components (optional for now)
        private final Map<IndirectInstances, Object> optionalInstances = new HashMap<>();

        public void set(final IndirectInstances key, final Object instance) {
            optionalInstances.put(key, instance);
        }

        public <T> T get(final IndirectInstances key, final Class<T> instance) {
            return instance.cast(optionalInstances.get(key));
        }
    }

    static void init(final Object instance, final Map<String, Object> globalMap) {
        if (JobStateAware.class.isInstance(instance)) {
            synchronized (globalMap) {
                final State state =
                        State.class.cast(globalMap.computeIfAbsent(JobStateAware.class.getName(), k -> new State()));
                JobStateAware.class.cast(instance).setState(state);
            }
        }
    }
}
