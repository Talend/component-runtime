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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withDynamicDependenciesConfiguration.service;

import java.io.Serializable;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.sample.feature.dynamicdependencies.service.AbstractDynamicDependenciesService;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withDynamicDependenciesConfiguration.config.Config;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withDynamicDependenciesConfiguration.config.SubConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DynamicDependenciesWithDynamicependenciesConfigurationService extends AbstractDynamicDependenciesService
        implements Serializable {

    public final static String DEPENDENCY_WITHDYNDEPSCONFIG_ACTION = "DEPENDENCY_WITHDYNDEPSCONFIG_ACTION";

    @DynamicDependencies()
    public List<String> getDynamicDependencies(@Option("theSubConfig") final SubConfig subConfig) {
        return super.getDynamicDependencies(subConfig.getDependencies(), subConfig.getConnectors());
    }

    @DiscoverSchemaExtended(DEPENDENCY_WITHDYNDEPSCONFIG_ACTION)
    public Schema guessSchema4Input(final @Option("configuration") Config config) {
        return super.buildSchema(config);
    }

}