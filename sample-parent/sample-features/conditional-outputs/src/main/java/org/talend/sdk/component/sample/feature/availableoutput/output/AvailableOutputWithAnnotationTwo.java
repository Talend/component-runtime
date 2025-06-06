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
package org.talend.sdk.component.sample.feature.availableoutput.output;

import static org.talend.sdk.component.api.component.Icon.IconType.CUSTOM;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.meta.ConditionalOutput;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.sample.feature.availableoutput.service.AvailableOutputService;

@Version(1)
// default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler
@Icon(value = CUSTOM, custom = "AvailableOutputsOutput")
// icon is located at src/main/resources/icons/AvailableOutputsOutput.svg
@Processor(family = "sampleAvailableOutput", name = "Output2")
@Documentation("Sample for Available output flows.")
@ConditionalOutput("output-flow2")
public class AvailableOutputWithAnnotationTwo implements Serializable {

    private final ConfigurationTwo configuration;

    private final AvailableOutputService service;

    public AvailableOutputWithAnnotationTwo(@Option("config") final ConfigurationTwo configuration,
            final AvailableOutputService service) {
        this.configuration = configuration;
        this.service = service;
    }

    @PostConstruct
    public void init() {
        // this method will be executed once for the whole component execution,
        // this is where you can establish a connection for instance
        // Note: if you don't need it you can delete it
    }

    @ElementListener
    public void process(@Input final Record input,
            @Output final OutputEmitter<Record> main,
            @Output(AvailableOutputService.SECOND_FLOW_NAME2) final OutputEmitter<Record> second,
            @Output(AvailableOutputService.THIRD_FLOW_NAME2) final OutputEmitter<Record> third) {
        main.emit(input);
        second.emit(input);
        third.emit(input);
    }

}