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
package org.talend.sdk.component.tcketl.domain.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import org.talend.sdk.component.tcketl.domain.Preconditions;

/**
 * Where the component server is. {@code basePath} is everything that precedes the resource paths (for example
 * {@code /api/v1}), normalized to a leading slash and no trailing slash, or empty for the root.
 */
public record ServerEndpoint(String scheme, String host, int port, String basePath) {

    public static final String DEFAULT_BASE_PATH = "/api/v1";

    private static final int MAX_PORT = 65535;

    public ServerEndpoint {
        scheme = Preconditions.requireNonBlank(scheme, "scheme").strip().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("scheme must be http or https: " + scheme);
        }
        host = Preconditions.requireNonBlank(host, "host").strip();
        if (port < 1 || port > MAX_PORT) {
            throw new IllegalArgumentException("port must be between 1 and " + MAX_PORT + ": " + port);
        }
        basePath = normalize(basePath);
        final URI uri = toUri(scheme, host, port, basePath);
        // a host such as "a/b" or "http://localhost" parses as another authority, so the round trip must match
        if (uri.getHost() == null || uri.getPort() != port) {
            throw new IllegalArgumentException("host must be a plain host name or address: " + host);
        }
    }

    public static ServerEndpoint defaults() {
        return new ServerEndpoint("http", "localhost", 8080, DEFAULT_BASE_PATH);
    }

    public URI baseUri() {
        return toUri(scheme, host, port, basePath);
    }

    @Override
    public String toString() {
        return baseUri().toString();
    }

    private static String normalize(final String path) {
        String normalized = Preconditions.nullToEmpty(path).strip();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty() || normalized.startsWith("/")) {
            return normalized;
        }
        return "/" + normalized;
    }

    private static URI toUri(final String scheme, final String host, final int port, final String path) {
        try {
            return new URI(scheme, null, host, port, path, null, null);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Invalid server endpoint: " + e.getReason(), e);
        }
    }
}
