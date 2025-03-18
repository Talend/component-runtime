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

import org.talend.sdk.component.api.component.Icon;
import static org.talend.sdk.component.api.component.Icon.IconType.CUSTOM;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.widget.ConditionalOutputFlows;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.processor.ElementListener;
import org.talend.sdk.component.api.processor.Input;
import org.talend.sdk.component.api.processor.Output;
import org.talend.sdk.component.api.processor.OutputEmitter;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.sample.feature.availableoutput.service.AvaiableoutputService;

import javax.annotation.PostConstruct;
import java.io.Serializable;

@Version(1)
// default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler
@Icon(value = CUSTOM, custom = "AvailableOutputsOutput")
// icon is located at src/main/resources/icons/AvailableOutputsOutput.svg
@Processor(family = "sampleAvailableOutput", name = "AvailableOutputsOutput1")
@Documentation("Sample for Available output flows.")
@ConditionalOutputFlows("output-flow1")
public class AvailableOutput1 implements Serializable {
    private final Configuration configuration;
    private final AvaiableoutputService service;

    public AvailableOutput1(@Option("configuration") final Configuration configuration,
                            final AvaiableoutputService service) {
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
                        @Output(AvaiableoutputService.SECOND_FLOW_NAME) final OutputEmitter<Record> second,
                        @Output(AvaiableoutputService.THIRD_FLOW_NAME) final OutputEmitter<Record> third) {
    }

}