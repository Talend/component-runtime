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
package org.talend.sdk.component.starter.server.configuration;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.filters.HttpHeaderSecurityFilter;

public class ServletConfigurer implements ServletContainerInitializer {

    @Override
    public void onStartup(final Set<Class<?>> set, final ServletContext servletContext) {
        addFilter(servletContext, "security-filter", HttpHeaderSecurityFilter.class);
        addFilter(servletContext, "encoding-filter", EncodingFilter.class);
        addFilter(servletContext, "csp-filter", CSPFilter.class);
    }

    private void addFilter(final ServletContext servletContext, final String name, final Class<? extends Filter> type) {
        final FilterRegistration.Dynamic reg = servletContext.addFilter(name, type);
        reg.setAsyncSupported(true);
        reg.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

    public static class CSPFilter implements Filter {

        @Inject
        private StarterConfiguration configuration;

        @Override
        public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                final FilterChain filterChain) throws IOException, ServletException {
            HttpServletResponse.class
                    .cast(servletResponse)
                    .addHeader("Content-Security-Policy", configuration.getCsp());
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public static class EncodingFilter implements Filter {

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            response.setCharacterEncoding("UTF-8");
            chain.doFilter(request, response);
        }
    }
}
