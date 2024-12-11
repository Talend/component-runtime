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
package org.talend.sdk.component.starter.server.front.js;

import java.io.IOException;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

// enforce the server to redirect to the client to handle the 404 to have a nice ui
@WebFilter(asyncSupported = true, urlPatterns = "/*")
public class IndexRedirector implements Filter {

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = HttpServletRequest.class.cast(servletRequest);
        System.out.println("doFilter with request : " + httpServletRequest.getRequestURI());
        if (exists(httpServletRequest.getRequestURI())) {
            System.out.println("doFilter : exists");
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            filterChain.doFilter(new HttpServletRequestWrapper(httpServletRequest) {

                @Override
                public String getPathInfo() {
                    System.out.println("doFilter : not exists - path");
                    return "";
                }

                @Override
                public String getServletPath() {
                    System.out.println("doFilter : not exists - servlet path");
                    return "/index.html";
                }
            }, servletResponse);
        }
    }

    private boolean exists(final String requestURI) {
        Boolean found = requestURI.startsWith("/api") || Stream
                .of(".png", ".html", ".js", ".js.map", ".css", ".css.map", ".json", ".ico", ".woff", ".woff2")
                .anyMatch(requestURI::contains);
        System.out.println(requestURI + " -> " + found);
        return found;
    }
}
