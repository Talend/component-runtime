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
package org.talend.sdk.component.sample.feature.dynamicdependencies.withspi.config;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

/**
 * For this sample, the same configuration is used for all connectors input/processor/output.
 */
@Data
@GridLayout({
        @GridLayout.Row({ "dse" }),
})
@GridLayout(names = GridLayout.FormType.ADVANCED, value = {
        @GridLayout.Row({ "dse" }),
        @GridLayout.Row({ "dieOnError" }),
})
public class Config implements Serializable {

    @Option
    @Documentation("The dataset configuration.")
    private Dataset dse = new Dataset();

    @Option
    @Documentation("If enable throw an exception for any error, if not just log the error.")
    private boolean dieOnError = false;

}