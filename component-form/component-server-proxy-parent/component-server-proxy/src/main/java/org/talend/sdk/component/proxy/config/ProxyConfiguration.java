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
package org.talend.sdk.component.proxy.config;

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.NONE;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Invocation;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.Getter;

@Getter
@ApplicationScoped
public class ProxyConfiguration {

    private static final String PREFIX = "talend.component.proxy.";

    @Inject
    @Documentation("The base to contact the remote server "
            + "(NOTE: it is recommanded to put a load balancer if you have multiple instances.)")
    @ConfigProperty(name = PREFIX + "server.base")
    private String targetServerBase;

    @Inject
    @Documentation("List of JAX-RS providers to register on the client, at least a JSON-B one should be here.")
    @ConfigProperty(name = PREFIX + "client.providers")
    private Collection<Class> clientProviders;

    @Inject
    @Getter(NONE)
    @Documentation("The headers to append to the request when contacting the server. Format is a properties one. "
            + "You can put a hardcoded value or a placeholder (`${key}`)."
            + "In this case it will be read from the request attributes and headers.")
    @ConfigProperty(name = PREFIX + "processing.headers")
    private String headers;

    @Getter
    private BiFunction<Invocation.Builder, Function<String, String>, Invocation.Builder> headerAppender;

    @PostConstruct
    private void init() {
        processHeaders();
    }

    private void processHeaders() {
        if (headers == null || headers.isEmpty()) {
            headerAppender = (a, b) -> a;
        } else {
            final Properties properties = new Properties();
            try (final Reader reader = new StringReader(headers.trim())) {
                properties.load(reader);
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
            final Map<String, Function<Function<String, String>, String>> providers =
                    properties.stringPropertyNames().stream().collect(toMap(identity(), e -> {
                        final String value = properties.getProperty(e);
                        if (value.contains("${") && value.contains("}")) {
                            final Map<String, String> toReplace = new HashMap<>();
                            int lastEnd = -1;
                            do {
                                final int start = value.indexOf("${", lastEnd);
                                if (start < 0) {
                                    break;
                                }
                                final int end = value.indexOf('}', start);
                                if (end < start) {
                                    break;
                                }

                                toReplace.put(value.substring(start, end + 1),
                                        value.substring(start + "${".length(), end));
                                lastEnd = end;
                            } while (lastEnd > 0);
                            if (!toReplace.isEmpty()) {
                                return placeholders -> {
                                    String output = value;
                                    for (final Map.Entry<String, String> placeholder : toReplace.entrySet()) {
                                        output = output.replace(placeholder.getKey(),
                                                ofNullable(placeholders.apply(placeholder.getValue())).orElse(""));
                                    }
                                    return output;
                                };
                            }
                        }
                        return ignored -> value;
                    }));
            headerAppender = (builder, placeholders) -> {
                Invocation.Builder out = builder;
                for (final Map.Entry<String, Function<Function<String, String>, String>> header : providers
                        .entrySet()) {
                    out = out.header(header.getKey(), header.getValue().apply(placeholders));
                }
                return out;
            };
        }
    }
}
