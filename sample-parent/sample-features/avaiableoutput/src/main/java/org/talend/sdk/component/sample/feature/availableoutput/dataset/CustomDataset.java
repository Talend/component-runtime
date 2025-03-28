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
package org.talend.sdk.component.sample.feature.availableoutput.dataset;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.sample.feature.availableoutput.datastore.CustomDatastore;

@DataSet("CustomDataset")
@GridLayout({
        // the generated layout put one configuration entry per line,
        // customize it as much as needed
        @GridLayout.Row({ "datastore" }),
        @GridLayout.Row({ "anotherInput" })
})
@Documentation("TODO fill the documentation for this configuration.")
public class CustomDataset implements Serializable {

    @Option
    @Documentation("TODO fill the documentation for this parameter.")
    private CustomDatastore datastore;

    @Option
    @Documentation("TODO fill the documentation for this parameter.")
    private String anotherInput;

    public CustomDatastore getDatastore() {
        return datastore;
    }

    public CustomDataset setDatastore(final CustomDatastore datastore) {
        this.datastore = datastore;
        return this;
    }

    public String getAnotherInput() {
        return anotherInput;
    }

    public CustomDataset setAnotherInput(final String anotherInput) {
        this.anotherInput = anotherInput;
        return this;
    }
}