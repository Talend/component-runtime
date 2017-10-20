// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.server.test;

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
