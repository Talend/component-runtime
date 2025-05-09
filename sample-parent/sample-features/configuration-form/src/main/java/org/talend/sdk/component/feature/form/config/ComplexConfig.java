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
package org.talend.sdk.component.feature.form.config;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.condition.ActiveIf;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout.FormType;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout(value = {
        @GridLayout.Row({ "dataset" }),
        @GridLayout.Row({ "displayAllSupportedTypes" }),
        @GridLayout.Row({ "allSupportedTypes" }),
        @GridLayout.Row({ "displaySomeLists" }),
        @GridLayout.Row({ "someLists" }),
        @GridLayout.Row({ "displayElementsWithConstraints" }),
        @GridLayout.Row({ "elementsWithConstraints" }),
        @GridLayout.Row({ "displayConditionalDisplay" }),
        @GridLayout.Row({ "conditionalDisplay" }),
        @GridLayout.Row({ "displayDynamicElements" }),
        @GridLayout.Row({ "dynamicElements" }),
})
@GridLayout(names = FormType.ADVANCED, value = { @GridLayout.Row({ "dataset" }) })
public class ComplexConfig implements Serializable {

    @Option
    @Documentation("The dataset part.")
    private ADataset dataset;

    @Option
    @Documentation("Display 'All supported types' section.")
    private boolean displayAllSupportedTypes;

    @Option
    @ActiveIf(target = "displayAllSupportedTypes", value = "true")
    @Documentation("A complex configuration with all supported types.")
    private AllSupportedTypes allSupportedTypes;

    @Option
    @Documentation("Display 'All some lists' section.")
    private boolean displaySomeLists;

    @Option
    @ActiveIf(target = "displaySomeLists", value = "true")
    @Documentation("Some list of configuration.")
    private SomeLists someLists;

    @Option
    @Documentation("Display 'Element with constraints' section.")
    private boolean displayElementsWithConstraints;

    @Option
    @ActiveIf(target = "displayElementsWithConstraints", value = "true")
    @Documentation("Elements with constraints.")
    private ElementsWithConstraints elementsWithConstraints;

    @Option
    @Documentation("Display 'Conditional display' section.")
    private boolean displayConditionalDisplay;

    @Option
    @ActiveIf(target = "displayConditionalDisplay", value = "true")
    @Documentation("Elements with conditional display.")
    private ConditionalDisplay conditionalDisplay;

    @Option
    @Documentation("Display dynamic elements section.")
    private boolean displayDynamicElements;

    @Option
    @ActiveIf(target = "displayDynamicElements", value = "true")
    @Documentation("Elements binded to some services.")
    private DynamicElements dynamicElements = new DynamicElements();

}
