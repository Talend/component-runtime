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
package org.talend.components.form.demo;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.talend.components.form.api.ActionService;
import org.talend.components.form.api.Client;
import org.talend.components.form.api.UiSpecService;
import org.talend.components.form.internal.ClientFactory;

@ApplicationScoped
public class DemoConfiguration {

    @Produces
    public Client client() {
        return ClientFactory.createDefault(System.getProperty("demo.components.base", "http://localhost:8080/api/v1"));
    }

    public void releaseClient(@Disposes final Client client) {
        client.close();
    }

    @Produces
    public UiSpecService uiSpecService(final Client client) {
        return new UiSpecService(client);
    }

    @Produces
    public ActionService actionService() {
        return new ActionService();
    }
}
