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
package org.talend.sdk.component.sample.feature.fixedschema.processor;

import java.io.Serializable;

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
import org.talend.sdk.component.api.service.schema.FixedSchema;
import org.talend.sdk.component.sample.feature.fixedschema.config.Config;
import org.talend.sdk.component.sample.feature.fixedschema.service.UIService;

@Version
@Processor(name = "Processor")
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Documentation("Fixed schema sample processor connector.")
@FixedSchema(value = "fixedschemaextended", flows = { "__default__", "second" })
public class FixedSchemaProcessor implements Serializable {

    private final Config config;

    private final RecordBuilderFactory factory;

    public FixedSchemaProcessor(final Config config, final RecordBuilderFactory factory) {
        this.config = config;
        this.factory = factory;
    }

    @ElementListener
    public void process(@Input final Record input,
            @Output final OutputEmitter<Record> main,
            @Output(UIService.SECOND_FLOW_NAME) final OutputEmitter<Record> second,
            @Output(UIService.THIRD_FLOW_NAME) final OutputEmitter<Record> third) {
        final int id = input.getOptionalInt("id").orElse(0);

        final int modulo = id % 3;
        if (modulo == 0) {
            final Record zero = factory.newRecordBuilder()
                    .withInt("id", id)
                    .withString("input", "modulo == 0")
                    .withString("another", "Fixed value")
                    .withBoolean("aBoolean", true)
                    .build();
            main.emit(zero);
        } else if (modulo == 1) {
            final Record one = factory.newRecordBuilder()
                    .withInt("second", id)
                    .withString("flow", "Second flow since module == 1")
                    .build();
            second.emit(one);
        } else {
            final Record three = factory.newRecordBuilder()
                    .withString(config.getDse().getDso().getInput(), String.valueOf(id))
                    .withString(config.getDse().getAnotherInput(), "Third flow since module == 2")
                    .build();
            third.emit(three);
        }

    }

}
