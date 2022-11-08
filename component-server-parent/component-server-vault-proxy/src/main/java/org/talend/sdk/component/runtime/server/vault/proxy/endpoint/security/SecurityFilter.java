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
package org.talend.sdk.component.runtime.server.vault.proxy.endpoint.security;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.talend.sdk.component.runtime.server.vault.proxy.configuration.Documentation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Dependent
@WebFilter(asyncSupported = true, urlPatterns = "/api/*")
public class SecurityFilter implements Filter {

    @Inject
    @Documentation("The IP or hosts allowed to call that server on `/api/*` if no token is passed.")
    @ConfigProperty(name = "talend.vault.cache.security.allowedIps",
            defaultValue = "localhost,127.0.0.1,0:0:0:0:0:0:0:1")
    private List<String> allowedIp;

    @Inject
    @Documentation("The tokens enabling a client to call this server without being in `allowedIp` whitelist.")
    @ConfigProperty(name = "talend.vault.cache.security.tokens", defaultValue = "-")
    private List<String> securedEndpointsTokens;

    @Inject
    @Documentation("Enable to sanitize the hostname before testing them. Default to `none` which is a noop. Supported values "
            + "are `docker` (for `<folder>_<service>_<number>.<folder>_<network>` pattern) and `weave` (for `<prefix>_dataset_<number>.<suffix>` pattern).")
    @ConfigProperty(name = "talend.vault.cache.security.hostname.sanitizer", defaultValue = "none")
    private String sanitization;

    @Inject
    private DockerHostNameSanitizer dockerSanitizer;

    @Override
    public void init(final FilterConfig filterConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Allowed remote hosts: {}", allowedIp);
        }
        if (sanitization != null) {
            log.info("Activating {} mode, hosts will be rewritten to extract service names", sanitization);
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!isAllowed(HttpServletRequest.class.cast(request)) && !isSecured(request)) {
            final HttpServletResponse httpServletResponse = HttpServletResponse.class.cast(response);
            httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            httpServletResponse.setContentType("application/json");
            response.getWriter().write("{}");
            return;
        }
        chain.doFilter(request, response);
    }

    // cheap security
    private boolean isAllowed(final HttpServletRequest request) {
        return allowedIp.contains(request.getRemoteAddr()) || allowedIp.contains(sanitizeHost(request.getRemoteHost()));
    }

    private String sanitizeHost(final String remoteHost) {
        switch (sanitization) {
        case "docker":
            return logHostSanitization(remoteHost, dockerSanitizer.sanitizeDockerHostname(remoteHost));
        case "weave":
            return logHostSanitization(remoteHost, dockerSanitizer.sanitizeWeaveHostname(remoteHost));
        case "none":
        default:
            return remoteHost;
        }
    }

    private String logHostSanitization(final String original, final String rewritten) {
        log.debug("Mapped '{}' on '{}'", original, rewritten);
        return rewritten;
    }

    private boolean isSecured(final ServletRequest servletRequest) {
        final String authorization = HttpServletRequest.class.cast(servletRequest).getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && !"-".equalsIgnoreCase(authorization)
                && securedEndpointsTokens.contains(authorization);
    }
}
