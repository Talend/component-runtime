// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.server.service;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.talend.components.server.configuration.ComponentServerConfiguration;

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
