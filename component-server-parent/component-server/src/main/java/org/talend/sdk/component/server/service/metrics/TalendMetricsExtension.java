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
package org.talend.sdk.component.server.service.metrics;

import static java.util.Optional.ofNullable;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TalendMetricsExtension implements Extension {

    private MetricsRegistrar metricsRegistrar;

    void activateMetrics(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
        System.setProperty("geronimo.metrics.jaxrs.activated", "true");
    }

    void registerMetrics(@Observes final AfterDeploymentValidation validation) {
        try {
            Thread
                    .currentThread()
                    .getContextClassLoader()
                    .loadClass("org.apache.geronimo.microprofile.metrics.cdi.MetricsExtension");
            log.info("Activating the metrics");
            metricsRegistrar = new MetricsRegistrar();
            metricsRegistrar.start();
        } catch (final Exception ex) {
            log.debug(ex.getMessage(), ex);
        }
    }

    void beforeShutdown(@Observes final BeforeShutdown beforeShutdown) {
        ofNullable(metricsRegistrar).ifPresent(MetricsRegistrar::stop);
    }
}
