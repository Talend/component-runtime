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
package org.talend.sdk.component.server.service.metrics;

import static java.util.Optional.ofNullable;

import java.io.IOException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Dependent
@WebFilter(urlPatterns = "/api/v1/metrics")
public class MetricsToggle implements Filter {

    @Inject
    private ComponentServerConfiguration configuration;

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain filterChain) throws IOException, ServletException {
        final String remoteAddr = ofNullable(servletRequest.getRemoteAddr()).orElse("x");
        if (configuration.getSupportsMetrics() && remoteAddr.startsWith("127.") || remoteAddr.startsWith("::1")) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            final HttpServletResponse response = HttpServletResponse.class.cast(servletResponse);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }
}
