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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.input;

import java.io.Serializable;
import java.util.Iterator;

import javax.annotation.PostConstruct;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.config.Config;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.service.DynamicDependenciesWithSPIService;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Emitter(name = "Input")
@Documentation("Dynamic dependencies sample input connector.")
public class DynamicDependenciesWithSPIInput implements Serializable {

    private final Config config;

    private final DynamicDependenciesWithSPIService service;

    private Iterator<Record> recordIterator;

    public DynamicDependenciesWithSPIInput(final Config config,
            final DynamicDependenciesWithSPIService service) {
        this.config = config;
        this.service = service;
    }

    @PostConstruct
    public void init() {
        this.recordIterator = this.service.getRecordIterator();
    }

    @Producer
    public Record next() {
        if (recordIterator == null || !recordIterator.hasNext()) {
            return null;
        }

        return recordIterator.next();
    }

}