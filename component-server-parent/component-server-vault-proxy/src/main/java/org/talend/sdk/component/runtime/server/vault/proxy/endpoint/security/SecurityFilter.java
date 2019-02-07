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

import org.eclipse.microprofile.config.inject.ConfigProperty;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Dependent
@WebFilter(asyncSupported = true, urlPatterns = "/api/*")
public class SecurityFilter implements Filter {

    @Inject
    @ConfigProperty(name = "talend.vault.cache.vault.security.allowedIps",
            defaultValue = "localhost,127.0.0.1,0:0:0:0:0:0:0:1")
    private List<String> allowedIp;

    @Override
    public void init(final FilterConfig filterConfig) {
        if (log.isDebugEnabled()) {
            log.debug("Allowed remote hosts: {}", allowedIp);
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (!isAllowed(HttpServletRequest.class.cast(request))) {
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
        return allowedIp.contains(request.getRemoteAddr()) || allowedIp.contains(request.getRemoteHost());
    }
}
