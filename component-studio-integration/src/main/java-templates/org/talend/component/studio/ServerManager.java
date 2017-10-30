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
package org.talend.component.studio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.talend.osgi.hook.maven.MavenResolver;

public class ServerManager extends AbstractUIPlugin {

    private final String groupId = "${project.groupId}";

    private final String artifactId = "${project.artifactId}";

    private ProcessManager manager;

    private final Collection<ServiceRegistration<?>> services = new ArrayList<>();

    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);

        manager = new ProcessManager(groupId, artifactId, findMavenResolver());
        manager.start();
        services.add(getBundle().getBundleContext().registerService(ProcessManager.class.getName(), manager, new Hashtable<>()));
        // todo: create a client (and register it as a service?), potentially reuse component-form-core if OSGified
    }

    @Override
    public synchronized void stop(final BundleContext context) throws Exception {
        try {
            services.forEach(ServiceRegistration::unregister);
            services.clear();

            if (manager != null) {
                manager.close();
                manager = null;
            }
        } finally {
            super.stop(context);
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
