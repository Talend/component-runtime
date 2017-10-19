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
package org.talend.components.starter.server.test;

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
