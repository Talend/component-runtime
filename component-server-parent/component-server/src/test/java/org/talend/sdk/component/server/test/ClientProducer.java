/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.meecrowave.Meecrowave;

@ApplicationScoped
public class ClientProducer {

    @Produces
    @ApplicationScoped
    public Client client() {
        return ClientBuilder.newClient().register(new Feature() { // gzip feature depends on jaxws API, fixed in 3.3.3

            @Override
            public void initialize(final Server server, final Bus bus) {
                initializeProvider(server.getEndpoint(), bus);
            }

            @Override
            public void initialize(final org.apache.cxf.endpoint.Client client, final Bus bus) {
                initializeProvider(client.getEndpoint(), bus);
            }

            @Override
            public void initialize(final InterceptorProvider interceptorProvider, final Bus bus) {
                initializeProvider(interceptorProvider, bus);
            }

            @Override
            public void initialize(final Bus bus) {
                initializeProvider(bus, bus);
            }

            private void initializeProvider(final InterceptorProvider provider, final Bus bus1) {
                provider.getInInterceptors().add(new GZIPInInterceptor());
                final GZIPOutInterceptor gzipOutInterceptor = new GZIPOutInterceptor();
                provider.getOutInterceptors().add(gzipOutInterceptor);
                provider.getOutFaultInterceptors().add(gzipOutInterceptor);
            }
        });
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
