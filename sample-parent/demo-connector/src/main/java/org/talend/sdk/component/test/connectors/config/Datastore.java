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
import org.talend.sdk.component.api.configuration.constraint.Required;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.test.connectors.migration.AbstractMigrationHandler.DatastoreMigrationHandler;

import lombok.Data;

@Version(value = Datastore.DATASTORE_VERSION, migrationHandler = DatastoreMigrationHandler.class)
@Data
@DataStore("Connection")
@Documentation("The connection part of the connector's configuration.")
@GridLayout({
        @GridLayout.Row({ "url" }),
        @GridLayout.Row({ "auth" }) })
@GridLayout(
        names = GridLayout.FormType.ADVANCED,
        value = {
                @GridLayout.Row({ "auth" }),
                @GridLayout.Row({ "timeout" }) })
public class Datastore implements Serializable {

    public final static int DATASTORE_VERSION = 2;

    @Option
    @Required
    @Documentation("The url connection.")
    private String url;

    @Option
    @Documentation("The authentication part.")
    private Auth auth;

    @Option
    @Documentation("Connection's timeout.")
    private int timeout;

}
