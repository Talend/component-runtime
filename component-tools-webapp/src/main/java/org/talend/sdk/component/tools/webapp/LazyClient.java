/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tools.webapp;

import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.internal.jaxrs.JAXRSClient;

import lombok.experimental.Delegate;

@ApplicationScoped
public class LazyClient implements Client<Object> {

    @Delegate
    private volatile Client<Object> client;

    private ExecutorService executorService;

    @Produces
    private volatile WebTarget webTarget;

    void lazyInit(final Supplier<String> base) {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    final String baseValue = base.get();
                    final AtomicInteger counter = new AtomicInteger(1);
                    executorService = Executors
                            .newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 32, new ThreadFactory() {

                                @Override
                                public Thread newThread(final Runnable r) {
                                    final Thread thread =
                                            new Thread(r, "jaxrs-client-actions-" + counter.getAndIncrement());
                                    thread.setDaemon(false);
                                    thread.setPriority(Thread.NORM_PRIORITY);
                                    thread.setContextClassLoader(LazyClient.class.getClassLoader());
                                    return thread;
                                }
                            });
                    final javax.ws.rs.client.Client jaxrsClient =
                            ClientBuilder.newBuilder().property("executorService", executorService).build();
                    webTarget = jaxrsClient.target(baseValue);
                    client = new JAXRSClient<>(jaxrsClient, baseValue, true);
                }
            }
        }
    }

    @PreDestroy
    private void onDestroy() {
        ofNullable(executorService).ifPresent(ExecutorService::shutdownNow);
        client.close();
    }

    @Dependent
    @WebFilter(urlPatterns = "/api/v1/*", asyncSupported = true)
    public static class LazyInitializer implements Filter {

        @Inject
        private LazyClient lazyClient;

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            lazyClient.lazyInit(() -> {
                final HttpServletRequest servletRequest = HttpServletRequest.class.cast(request);
                return String
                        .format("%s://%s:%d/%s", servletRequest.getScheme(), servletRequest.getServerName(),
                                servletRequest.getServerPort(), "api/v1");
            });
            chain.doFilter(request, response);
        }
    }
}
