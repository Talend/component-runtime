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

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;

@TestInstance(PER_CLASS)
class SecuredFilterTest {

    private final Collection<String> tokens = new ArrayList<>(singletonList("-"));

    private final SecuredFilter filter = new SecuredFilter() {

        {
            try {
                final Field configuration = SecuredFilter.class.getDeclaredField("configuration");
                configuration.setAccessible(true);
                configuration.set(this, new ComponentServerConfiguration() {

                    @Override
                    public String getSecuredEndpointsTokens() {
                        return String.join(",", tokens);
                    }
                });
            } catch (final Exception e) {
                fail(e.getMessage());
            }
        }
    };

    @BeforeEach
    void resetTokens() {
        tokens.clear();
        tokens.add("-");
        filter.init(null);
    }

    @Test
    void localIsAllowed() {
        assertEquals(HttpServletResponse.SC_OK, executeAndGetStatus(newRequest(null, "127.0.0.1")));
    }

    @Test
    void loggedIsAllowed() {
        tokens.add("logged");
        filter.init(null);
        assertEquals(HttpServletResponse.SC_OK, executeAndGetStatus(newRequest("logged", "127.0.0.1")));
        assertEquals(HttpServletResponse.SC_OK, executeAndGetStatus(newRequest("logged", "10.8.17.25")));
    }

    @Test
    void anonymousRemoteFails() {
        assertEquals(HttpServletResponse.SC_NOT_FOUND, executeAndGetStatus(newRequest(null, "10.8.17.25")));
    }

    @Test
    void badTokenFails() {
        assertEquals(HttpServletResponse.SC_NOT_FOUND, executeAndGetStatus(newRequest("notset", "10.8.17.25")));
    }

    private int executeAndGetStatus(final HttpServletRequest request) {
        final AtomicInteger status = new AtomicInteger(-1);
        try {
            filter
                    .doFilter(request, HttpServletResponse.class
                            .cast(Proxy
                                    .newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                            new Class<?>[] { HttpServletResponse.class }, (proxy, method, args) -> {
                                                if (method.getName().equals("setStatus")) {
                                                    status.set(Integer.class.cast(args[0]));
                                                }
                                                return null;
                                            })),
                            (req, response) -> status.set(HttpServletResponse.SC_OK));
        } catch (final IOException | ServletException e) {
            fail(e.getMessage());
        }
        return status.get();
    }

    private HttpServletRequest newRequest(final String authHeader, final String remoteAddr) {
        return HttpServletRequest.class
                .cast(Proxy
                        .newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {
                                    if (method.getName().equals("getHeader")
                                            && "Authorization".equalsIgnoreCase(String.valueOf(args[0]))) {
                                        return authHeader;
                                    }
                                    if (method.getName().equalsIgnoreCase("getRemoteAddr")) {
                                        return remoteAddr;
                                    }
                                    return null;
                                }));
    }
}
