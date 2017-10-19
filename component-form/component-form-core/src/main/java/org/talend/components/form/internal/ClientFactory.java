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
package org.talend.components.form.internal;

import org.talend.components.form.api.Client;
import org.talend.components.form.internal.jaxrs.JAXRSClient;
import org.talend.components.form.internal.spring.SpringRestClient;

public final class ClientFactory {

    private ClientFactory() {
        // no-op
    }

    public static Client createDefault(final String base) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("javax.ws.rs.client.ClientBuilder");
            return new JAXRSClient(base);
        } catch (final NoClassDefFoundError | ClassNotFoundException cnfe) {
            return new SpringRestClient(base);
        }
    }
}
