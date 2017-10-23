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
package org.talend.component.server.test;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.meecrowave.Meecrowave;

@ApplicationScoped
public class ClientProducer {

    @Inject
    private Bus bus;

    @PostConstruct
    private void enableLogging() {
        if (Boolean.getBoolean("component.server.test.logging.skip")) {
            return;
        }
        new LoggingFeature().initialize(bus);
    }

    @Produces
    @ApplicationScoped
    public Client client() {
        return ClientBuilder.newClient();
    }

    @Produces
    @ApplicationScoped
    public WebTarget webTarget(final Client client, final Meecrowave.Builder config) {
        return client.target(String.format("http://localhost:%d/api/v1", config.getHttpPort()));
    }

    public void releaseClient(@Disposes final Client client) {
        client.close();
    }
}
