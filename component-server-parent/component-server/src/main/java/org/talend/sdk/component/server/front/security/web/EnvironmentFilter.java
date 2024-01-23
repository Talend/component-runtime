/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

@Dependent
@WebFilter(urlPatterns = { "/api/v1/environment", "/api/v1/environment/" })
public class EnvironmentFilter implements Filter {

    @Inject
    private ComponentServerConfiguration configuration;

    @Override
    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain filterChain) throws IOException, ServletException {
        if (configuration.getSupportsEnvironment()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final HttpServletResponse response = HttpServletResponse.class.cast(servletResponse);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}
