/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Processor(family = "checkpoint", name = "storage")
public class StoreRecordInMemoryProcessor implements Serializable, Supplier<List<Record>> {

    private static final Map<String, List<Record>> cache = Collections.synchronizedMap(new HashMap<>());

    private final StoreRecordInMemoryProcessorConfig config;

    @Data
    public static class StoreRecordInMemoryProcessorConfig {

        @Option
        private String id;

    }

    @ElementListener
    public void onNext(@Input final Record value) {
        List<Record> records = cache.computeIfAbsent(this.config.id, s -> new ArrayList<>());
        records.add(value);
    }

    @Override
    public List<Record> get() {
        return cache.getOrDefault(this.getConfig().getId(), Collections.emptyList());
    }
}
