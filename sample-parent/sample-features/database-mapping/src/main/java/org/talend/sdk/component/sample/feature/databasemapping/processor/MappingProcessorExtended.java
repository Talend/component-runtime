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
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.sample.feature.databasemapping.config.Config;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Processor(name = "ProcessorExtended")
@DatabaseMapping(value = "custom", mapping = "processor_mapping_extended")
@Documentation("Database Mapping sample processor connector with config.")
public class MappingProcessorExtended extends MappingProcessor implements Serializable {

    public MappingProcessorExtended(final Config config, final RecordBuilderFactory factory) {
        super(config, factory);
    }
}
