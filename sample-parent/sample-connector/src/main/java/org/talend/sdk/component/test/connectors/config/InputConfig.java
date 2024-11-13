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
package org.talend.sdk.component.test.connectors.config;

import java.io.Serializable;
import java.time.ZonedDateTime;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.Hidden;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.DateTime;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@GridLayout({
        @GridLayout.Row({ "date" }),
        @GridLayout.Row({ "dataset" }),
        @GridLayout.Row({ "generateException" }),
        @GridLayout.Row({ "dbType" }) })
@GridLayout(
        names = GridLayout.FormType.ADVANCED,
        value = {
                @GridLayout.Row({ "dataset" }) })
public class InputConfig implements Serializable {

    public final static int INPUT_CONFIG_VERSION = 3;

    @Option
    @Documentation("Doc: default dataset documentation without Internationalization.")
    private TheDataset dataset;

    @Option
    @DateTime
    @Documentation("Doc: default date documentation without Internationalization.")
    private ZonedDateTime date;

    @Option
    @Documentation("Doc: Use the generate exception service or not.")
    @DefaultValue("false")
    private Boolean generateException = false;

    @Option
    @Documentation("Doc: Use the generate runtime exception service or not.")
    @DefaultValue("false")
    private Boolean generateRuntimeException = false;

    @Option
    @Documentation("Data base type from the supported data base list.")
    @DefaultValue("Mysql")
    @Hidden
    private String dbType;
}
