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
package org.talend.sdk.component.sample.feature.availableoutput.source;

import static java.util.Collections.singletonList;
import org.talend.sdk.component.api.component.Icon;
import static org.talend.sdk.component.api.component.Icon.IconType.CUSTOM;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.sample.feature.availableoutput.service.AvaiableoutputService;

import java.io.Serializable;
import java.util.List;

//
// this class role is to enable the work to be distributed in environments supporting it.
//
@Version(1)
// default version is 1, if some configuration changes happen between 2 versions you can add a migrationHandler
@Icon(value = CUSTOM, custom = "AvailableOutputsInput")
// icon is located at src/main/resources/icons/AvailableOutputsInput.svg
@PartitionMapper(name = "AvailableOutputsInput")
@Documentation("TODO fill the documentation for this mapper.")
public class AvailableOutputsInputMapper implements Serializable {
    private final AvailableOutputsInputMapperConfiguration configuration;
    private final AvaiableoutputService service;
    private final RecordBuilderFactory recordBuilderFactory;

    public AvailableOutputsInputMapper(@Option("configuration") final AvailableOutputsInputMapperConfiguration configuration,
                                       final AvaiableoutputService service,
                                       final RecordBuilderFactory recordBuilderFactory) {
        this.configuration = configuration;
        this.service = service;
        this.recordBuilderFactory = recordBuilderFactory;
    }

    @Assessor
    public long estimateSize() {
        // this method should return the estimation of the dataset size
        // it is recommended to return a byte value
        // if you don't have the exact size you can use a rough estimation
        return 1L;
    }

    @Split
    public List<AvailableOutputsInputMapper> split(@PartitionSize final long bundles) {
        // overall idea here is to split the work related to configuration in bundles of size "bundles"
        //
        // for instance if your estimateSize() returned 1000 and you can run on 10 nodes
        // then the environment can decide to run it concurrently (10 * 100).
        // In this case bundles = 100 and we must try to return 10 AvailableOutputsInputMapper with 1/10 of the overall work each.
        //
        // default implementation returns this which means it doesn't support the work to be split
        return singletonList(this);
    }

    @Emitter
    public AvailableOutputsInputSource createWorker() {
        // here we create an actual worker,
        // you are free to rework the configuration etc but our default generated implementation
        // propagates the partition mapper entries.
        return new AvailableOutputsInputSource(configuration, service, recordBuilderFactory);
    }
}