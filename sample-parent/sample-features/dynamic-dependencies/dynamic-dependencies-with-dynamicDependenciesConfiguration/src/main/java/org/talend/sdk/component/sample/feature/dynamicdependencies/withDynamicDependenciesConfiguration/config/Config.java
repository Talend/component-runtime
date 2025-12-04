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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withDynamicDependenciesConfiguration.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Connector;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dependency;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.DynamicDependencyConfig;

import lombok.Data;

/**
 * For this sample, the same configuration is used for all connectors input/processor/output.
 */
@Data
@GridLayout({
        @GridLayout.Row({ "dse" }),
        @GridLayout.Row({ "subConfig" }),
        @GridLayout.Row({ "environmentInformation" })
})
@GridLayout(names = GridLayout.FormType.ADVANCED, value = {
        @GridLayout.Row({ "dse" }),
        @GridLayout.Row({ "dieOnError" }),
})
public class Config implements DynamicDependencyConfig, Serializable {

    @Option
    @Documentation("The dataset configuration.")
    private Dataset dse = new Dataset();

    @Option
    @Documentation("Sub-configuration that contains the DynamidDependenciesConfiguration.")
    private SubConfig subConfig = new SubConfig();

    @Option
    @Documentation("If enable throw an exception for any error, if not just log the error.")
    private boolean dieOnError = false;

    @Option
    @Documentation("More environment information.")
    private boolean environmentInformation = false;

    @Override
    public List<Dependency> getDependencies() {
        return new ArrayList<>(this.getSubConfig().getDependencies());
    }

    public List<Connector> getConnectors() {
        return new ArrayList<>(this.getSubConfig().getConnectors());
    }

}