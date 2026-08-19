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

import java.io.File;
import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.Hidden;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.Code;
import org.talend.sdk.component.api.configuration.ui.widget.Credential;
import org.talend.sdk.component.api.configuration.ui.widget.Path;
import org.talend.sdk.component.api.configuration.ui.widget.Path.Type;
import org.talend.sdk.component.api.configuration.ui.widget.ReadOnly;
import org.talend.sdk.component.api.configuration.ui.widget.TextArea;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout(value = {
        @GridLayout.Row({ "stringWithDefault" }),
        @GridLayout.Row({ "hiddenString" }),
        @GridLayout.Row({ "multiline" }),
        @GridLayout.Row({ "sqlInput" }),
        @GridLayout.Row({ "jsonInput" }),
        @GridLayout.Row({ "aToken" }),
        @GridLayout.Row({ "folder" }),
        @GridLayout.Row({ "file" }),
        @GridLayout.Row({ "readOnly" }),
})
public class ConfiguredWidgets implements Serializable {

    @Option
    @Documentation("String with a default value.")
    @DefaultValue("A default value.")
    private String stringWithDefault;

    @Option
    @Documentation("Hidden field, could be set by an @Update service.")
    @Hidden
    @DefaultValue("A default for the hidden value.")
    private String hiddenString;

    @Option
    @Documentation("A multiline input.")
    @TextArea
    private String multiline;

    @Option
    @Documentation("Multiline text area with syntax highlighting, here with sql syntax highlighting.")
    @Code("sql")
    private String sqlInput;

    @Option
    @Documentation("Multiline text area with syntax highlighting, here with json syntax highlighting.")
    @Code("json")
    private String jsonInput;

    @Option
    @Documentation("A credential, input should be hidden and encrypted in backend.")
    @Credential
    private String aToken;

    @Option
    @Documentation("A folder selector, only supported by studio.")
    @Path(Type.DIRECTORY)
    private File folder;

    @Option
    @Documentation("A file selector, only supported by studio.")
    @Path(Type.FILE)
    private File file;

    @Option
    @Documentation("A read only property.")
    @ReadOnly
    private String readOnly;

}
