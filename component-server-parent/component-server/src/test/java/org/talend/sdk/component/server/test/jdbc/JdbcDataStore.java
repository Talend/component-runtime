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
package org.talend.sdk.component.server.test.jdbc;

import static java.util.Arrays.asList;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.widget.Credential;
import org.talend.sdk.component.api.configuration.ui.widget.DateTime;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Data
@DataStore("jdbc")
public class JdbcDataStore implements Serializable {

    @Option
    @Documentation(value = "Documentation for Datastore url.", tooltip = true)
    private String url;

    @Option
    @Documentation(value = "Documentation for Datastore user.", tooltip = true)
    private String username;

    @Option
    @Credential
    @Documentation(value = "Documentation for Datastore password.", tooltip = true)
    private String password;

    @Option // to test tables
    @Documentation(value = "Documentation for Datastore configurations.", tooltip = true)
    private List<JdbcConfig> configurations = asList(new JdbcConfig("d1", "D1"), new JdbcConfig("d2", "D2"));

    @Option
    @DateTime(dateFormat = "DD-MM-YYYY", useUTC = false, useSeconds = false)
    @Documentation(value = "Documentation for Credentials valid until date.")
    private ZonedDateTime credentialsValidity;
}
