/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.test.connectors.migration.AbstractMigrationHandler.ExtraMigrationHandler;

@Version(value = InputConfig.INPUT_CONFIG_VERSION, migrationHandler = ExtraMigrationHandler.class)
@GridLayout({
        @GridLayout.Row({ "date" }),
        @GridLayout.Row({ "dataset" }) })
@GridLayout(
        names = GridLayout.FormType.ADVANCED,
        value = {
                @GridLayout.Row({ "dataset" }) })
public class InputConfig implements Serializable {

    public final static int INPUT_CONFIG_VERSION = 3;

    @Option
    @Documentation("Doc: default dataset documentation without Internationalization.")
    private DemoDataset dataset;

    @Option
    @Documentation("Doc: default date documentation without Internationalization.")
    private ZonedDateTime date;

}
