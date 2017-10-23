/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.components.server.test.jdbc;

import java.io.Serializable;

import org.talend.component.api.configuration.Option;
import org.talend.component.api.configuration.ui.widget.Credential;
import org.talend.component.api.configuration.type.DataStore;

import lombok.Data;

@Data
@DataStore("jdbc")
public class JdbcDataStore implements Serializable {

    @Option
    private String url;

    @Option
    private String username;

    @Option
    @Credential
    private String password;
}
