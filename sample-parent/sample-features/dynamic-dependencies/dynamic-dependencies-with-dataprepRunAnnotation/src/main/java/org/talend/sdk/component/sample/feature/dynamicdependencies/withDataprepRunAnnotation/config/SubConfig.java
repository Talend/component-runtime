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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withDataprepRunAnnotation.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Connector;
import org.talend.sdk.component.sample.feature.dynamicdependencies.config.Dependency;

import lombok.Data;

@Data
@GridLayout({
        @GridLayout.Row({ "dependencies" }),
        @GridLayout.Row({ "connectors" })
})
public class SubConfig implements Serializable {

    @Option
    @Documentation("The dependencies to load dynamically.")
    private List<Dependency> dependencies = new ArrayList<>();

    @Option
    @Documentation("The connectors to load dynamically.")
    private List<Connector> connectors = new ArrayList<>();

}