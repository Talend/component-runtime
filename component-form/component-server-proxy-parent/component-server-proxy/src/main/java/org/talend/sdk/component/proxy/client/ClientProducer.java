/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.client;

import static java.util.Optional.ofNullable;

import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.jaxrs.JAXRSClient;
import org.talend.sdk.component.proxy.config.ProxyConfiguration;

@ApplicationScoped
public class ClientProducer {

    @Inject
    private ProxyConfiguration configuration;

    @Produces
    @ApplicationScoped
    public javax.ws.rs.client.Client client() {
        final javax.ws.rs.client.Client client = ClientBuilder
                .newBuilder()
                .executorService(Executors.newCachedThreadPool()) // todo make the pool configurable
                .newClient();
        ofNullable(configuration.getClientProviders()).ifPresent(list -> list.forEach(client::register));
        return client;
    }

    @Produces
    @ApplicationScoped
    public WebTarget webTarget(final javax.ws.rs.client.Client client) {
        return client.target(configuration.getTargetServerBase());
    }

    @Produces
    @ApplicationScoped
    public Client actionClient(final javax.ws.rs.client.Client client) {
        return new JAXRSClient(client, configuration.getTargetServerBase(), true);
    }

    public void disposeClient(@Disposes final javax.ws.rs.client.Client client) {
        client.close();
    }

}
