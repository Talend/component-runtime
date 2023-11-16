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
package org.talend.sdk.component.documentation.sample.dropdownlist.components.processor;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@GridLayout({
        // the generated layout put one configuration entry per line,
        // customize it as much as needed
        @GridLayout.Row({ "action1" }), @GridLayout.Row({ "action2" }) })
@Documentation("TODO fill the documentation for this configuration")
public class DropdownlistProcessorProcessorConfiguration implements Serializable {

    public enum ActionEnum {
        Delete,
        Insert,
        Update
    }

    @Option
    @Proposable("valuesProvider")
    @Documentation("")
    private String action1;

    @Option
    @Documentation("")
    private ActionEnum action2;

    public String getAction1() {
        return action1;
    }

    public DropdownlistProcessorProcessorConfiguration setAction1(String action1) {
        this.action1 = action1;
        return this;
    }

}