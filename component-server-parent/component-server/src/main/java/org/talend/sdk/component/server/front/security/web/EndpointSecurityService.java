/**
 * Copyright (C) 2006-2021 Talend Inc. - www.talend.com
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

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;

import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;

@ApplicationScoped
public class EndpointSecurityService {

    @Inject
    private ComponentServerConfiguration configuration;

    private Set<String> tokens;

    @PostConstruct
    private void init() {
        tokens = Stream
                .of(configuration.getSecuredEndpointsTokens().split(","))
                .map(String::trim)
                .filter(it -> !it.isEmpty() && !"-".equals(it))
                .collect(toSet());
    }

    public boolean isAllowed(final ServletRequest servletRequest) {
        return isSecured(servletRequest) || isLocal(servletRequest);
    }

    private boolean isLocal(final String addr) {
        return addr != null && (addr.startsWith("127.0.0.") || addr.equals("::1") || addr.equals("0:0:0:0:0:0:0:1"));
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
}
