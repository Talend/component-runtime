/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.documentation.sample.datastorevalidation.components.source;

import java.io.Serializable;

import org.talend.sdk.component.documentation.sample.datastorevalidation.components.dataset.DatasetA;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@GridLayout({
        // the generated layout put one configuration entry per line,
        // customize it as much as needed
        @GridLayout.Row({ "dataset" }) })
@Documentation("TODO fill the documentation for this configuration")
public class DataStoreValidationInputMapperConfiguration implements Serializable {

    @Option
    @Documentation("TODO fill the documentation for this parameter")
    private DatasetA dataset;

    public DatasetA getDataset() {
        return dataset;
    }

    public DataStoreValidationInputMapperConfiguration setDataset(DatasetA dataset) {
        this.dataset = dataset;
        return this;
    }
}