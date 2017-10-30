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
package org.talend.sdk.component.starter.server.test;

import lombok.experimental.Delegate;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.Meecrowave;
import org.junit.rules.ExternalResource;

public class ClientRule extends ExternalResource {
    @Delegate
    private Client client;

    @Override
    protected void before() throws Throwable {
        client = ClientBuilder.newClient();
    }

    @Override
    protected void after() {
        client.close();
    }

    public WebTarget target() {
        final Meecrowave.Builder config = CDI.current().select(Meecrowave.Builder.class).get();
        return client.target("http://localhost:" + config.getHttpPort() + "/api");
    }
}
