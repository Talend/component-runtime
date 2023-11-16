/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.tomcat;

import java.util.stream.Stream;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.meecrowave.Meecrowave;

public class TomcatSetup implements Meecrowave.InstanceCustomizer {

    @Override
    public void accept(final Tomcat tomcat) {
        final Server server = tomcat.getServer();
        server.addLifecycleListener(event -> {
            if (Server.class.isInstance(event.getData()) && Lifecycle.AFTER_DESTROY_EVENT.equals(event.getType())
                    && Boolean.getBoolean("talend.component.exit-on-destroy")) {
                System.exit(0);
            }
        });
        // if we want it to be really configurable we should add it in ComponentServerConfiguration
        // and set this instance in the standard context to be able to configure it from cdi side
        final boolean dev = Boolean.getBoolean("talend.component.server.tomcat.valve.error.debug");
        if (!dev) {
            Stream
                    .of(server.findServices())
                    .map(Service::getContainer)
                    .flatMap(e -> Stream.of(e.findChildren()))
                    .filter(StandardHost.class::isInstance)
                    .map(StandardHost.class::cast)
                    .forEach(host -> host.addLifecycleListener(event -> {
                        if (event.getType().equals(Lifecycle.BEFORE_START_EVENT)) {
                            StandardHost.class
                                    .cast(host)
                                    .setErrorReportValveClass(MinimalErrorReportValve.class.getName());
                        }
                    }));
        }
    }

    public static class MinimalErrorReportValve extends ErrorReportValve {

        public MinimalErrorReportValve() {
            setShowReport(false);
            setShowServerInfo(false);
        }
    }
}
