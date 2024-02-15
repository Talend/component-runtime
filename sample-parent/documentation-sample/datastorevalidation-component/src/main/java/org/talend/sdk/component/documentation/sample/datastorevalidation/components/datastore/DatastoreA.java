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
package org.talend.sdk.component.documentation.sample.datastorevalidation.components.datastore;

import java.io.Serializable;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.action.Checkable;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.configuration.ui.widget.Credential;
import org.talend.sdk.component.api.meta.Documentation;

@DataStore("DatastoreA")
@Checkable
@GridLayout({
        // the generated layout put one configuration entry per line,
        // customize it as much as needed
        @GridLayout.Row({ "APIURL" }), @GridLayout.Row({ "username" }), @GridLayout.Row({ "password" }) })
@Documentation("TODO fill the documentation for this configuration")
public class DatastoreA implements Serializable {

    @Option
    @Documentation("")
    private java.net.URL APIURL;

    @Option
    @Documentation("")
    private String username;

    @Option
    @Credential
    @Documentation("")
    private String password;

    public java.net.URL getAPIURL() {
        return APIURL;
    }

    public DatastoreA setAPIURL(java.net.URL APIURL) {
        this.APIURL = APIURL;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public DatastoreA setUsername(String Username) {
        this.username = Username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public DatastoreA setPassword(String Password) {
        this.password = Password;
        return this;
    }
}