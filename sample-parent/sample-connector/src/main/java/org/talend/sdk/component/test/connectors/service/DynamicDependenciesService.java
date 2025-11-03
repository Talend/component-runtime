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
package org.talend.sdk.component.test.connectors.service;

import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.dependency.DynamicDependencies;
import org.talend.sdk.component.test.connectors.config.DynamicDependenciesConf;

@Service
public class DynamicDependenciesService {

    /**
     * This is a fake dynamic depencencies service.
     * It returns 3 times the dependency define in the configuration with version 1.0, 2.0 and 3.0.
     *
     * @param conf
     * @return The list of dynamic dependencies.
     */
    @DynamicDependencies
    public List<String> getDynamicDependencies(@Option("DynDepsConfig") final DynamicDependenciesConf conf) {
        List<String> dependencies = new ArrayList<>();

        for (int version = 1; version <= 3; version++) {
            dependencies.add(String.format("%s:%s:%s.0", conf.getGroup(), conf.getArtifact(), version));
        }

        return dependencies;
    }

}