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
package org.talend.sdk.component.sample.feature.databasemapping.config;

import java.io.Serializable;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.Structure;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

/**
 * For this sample, the same configuration is used for all connectors input/processor/output.
 */
@Data
@GridLayout({
        @GridLayout.Row("schema"),
        @GridLayout.Row({ "dse" }),
        @GridLayout.Row({ "aBoolean" })
})
public class Config implements Serializable {

    @Option
    @Structure(type = Structure.Type.OUT)
    @Documentation("Schema.")
    private List<SchemaInfo> schema;

    @Option
    @Documentation("The dataset configuration.")
    private Dataset dse = new Dataset();

    @Option
    @Documentation("A boolean.")
    private boolean aBoolean;

}
