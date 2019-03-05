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
package org.talend.sdk.component.server.extension.stitch;

import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

@ApplicationScoped
public class ClientProducer {

    @Ext
    @Produces
    @ApplicationScoped
    public ExecutorService pool(final StitchConfiguration configuration) {
        final int threads = configuration.getParallelism() <= 0 ? Runtime.getRuntime().availableProcessors()
                : configuration.getParallelism();
        return new ThreadPoolExecutor(threads, threads, 1, MINUTES, new LinkedBlockingQueue<>(),
                new StitchThreadFactory());
    }

    public void releasePool(@Disposes @Ext final ExecutorService executorService) {
        executorService.shutdownNow();
        try {
            executorService.awaitTermination(1, MINUTES);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Ext
    @Produces
    @ApplicationScoped
    public Client client(final StitchConfiguration configuration, @Ext final ExecutorService stitchExecutor) {
        final ClientBuilder builder = ClientBuilder.newBuilder();
        builder.connectTimeout(configuration.getConnectTimeout(), MILLISECONDS);
        builder.readTimeout(configuration.getReadTimeout(), MILLISECONDS);
        builder.executorService(stitchExecutor);
        return builder.build();
    }

    public void releaseClient(@Disposes @Ext final Client client) {
        client.close();
    }

    @Ext
    @Produces
    @ApplicationScoped
    public WebTarget webTarget(@Ext final Client client, final StitchConfiguration configuration) {
        return client.target(configuration.getBase());
    }

    private static class StitchThreadFactory implements ThreadFactory {

        private final ThreadGroup group = ofNullable(System.getSecurityManager())
                .map(SecurityManager::getThreadGroup)
                .orElseGet(() -> Thread.currentThread().getThreadGroup());

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(final Runnable worker) {
            final Thread t =
                    new Thread(group, worker, "talend-server-stitch-extension-" + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
