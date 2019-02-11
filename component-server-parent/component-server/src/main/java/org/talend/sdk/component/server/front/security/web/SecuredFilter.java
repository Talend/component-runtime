/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front.security.web;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;

public abstract class SecuredFilter implements Filter {

    @Inject
    private ComponentServerConfiguration configuration;

    private Set<String> tokens;

    @Override
    public void init(final FilterConfig filterConfig) {
        tokens = Stream
                .of(configuration.getSecuredEndpointsTokens().split(","))
                .map(String::trim)
                .filter(it -> !it.isEmpty() && !"-".equals(it))
                .collect(toSet());
    }

    @Override
    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain filterChain) throws IOException, ServletException {
        if ((isLocal(servletRequest) || isSecured(servletRequest)) && canCall(servletRequest)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final HttpServletResponse response = HttpServletResponse.class.cast(servletResponse);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    protected boolean canCall(final ServletRequest servletRequest) {
        return true;
    }

    private boolean isSecured(final ServletRequest servletRequest) {
        final String authorization = HttpServletRequest.class.cast(servletRequest).getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null && tokens.contains(authorization);
    }

    private boolean isLocal(final ServletRequest servletRequest) {
        return HttpServletRequest.class.isInstance(servletRequest)
                && ofNullable(HttpServletRequest.class.cast(servletRequest).getRemoteAddr())
                        .map(this::isLocal)
                        .orElse(false);
    }

    private boolean isLocal(final String addr) {
        return addr != null && (addr.startsWith("127.0.0.") || addr.equals("::1") || addr.equals("0:0:0:0:0:0:0:1"));
    }
}
