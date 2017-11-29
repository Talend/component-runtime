/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.studio;

import static org.talend.sdk.component.studio.GAV.ARTIFACT_ID;
import static org.talend.sdk.component.studio.GAV.GROUP_ID;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.tomcat.websocket.Constants;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.talend.osgi.hook.URIUtil;
import org.talend.osgi.hook.maven.MavenResolver;
import org.talend.sdk.component.studio.service.ComponentService;
import org.talend.sdk.component.studio.websocket.WebSocketClient;

public class ServerManager extends AbstractUIPlugin {

    private ProcessManager manager;

    private final Collection<ServiceRegistration<?>> services = new ArrayList<>();

    private WebSocketClient client;

    private Runnable reset;

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        reset = Lookups.init();

        manager = new ProcessManager(GROUP_ID, ARTIFACT_ID, findMavenResolver(), findConfigDir());
        manager.start();

        client = new WebSocketClient("ws://localhost:" + manager.getPort() + "/websocket/v1",
                Long.getLong("talend.component.websocket.client.timeout", Constants.IO_TIMEOUT_MS_DEFAULT));
        client.setSynch(() -> manager.waitForServer(() -> client.v1().healthCheck()));

        final BundleContext ctx = getBundle().getBundleContext();
        services.add(ctx.registerService(ProcessManager.class.getName(), manager, new Hashtable<>()));
        services.add(ctx.registerService(WebSocketClient.class.getName(), client, new Hashtable<>()));
        services.add(ctx.registerService(ComponentService.class.getName(), new ComponentService(), new Hashtable<>()));
    }

    @Override
    public synchronized void stop(final BundleContext context) throws Exception {
        try {
            services.forEach(ServiceRegistration::unregister);
            services.clear();

            RuntimeException error = null;
            try {
                if (manager != null) {
                    manager.close();
                    manager = null;
                }
            } catch (final RuntimeException re) {
                error = re;
            }
            try {
                if (client != null) {
                    client.close();
                    client = null;
                }
            } catch (final RuntimeException ioe) {
                if (error != null) {
                    throw error;
                }
                throw ioe;
            }

            if (reset != null) {
                reset.run();
            }

            if (error != null) {
                throw error;
            }
        } finally {
            super.stop(context);
        }
    }

    private File findConfigDir() {
        try {
            return URIUtil.toFile(Platform.getConfigurationLocation().getURL().toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("bad configuration configuration", e);
        }
    }

    private MavenResolver findMavenResolver() {
        final BundleContext bundleContext = getBundle().getBundleContext();
        final ServiceReference<MavenResolver> serviceReference = bundleContext.getServiceReference(MavenResolver.class);
        MavenResolver mavenResolver = null;
        if (serviceReference != null) {
            mavenResolver = bundleContext.getService(serviceReference);
        }
        if (mavenResolver == null) {
            throw new IllegalArgumentException("No MavenResolver found");
        }
        return mavenResolver;
    }
}
