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

public class CustomizeClassLoader implements ComponentManager.Customizer {

    @Override
    public Stream<String> containerClassesAndPackages() {
        return Stream.of(
                // Implementation should come from a dynamic dependency
                "org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.serviceInterfaces.StringsProviderSPIAsDependency",
                // Implementation should come from runtime
                "org.talend.sdk.component.sample.feature.dynamicdependencies.classloadertestlibrary.serviceInterfaces.StringsProviderSPIAsDynamicDependency");
    }
}