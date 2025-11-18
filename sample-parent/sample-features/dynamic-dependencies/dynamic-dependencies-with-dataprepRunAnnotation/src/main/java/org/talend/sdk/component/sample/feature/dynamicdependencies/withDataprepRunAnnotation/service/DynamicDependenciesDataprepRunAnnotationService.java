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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation.service;

import java.io.Serializable;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.sample.feature.dynamicdependencies.service.AbstractDynamicDependenciesService;
import org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation.config.Config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DynamicDependenciesDataprepRunAnnotationService extends AbstractDynamicDependenciesService
        implements Serializable {

    public final static String DEPENDENCY_WITHDATAPREPRUN_ACTION = "DEPENDENCY_WITHDATAPREPRUN_ACTION";

    public static final String DEPENDENCY_ACTION = "dataprep-dependencies";

    @DynamicDependencies(DEPENDENCY_ACTION)
    public List<String> getDynamicDependencies(@Option("theConfig") final Config config) {
        return super.getDynamicDependencies(config.getDependencies(), config.getConnectors());
    }

    @DiscoverSchemaExtended(DEPENDENCY_WITHDATAPREPRUN_ACTION)
    public Schema guessSchema4Input(final @Option("configuration") Config config) {
        return super.buildSchema(config);
    }

}