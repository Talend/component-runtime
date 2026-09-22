/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.webapp;

import static jakarta.ws.rs.client.Entity.entity;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;

import org.talend.sdk.component.form.api.Client;

/**
 * jakarta.ws.rs based twin of {@link org.talend.sdk.component.form.internal.jaxrs.JAXRSClient}, kept local to this
 * module since component-form-core stays on javax.ws.rs (it must remain usable by connectors, which are unaffected
 * by the CXF 4.x/jakarta migration of component-server and this webapp).
 */
public class JakartaJAXRSClient<T> implements Client<T> {

    private final jakarta.ws.rs.client.Client delegate;

    private final WebTarget target;

    private final boolean closeClient;

    private final GenericType<Map<String, Object>> mapType;

    public JakartaJAXRSClient(final String base) {
        this(newClient(), base, true);
    }

    public JakartaJAXRSClient(final jakarta.ws.rs.client.Client client, final String base,
            final boolean closeClient) {
        this.delegate = client;
        this.closeClient = closeClient;
        this.target = client.target(base);
        this.mapType = new GenericType<Map<String, Object>>() {
        };
    }

    @Override
    public CompletableFuture<Map<String, Object>> action(final String family, final String type, final String action,
            final String lang, final Map<String, Object> params, final T context) {
        final Map<Object, Object> payload =
                params.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
        return target
                .path("action/execute")
                .queryParam("family", family)
                .queryParam("type", type)
                .queryParam("action", action)
                .queryParam("lang", lang)
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .post(entity(payload, APPLICATION_JSON_TYPE), mapType)
                .toCompletableFuture();
    }

    @Override
    public void close() {
        if (closeClient) {
            delegate.close();
        }
    }

    private static jakarta.ws.rs.client.Client newClient() {
        final jakarta.ws.rs.client.Client instance = ClientBuilder.newClient();
        System
                .getProperties()
                .stringPropertyNames()
                .stream()
                .filter(k -> k.startsWith("talend.component.form.client.jaxrs.properties."))
                .forEach(k -> instance
                        .property(k.substring("talend.component.form.client.jaxrs.properties.".length()),
                                System.getProperty(k)));
        return instance;
    }
}
