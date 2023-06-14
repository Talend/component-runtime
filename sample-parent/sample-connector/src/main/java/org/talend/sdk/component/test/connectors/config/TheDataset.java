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
package org.talend.sdk.component.test.connectors.config;

import java.io.Serializable;

import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Suggestable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.test.connectors.migration.AbstractMigrationHandler.DatasetMigrationHandler;
import org.talend.sdk.component.test.connectors.service.ActionsServices;

import lombok.Data;

@Version(value = TheDataset.DATASET_VERSION, migrationHandler = DatasetMigrationHandler.class)
@Data
@DataSet("TheDataset")
@GridLayout({
        @GridLayout.Row({ "entity" }),
        @GridLayout.Row({ "nestedConfigSrc" }),
        @GridLayout.Row({ "nestedConfigDest" }),
        @GridLayout.Row({ "datastore" }) })
@GridLayout(
        names = GridLayout.FormType.ADVANCED,
        value = {
                @GridLayout.Row({ "readAll" }),
                @GridLayout.Row({ "datastore" }) })
public class TheDataset implements Serializable {

    public final static int DATASET_VERSION = 5;
    public final static String DATASET_INFO = "info-to-sanitize";

    @Option
    @Documentation("Doc: default datastore documentation without Internationalization.")
    private TheDatastore datastore;

    @Option
    @Documentation("Doc: default entity documentation without Internationalization.")
    @Suggestable(value = ActionsServices.LIST_ENTITIES)
    private String entity;

    @Option
    @Documentation("Doc: default readAll documentation without Internationalization.")
    private boolean readAll;

    @Option
    @Documentation("Doc: default nestedConfigSrc documentation without Internationalization.")
    private NestedConfig nestedConfigSrc;

    @Option
    @Documentation("Doc: default nestedConfigDest documentation without Internationalization.")
    @Updatable(value = ActionsServices.UPDATE_CONFIG, parameters = { "nestedConfigSrc" }, after = "nestedConfigDest")
    private NestedConfig nestedConfigDest;

}
