/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.component.server.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.talend.component.server.configuration.ComponentServerConfiguration;

@ApplicationScoped
public class PoolProducer {

    @Produces
    @ApplicationScoped
    public ExecutorService executorService(final ComponentServerConfiguration configuration) {
        return Executors.newFixedThreadPool(configuration.executionPoolSize());
    }

    public void release(@Disposes final ExecutorService executorService, final ComponentServerConfiguration configuration) {
        final long timeout = Duration.parse(configuration.executionPoolShutdownTimeout()).toMillis();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout, MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (final InterruptedException e) {
            Thread.interrupted();
        }
    }
}
