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
package org.talend.sdk.component.runtime.server.vault.proxy.endpoint.jaxrs;

import static java.util.Locale.ROOT;
import static lombok.AccessLevel.PRIVATE;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.Response;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Responses {

    public static CompletionStage<Response> decorate(final CompletionStage<Response> source) {
        return source.thenApply(Responses::decorate);
    }

    public static Response decorate(final Response source) {
        final Response.ResponseBuilder builder = Response.status(source.getStatus());
        source
                .getStringHeaders()
                .entrySet()
                .stream()
                .filter(it -> !isBlacklistedHeader(it.getKey()))
                .forEach(e -> builder.header(e.getKey(), String.join(",", e.getValue())));
        return builder.entity(loadInMemory(source)).build();
    }

    // to be cache friendly
    private static byte[] loadInMemory(final Response source) {
        return source.readEntity(byte[].class);
    }

    private static boolean isBlacklistedHeader(final String name) {
        final String header = name.toLowerCase(ROOT);
        // tracing headers, setup recomputes them to propagate them properly
        return header.startsWith("x-b3-") || header.startsWith("baggage-")
        // set by container
                || header.startsWith("content-type") || header.startsWith("transfer-encoding");
    }
}
