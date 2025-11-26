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
package org.talend.sdk.component.sample.feature.dynamicdependencies.config;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout(value = {
        @GridLayout.Row({ "groupId", "artifactId", "version", "connectorFamily", "connectorName", "connectorVersion",
                "loadTransitiveDependencies", "connectorConfiguration" })
})
public class Connector implements Serializable {

    @Option
    @Documentation("The connector's group id.")
    private String groupId;

    @Option
    @Documentation("The connector's artifact id.")
    private String artifactId;

    @Option
    @Documentation("The connector's artifact version.")
    private String version;

    @Option
    @Documentation("The connector's family.")
    private String connectorFamily;

    @Option
    @Documentation("The connector's namer.")
    private String connectorName;

    @Option
    @Documentation("The connector's version.")
    private int connectorVersion;

    @Option
    @Documentation("Load transitive dependencies from TALEND-INF/dependencies.txt.")
    private boolean loadTransitiveDependencies;

    @Option
    @Documentation("The connector's configuration.")
    private String connectorConfiguration;

}