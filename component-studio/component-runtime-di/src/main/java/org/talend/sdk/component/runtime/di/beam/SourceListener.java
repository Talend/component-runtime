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
package org.talend.sdk.component.runtime.di.beam;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

public interface SourceListener extends Serializable {

    Map<String, Tracker> TRACKERS = new ConcurrentHashMap<>();

    void onSplit(int count);

    void onElement();

    void onReaderClose();

    @Data
    class Tracker {

        private final String id = UUID.randomUUID().toString();

        private final AtomicInteger active = new AtomicInteger(0);
    }

    @Data
    @Slf4j
    @NoArgsConstructor
    @AllArgsConstructor
    class StateReleaserSourceListener implements SourceListener {

        private String trackerId;

        private String stateId;

        private volatile LoopState state;

        private volatile Tracker tracker;

        @Override
        public void onSplit(final int count) {
            getTracker().getActive().addAndGet(count);
        }

        @Override
        public void onElement() {
            getState().getRecordCount().incrementAndGet();
        }

        @Override
        public void onReaderClose() {
            final int stillActive = getTracker().getActive().decrementAndGet();
            log.debug("Active sources: {}", stillActive);
            if (stillActive == 0) {
                TRACKERS.remove(trackerId);

                final LoopState state = getState();
                state.done();
                if (state.getRecordCount().get() == 0) {
                    state.end();
                }
            }
        }

        private Tracker getTracker() {
            return tracker == null ? tracker = TRACKERS.get(trackerId) : tracker;
        }

        private synchronized LoopState getState() {
            return state == null ? state = LoopState.lookup(stateId) : state;
        }
    }
}
