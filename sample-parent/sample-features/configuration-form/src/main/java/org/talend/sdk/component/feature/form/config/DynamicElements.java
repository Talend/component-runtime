/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
import org.talend.sdk.component.api.configuration.action.Suggestable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.action.Validable;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.feature.form.service.UIService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@GridLayout(value = {
        @GridLayout.Row({ "singleString" }),
        @GridLayout.Row({ "someComplexConfig" }),
        @GridLayout.Row({ "asyncValidation" }),
        @GridLayout.Row({ "suggestedElement" }),
})
public class DynamicElements implements Serializable {

    @Option
    @Documentation("A single option used as parameter for dynamic elements.")
    private String singleString;

    // Todo: Issue in webUI and studio, the update is not completely effective.
    @Option
    @Documentation("Some configuration used by dynamics elements. This object is validated by a service call.")
    @Updatable(value = UIService.UPDATABLE, parameters = { "singleString", "suggestedElement" },
            after = "anInteger")
    @Validable(UIService.ASYNC_VALIDATION)
    private SomeComplexConfig someComplexConfig = new SomeComplexConfig();

    @Option
    @Documentation("A String with async validation.")
    @Validable(UIService.ASYNC_VALIDATION_ONSTRING)
    private String asyncValidation = "";

    @Option
    @Documentation("Select one value among suggestions retrieved from a service.")
    @Suggestable(value = UIService.SUGGESTABLE, parameters = { "someComplexConfig", "singleString" })
    private String suggestedElement;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @GridLayout(value = {
            @GridLayout.Row({ "aString" }),
            @GridLayout.Row({ "aBoolean" }),
            @GridLayout.Row({ "anInteger" })
    })
    public static class SomeComplexConfig implements Serializable {

        @Option
        @Documentation("A String value.")
        private String aString;

        @Option
        @Documentation("A boolean value.")
        private boolean aBoolean;

        @Option
        @Documentation("An integer value.")
        private int anInteger;

    }

}
