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
import org.talend.sdk.component.api.configuration.action.Checkable;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout.FormType;
import org.talend.sdk.component.api.configuration.ui.widget.Credential;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.feature.form.service.UIService;

import lombok.Data;

@Data
@DataStore
@Checkable(UIService.HEALTHCHECK)
@GridLayout(value = {
        @GridLayout.Row({ "login", "password" }),
        @GridLayout.Row({ "healthcheckOk" })
})
@GridLayout(names = FormType.ADVANCED, value = { @GridLayout.Row({ "timeout" }) })
public class ADatastore implements Serializable {

    @Option
    @Documentation("A login.")
    private String login;

    @Option
    @Credential
    @Documentation("A password.")
    private String password;

    @Option
    @Documentation("Force the healtcheck result.")
    private boolean healthcheckOk;

    @Option
    @Documentation("A timeout in the advanced settings form.")
    private int timeout;

}
