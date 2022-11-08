/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.cli;

import static java.util.Optional.ofNullable;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.runner.Cli;

// utility to use into the studio, mainly to workaround JVM URL stream handler limitations
public class EnhancedCli extends Cli implements AutoCloseable {

    private final String[] args;

    private volatile Meecrowave instance;

    public EnhancedCli(final String[] args) {
        super(args);
        this.args = args;
    }

    @Override
    public void run() {
        try {
            try (final Meecrowave meecrowave = new Meecrowave(create(args)) {

                @Override
                protected Connector createConnector() {
                    return new Connector(CustomPefixHttp11NioProtocol.class.getName());
                }
            }) {
                this.instance = meecrowave;

                meecrowave.start();
                meecrowave.deployClasspath(new Meecrowave.DeploymentMeta("", null, stdCtx -> {
                    stdCtx.setResources(new StandardRoot() {

                        @Override
                        protected void registerURLStreamHandlerFactory() {
                            // no-op: not supported into OSGi since there is already one and it must set a
                            // single time
                        }
                    });
                }, null));

                doWait(meecrowave, null);
            }
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        ofNullable(instance).ifPresent(mw -> StandardServer.class.cast(mw.getTomcat().getServer()).stopAwait());
    }

    public static class CustomPefixHttp11NioProtocol extends Http11NioProtocol {

        @Override
        protected String getNamePrefix() {
            return "talend-component-kit-" + super.getNamePrefix();
        }
    }
}
