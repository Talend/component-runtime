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
package org.talend.sdk.component.documentation.sample.activeif.components.processor;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.condition.ActiveIfs;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

@GridLayout({
        // the generated layout put one configuration entry per line,
        // customize it as much as needed
        @GridLayout.Row({ "checkbox1" }), @GridLayout.Row({ "checkbox2" }), @GridLayout.Row({ "checkbox3" }),
        @GridLayout.Row({ "configuration4" }), @GridLayout.Row({ "configuration5" }),
        @GridLayout.Row({ "configuration6" }) })
@Documentation("TODO fill the documentation for this configuration")
public class ActiveifProcessorProcessorConfiguration implements Serializable {

    @Option
    @Documentation("")
    private boolean checkbox1;

    @Option
    @Documentation("")
    private boolean checkbox2;

    @Option
    @Documentation("")
    private boolean checkbox3;

    @Option
    @ActiveIf(target = "checkbox1", value = "true")
    @Documentation("Active if checkbox1 is selected")
    private String configuration4;

    @Option
    @ActiveIfs(operator = ActiveIfs.Operator.OR, value = { @ActiveIf(target = "checkbox2", value = "true"),
            @ActiveIf(target = "checkbox3", value = "true") })
    @Documentation("Active if checkbox2 or checkbox 3 are selected")
    private String configuration5;

    @Option
    @ActiveIfs(operator = ActiveIfs.Operator.AND, value = { @ActiveIf(target = "checkbox2", value = "true"),
            @ActiveIf(target = "checkbox3", value = "true") })
    @Documentation("Active if checkbox2 and checkbox 3 are selected")
    private String configuration6;

    public boolean getCheckbox1() {
        return checkbox1;
    }

    public ActiveifProcessorProcessorConfiguration setCheckbox1(boolean checkbox1) {
        this.checkbox1 = checkbox1;
        return this;
    }

    public boolean getCheckbox2() {
        return checkbox2;
    }

    public ActiveifProcessorProcessorConfiguration setCheckbox2(boolean checkbox2) {
        this.checkbox2 = checkbox2;
        return this;
    }

    public boolean getCheckbox3() {
        return checkbox3;
    }

    public ActiveifProcessorProcessorConfiguration setCheckbox3(boolean checkbox3) {
        this.checkbox3 = checkbox3;
        return this;
    }

    public String getConfiguration4() {
        return configuration4;
    }

    public ActiveifProcessorProcessorConfiguration setConfiguration4(String configuration4) {
        this.configuration4 = configuration4;
        return this;
    }

    public String getConfiguration5() {
        return configuration5;
    }

    public ActiveifProcessorProcessorConfiguration setConfiguration5(String configuration5) {
        this.configuration5 = configuration5;
        return this;
    }

    public String getConfiguration6() {
        return configuration6;
    }

    public ActiveifProcessorProcessorConfiguration setConfiguration6(String configuration6) {
        this.configuration6 = configuration6;
        return this;
    }
}