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
package org.talend.sdk.component.sample.feature.availableoutput.output;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.sample.feature.availableoutput.dataset.CustomDataset;

import java.io.Serializable;

@GridLayout({
        // the generated layout put one configuration entry per line,
        // customize it as much as needed
        @GridLayout.Row({"dataset"}),
        @GridLayout.Row({"showSecond"}),
        @GridLayout.Row({"showThird"})
})
@Documentation("TODO fill the documentation for this configuration")
public class Configuration implements Serializable {
    @Option
    @Documentation("Dataset.")
    private CustomDataset dataset;

    @Option
    @Documentation("Show second output or not.")
    private boolean showSecond;

    @Option
    @Documentation("Show third output or not.")
    private boolean showThird;

    public CustomDataset getDataset() {
        return dataset;
    }

    public Configuration setDataset(final CustomDataset dataset) {
        this.dataset = dataset;
        return this;
    }

    public boolean getShowSecond() {
        return showSecond;
    }

    public Configuration setShowSecond(final boolean showSecond) {
        this.showSecond = showSecond;
        return this;
    }

    public boolean getShowThird() {
        return showThird;
    }

    public Configuration setShowThird(final boolean showThird) {
        this.showThird = showThird;
        return this;
    }
}