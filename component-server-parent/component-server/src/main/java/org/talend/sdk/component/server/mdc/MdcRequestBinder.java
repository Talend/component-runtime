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
package org.talend.sdk.component.server.mdc;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Vetoed;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.ThreadContext;

@Vetoed
public class MdcRequestBinder implements Filter {

    private String hostname;

    @Override
    public void init(final FilterConfig filterConfig) {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = ofNullable(System.getenv("HOST")).orElse("unknown");
        }
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (HttpServletRequest.class.isInstance(request)) {
            ThreadContext.putAll(createContext(HttpServletRequest.class.cast(request)));
        }
        chain.doFilter(request, response);
    }

    private Map<String, String> createContext(final HttpServletRequest req) {
        final Map<String, String> map = new HashMap<>();
        ofNullable(req.getHeader("X-B3-TraceId")).ifPresent(v -> map.put("spanId", v));
        ofNullable(req.getHeader("X-B3-SpanId")).ifPresent(v -> map.put("traceId", v));
        map.put("hostname", hostname);
        return map;
    }
}
