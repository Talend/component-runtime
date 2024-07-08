/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.source;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.dataset.TCKDataset;

@GridLayout({
        // the generated layout put one configuration entry per line,
        // customize it as much as needed
        @GridLayout.Row({ "dataset" })
})
@Documentation("Input configuration for TCK validator")
public class TCKInputMapperConfiguration implements Serializable {

    @Option
    @Documentation("Dataset for TCK validator")
    private TCKDataset dataset;

    public TCKDataset getDataset() {
        return dataset;
    }

    public TCKInputMapperConfiguration setDataset(final TCKDataset dataset) {
        this.dataset = dataset;
        return this;
    }
}