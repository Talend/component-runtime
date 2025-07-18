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
package org.talend.sdk.component.sample.feature.databasemapping.processor;

import java.io.Serializable;

import org.talend.sdk.component.api.component.DatabaseMapping;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.sample.feature.databasemapping.config.Config;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Processor(name = "Processor")
@DatabaseMapping(value = "custom", mapping = "processor_mapping")
@Documentation("Database Mapping sample processor connector.")
public class MappingProcessor implements Serializable {

    private final Config config;

    private final RecordBuilderFactory factory;

    public MappingProcessor(final Config config, final RecordBuilderFactory factory) {
        this.config = config;
        this.factory = factory;
    }

    @ElementListener
    public void process(@Input final Record input, @Output final OutputEmitter<Record> main) {
        final int id = input.getOptionalInt("id").orElse(0);
        final Record r = factory.newRecordBuilder()
                .withInt("id", id)
                .withString("input", "modulo == 0")
                .withString("another", "Fixed value")
                .withBoolean("aBoolean", true)
                .build();
        main.emit(r);
    }

}
