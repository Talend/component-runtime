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
package org.talend.sdk.component.sample.feature.databasemapping.input;

import java.io.Serializable;

import org.talend.sdk.component.api.component.DatabaseMapping;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.sample.feature.databasemapping.config.Config;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Emitter(name = "Input")
@DatabaseMapping("postgres")
@Documentation("Database Mapping sample input connector.")
public class MappingInput implements Serializable {

    private final Config config;

    private final RecordBuilderFactory factory;

    private static final int MAX = 5;

    private int current = 0;

    public MappingInput(final Config config, final RecordBuilderFactory factory) {
        this.config = config;
        this.factory = factory;
    }

    @Producer
    public Record next() {
        if (current >= MAX) {
            return null;
        }
        current++;
        return factory.newRecordBuilder()
                .withInt("id", current)
                .withString("input", this.config.getDse().getDso().getBackend().getKey())
                .withString("another", this.config.getDse().getAnotherInput())
                .withBoolean("aBoolean", this.config.isABoolean())
                .build();
    }

}
