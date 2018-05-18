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
package org.talend.sdk.component.server.front.monitoring;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;

import brave.Tracing;
import brave.http.HttpRuleSampler;
import brave.http.HttpTracing;
import brave.sampler.CountingSampler;

import zipkin2.Span;
import zipkin2.reporter.Reporter;

@ApplicationScoped
public class BraveConfiguration {

    @Inject
    @ConfigProperty(name = "TRACING_SAMPLING_RATE")
    private Float samplingRate;


    @Inject
    @ConfigProperty(name = "TRACING_ON")
    private Boolean tracingOn;

    @Inject
    private MonitoringLogger monitoringLogger;

    @Produces
    @ApplicationScoped
    public HttpTracing httpTracing(final ComponentServerConfiguration configuration) {
        return HttpTracing
                .newBuilder(Tracing
                        .newBuilder()
                        .localServiceName(configuration.serviceName())
                        .sampler(CountingSampler.create(toActualRate(configuration.samplerRate())))
                        .spanReporter(createReporter(configuration))
                        .build())
                .serverSampler(HttpRuleSampler
                        .newBuilder()
                        .addRule("GET", "/api/v1/environment", toActualRate(configuration.samplerEnvironmentRate()))
                        .addRule("GET", "/api/v1/configurationtype", toActualRate(configuration.samplerConfigurationTypeRate()))
                        .addRule("GET", "/api/v1/component", toActualRate(configuration.samplerComponentRate()))
                        .addRule("POST", "/api/v1/component", toActualRate(configuration.samplerComponentRate()))
                        .addRule("POST", "/api/v1/execution", configuration.samplerExecutionRate())
                        .addRule("GET", "/api/v1/action", toActualRate(configuration.samplerActionRate()))
                        .addRule("POST", "/api/v1/action", toActualRate(configuration.samplerActionRate()))
                        .addRule("GET", "/api/v1/documentation", toActualRate(configuration.samplerDocumentationRate()))
                        .build())
                .clientSampler(HttpRuleSampler.newBuilder().build())
                .build();
    }

    private float toActualRate(final float rate) {
        if (rate < 0) {
            return samplingRate != null ? samplingRate : 0.1f;
        }
        return rate;
    }

    private Reporter<Span> createReporter(final ComponentServerConfiguration configuration) {
        if (!tracingOn) {
            return Reporter.NOOP;
        }
        final String reporter = configuration.reporter();
        final String type = reporter.contains("(") ? reporter.substring(0, reporter.indexOf('(')) : reporter;
        switch (type) {
        case "noop":
            return Reporter.NOOP;
        case "log":
            return monitoringLogger;
        default:
            throw new IllegalArgumentException("Unsupported reporter: '" + reporter + "', "
                    + "please do a PR on github@Talend/component-runtime if you want it to be supported");
        }
    }
}