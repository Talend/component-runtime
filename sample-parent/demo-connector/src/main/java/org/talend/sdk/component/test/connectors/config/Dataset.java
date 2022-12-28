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

import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Suggestable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.test.connectors.migration.AbstractMigrationHandler.DatasetMigrationHandler;
import org.talend.sdk.component.test.connectors.service.UIService;

import lombok.Data;

@Version(value = Dataset.DATASET_VERSION, migrationHandler = DatasetMigrationHandler.class)
@Data
@DataSet("Dataset")
@Documentation("The dataset of the demo connector plugin.")
@GridLayout({
        @GridLayout.Row({ "datastore" }),
        @GridLayout.Row({ "entity" }),
        @GridLayout.Row({ "nestedConfigSrc" }),
        @GridLayout.Row({ "nestedConfigDest" }) })
@GridLayout(
        names = GridLayout.FormType.ADVANCED,
        value = { @GridLayout.Row({ "datastore" }),
                @GridLayout.Row({ "readAll" }) })
public class Dataset implements Serializable {

    public final static int DATASET_VERSION = 3;

    @Option
    @Documentation("The connection configuration.")
    private Datastore datastore;

    @Option
    @Documentation("Entity list.")
    @Suggestable(value = UIService.LIST_ENTITIES)
    private String entity;

    @Option
    @Documentation("Read all the data.")
    private boolean readAll;

    @Option
    @Documentation("Original value for the updatable.")
    private NestedConfig nestedConfigSrc;

    @Option
    @Documentation("Updatable sub-object that should retrieve its values from nestedConfigSrc.")
    @Updatable(value = UIService.UPDATE_NESTED_CONFIG, parameters = { "nestedConfigSrc" }, after = "nestedConfigDest")
    private NestedConfig nestedConfigDest;

}
