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
package org.talend.sdk.component.server.front.monitoring;

import static java.util.stream.Collectors.toMap;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.lang.StringPropertiesTokenizer;

import brave.Tracing;
import brave.http.HttpRuleSampler;
import brave.http.HttpTracing;
import brave.sampler.CountingSampler;

import zipkin2.Span;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;

@ApplicationScoped
public class BraveConfiguration {

    @Inject
    @ConfigProperty(name = "TRACING_ON", defaultValue = "false")
    private Boolean tracingOn;

    @Inject
    @ConfigProperty(name = "TRACING_KAFKA_URL")
    private String kafkaUrl;

    @Inject
    @ConfigProperty(name = "TRACING_KAFKA_TOPIC", defaultValue = "zipkin")
    private String kafkaTopic;

    @Inject
    @ConfigProperty(name = "TRACING_SAMPLING_RATE")
    private Float samplingRate;

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
                        .addRule("GET", "/api/v1/environment", toActualRate(configuration.samplerComponentRate()))
                        .addRule("GET", "/api/v1/configurationtype", toActualRate(configuration.samplerComponentRate()))
                        .addRule("GET", "/api/v1/component", toActualRate(configuration.samplerComponentRate()))
                        .addRule("POST", "/api/v1/component", toActualRate(configuration.samplerComponentRate()))
                        .addRule("POST", "/api/v1/execution", configuration.samplerExecutionRate())
                        .addRule("GET", "/api/v1/action", toActualRate(configuration.samplerActionRate()))
                        .addRule("POST", "/api/v1/action", toActualRate(configuration.samplerActionRate()))
                        .build())
                .clientSampler(HttpRuleSampler.newBuilder().build())
                .build();
    }

    private float toActualRate(final float rate) {
        if (rate < 0) {
            return tracingOn && samplingRate != null ? samplingRate : 0.1f;
        }
        return rate;
    }

    private Reporter<Span> createReporter(final ComponentServerConfiguration configuration) {
        final String reporter = configuration.reporter();
        if ("auto".equalsIgnoreCase(reporter)) {
            if (!tracingOn || kafkaUrl == null) {
                return Reporter.NOOP;
            }
            return customizeAsyncReporter(configuration, AsyncReporter
                    .builder(toKafkaSender("kafka(" + "servers=" + kafkaUrl + ",topic=" + kafkaTopic + ")")));
        }

        final String type = reporter.contains("(") ? reporter.substring(0, reporter.indexOf('(')) : reporter;
        switch (type) {
        case "noop":
            return Reporter.NOOP;
        case "url":
            return customizeAsyncReporter(configuration, AsyncReporter.builder(toUrlSender(reporter)));
        case "kafka":
            return customizeAsyncReporter(configuration, AsyncReporter.builder(toKafkaSender(reporter)));
        case "console":
            return Reporter.CONSOLE;
        default:
            throw new IllegalArgumentException("Unsupported reporter: '" + reporter + "', "
                    + "please do a PR on github@Talend/component-runtime if you want it to be supported");
        }
    }

    private AsyncReporter<Span> customizeAsyncReporter(final ComponentServerConfiguration configuration,
            final AsyncReporter.Builder build) {
        final Map<String, String> config = readConfiguration(configuration.reporterAsyncConfiguration());
        Object out = build;
        out = builderSet(out, "messageMaxBytes", config, int.class);
        out = builderSet(out, "queuedMaxSpans", config, int.class);
        out = builderSet(out, "queuedMaxBytes", config, int.class);
        out = builderSet(out, "messageTimeout", config, long.class, TimeUnit.class);
        out = builderSet(out, "closeTimeout", config, long.class, TimeUnit.class);
        return AsyncReporter.Builder.class.cast(out).build();
    }

    private Sender toUrlSender(final String reporter) {
        final Map<String, String> configuration = readConfiguration(reporter);
        if (!configuration.containsKey("endpoint")) {
            throw new IllegalArgumentException(
                    "Please pass endpoint configuration to the url reporter: url(endpoint=....)");
        }

        try {
            Object builder = Thread
                    .currentThread()
                    .getContextClassLoader()
                    .loadClass("zipkin2.reporter.urlconnection.URLConnectionSender")
                    .getMethod("create", String.class)
                    .invoke(null, configuration.get("endpoint"));
            builder = builderSet(builder, "connectTimeout", configuration, int.class);
            builder = builderSet(builder, "readTimeout", configuration, int.class);
            builder = builderSet(builder, "messageMaxBytes", configuration, int.class);
            builder = builderSet(builder, "compressionEnabled", configuration, boolean.class);
            builder = builderSet(builder, "encoding", configuration, Encoding.class);
            return Sender.class.cast(builder.getClass().getMethod("build").invoke(builder));
        } catch (final InvocationTargetException e) {
            throw new IllegalArgumentException(e.getTargetException());
        } catch (final IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "You surely have a not compatible zipkin-sender-urlconnection dependency");
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Did you add io.zipkin.reporter2:zipkin-sender-urlconnection to the classpath?", e);
        }
    }

    private Sender toKafkaSender(final String reporter) {
        final Map<String, String> configuration = readConfiguration(reporter);
        if (!configuration.containsKey("servers")) {
            throw new IllegalArgumentException(
                    "Please pass servers configuration to the kafka reporter: kafka(servers=....)");
        }

        try {
            Object builder = Thread
                    .currentThread()
                    .getContextClassLoader()
                    .loadClass("zipkin2.reporter.kafka11.KafkaSender")
                    .getMethod("create", String.class)
                    .invoke(null, configuration.get("servers"));
            builder = builderSet(builder, "messageMaxBytes", configuration, int.class);
            builder = builderSet(builder, "encoding", configuration, Encoding.class);
            builder = builderSet(builder, "overrides", configuration, Map.class);
            return Sender.class.cast(builder.getClass().getMethod("build").invoke(builder));
        } catch (final InvocationTargetException e) {
            throw new IllegalArgumentException(e.getTargetException());
        } catch (final IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalArgumentException("You surely have a not compatible zipkin-sender-kafka11 dependency");
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Did you add io.zipkin.reporter2:zipkin-sender-kafka11 to the classpath?",
                    e);
        }
    }

    private Object builderSet(final Object builder, final String name, final Map<String, String> config,
            final Class<?> type, final Class<?>... otherParams) {
        if (!config.containsKey(name)) {
            return builder;
        }
        Object value = config.get(name);
        if (int.class == type) {
            value = Integer.parseInt(value.toString().trim());
        } else if (boolean.class == type) {
            value = Boolean.parseBoolean(value.toString().trim());
        } else if (Encoding.class == type) {
            value = Encoding.valueOf(value.toString().trim());
        } else if (Map.class == type) {
            value = readConfiguration(value.toString());
        }
        final Class<?>[] params =
                Stream.concat(Stream.of(type), otherParams == null ? Stream.empty() : Stream.of(otherParams)).toArray(
                        Class[]::new);
        final Object[] values = new Object[params.length];
        values[0] = value;
        if (otherParams != null) {
            for (int i = 0; i < otherParams.length; i++) {
                if (otherParams[i] == TimeUnit.class) {
                    values[i + 1] = TimeUnit.MILLISECONDS;
                } else {
                    throw new IllegalArgumentException("Unsupported type: " + otherParams[i]);
                }
            }
        }

        try {
            return builder.getClass().getMethod(name, params).invoke(builder, values);
        } catch (final NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (final InvocationTargetException e) {
            throw new IllegalArgumentException(e.getTargetException());
        }
    }

    private Map<String, String> readConfiguration(final String reporter) {
        return new StringPropertiesTokenizer(
                reporter.contains("(") ? reporter.substring(reporter.indexOf('(') + 1, reporter.length() - 1) : "")
                        .tokens()
                        .stream()
                        .map(data -> data.split("="))
                        .collect(toMap(s -> s[0], s -> s[1]));

    }
}
