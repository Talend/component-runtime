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
package org.talend.runtime.documentation.component.configuration;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout({ @GridLayout.Row({ "dataStore" }), @GridLayout.Row({ "commonConfig", "actionOnTable" }) })
@GridLayout(names = GridLayout.FormType.ADVANCED,
        value = { @GridLayout.Row({ "commonConfig" }), @GridLayout.Row({ "noResponseBody" }) })
public class OutputConfig implements Serializable {

    @Option
    private BasicAuthConfig dataStore;

    @Option
    private CommonConfig commonConfig;

    @Option
    @Documentation("Action to execute on a table, Insert, Update, Delete")
    private ActionOnTable actionOnTable = ActionOnTable.Insert;

    @Option
    @Documentation("Activate or deactivate the response body after the action. Default is true")
    private boolean noResponseBody = false;

    public enum ActionOnTable {
        Insert,
        Update,
        Delete
    }

}