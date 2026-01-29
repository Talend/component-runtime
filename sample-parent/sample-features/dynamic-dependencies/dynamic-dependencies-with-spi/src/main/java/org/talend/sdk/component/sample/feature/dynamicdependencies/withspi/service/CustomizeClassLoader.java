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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.service;

import java.util.stream.Stream;

import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomizeClassLoader implements ComponentManager.Customizer {

    private static final String DISABLE_CUSTOMIZE_PROPERTY =
            "talend.sample.feature.dynamicdependencies.withspi.CustomizeClassLoader.disabled";

    private static final boolean DISABLE_CUSTOMIZE = Boolean.parseBoolean(
            System.getProperty(DISABLE_CUSTOMIZE_PROPERTY, "true"));

    @Override
    public Stream<String> containerClassesAndPackages() {
        if (DISABLE_CUSTOMIZE) {
            log.info(
                    "org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.service.CustomizeClassLoader is disabled.\n"
                            + "use \"" + DISABLE_CUSTOMIZE_PROPERTY + "=false\""
                            + " property to enable it.");
            return Stream.empty();
        }

        log.info(
                "org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.service.CustomizeClassLoader is enable,\n"
                        + "use \"" + DISABLE_CUSTOMIZE_PROPERTY + "=true\""
                        + " property to disable it.");
        return Stream.of(
                // Implementation should come from a dynamic dependency
                "org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.serviceInterfaces.StringProviderSPIAsDependency",
                // Implementation should come from runtime
                "org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.serviceInterfaces.StringProviderSPIAsDynamicDependency");
    }
}