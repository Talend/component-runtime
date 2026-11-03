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
package org.talend.sdk.component.tcketl.web;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.talend.sdk.component.tcketl.config.ApplicationConfig;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;

/**
 * The deployment settings of the web application ({@code tcketl.web.*}), as opposed to the connection settings a user
 * edits in the browser.
 */
@Component
public class WebSettings {

    private final ServerEndpoint defaultServer;

    private final List<String> allowedHosts;

    private final boolean blockPrivateHosts;

    public WebSettings(@Value("${tcketl.web.default-server:http://localhost:8080/api/v1}") final String defaultServer,
            @Value("${tcketl.web.allowed-hosts:}") final String allowedHosts,
            @Value("${tcketl.web.block-private-hosts:false}") final boolean blockPrivateHosts) {
        this.blockPrivateHosts = blockPrivateHosts;
        this.defaultServer = parse(defaultServer);
        this.allowedHosts = Arrays
                .stream(allowedHosts.split(","))
                .map(host -> host.strip().toLowerCase(Locale.ROOT))
                .filter(host -> !host.isEmpty())
                .toList();
    }

    /** What a new browser tab starts with. */
    public ApplicationConfig initialConfig() {
        return ApplicationConfig.defaults().withServer(defaultServer);
    }

    /**
     * The browser users choose the server the web application calls, so the hosts can be restricted. An explicit list
     * wins over everything else; without a list, every host may be called unless private hosts are blocked, in which
     * case a host that resolves to a loopback, link-local, site-local or wildcard address is refused, which keeps the
     * application from being used to reach the network behind it. The default server is always allowed.
     *
     * @return {@code true} when the host may be called
     */
    public boolean isAllowed(final String host) {
        final String normalized = host.strip().toLowerCase(Locale.ROOT);
        if (normalized.equals(defaultServer.host().toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (!allowedHosts.isEmpty()) {
            return allowedHosts.contains(normalized);
        }
        return !blockPrivateHosts || isPublic(normalized);
    }

    private static boolean isPublic(final String host) {
        if (host.isEmpty()) {
            return false;
        }
        try {
            for (final InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                        || address.isAnyLocalAddress() || address.isMulticastAddress()) {
                    return false;
                }
            }
            return true;
        } catch (final UnknownHostException e) {
            return false;
        }
    }

    public List<String> allowedHosts() {
        return allowedHosts;
    }

    static ServerEndpoint parse(final String url) {
        final URI uri = URI.create(url.strip());
        final String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        final int port = uri.getPort() > 0 ? uri.getPort() : "https".equals(scheme) ? 443 : 80;
        return new ServerEndpoint(scheme, uri.getHost(), port, uri.getPath());
    }
}
