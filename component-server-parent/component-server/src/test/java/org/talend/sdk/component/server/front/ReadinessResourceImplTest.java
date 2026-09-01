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
package org.talend.sdk.component.server.front;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.front.model.HealthStatus;
import org.talend.sdk.component.server.service.ComponentManagerService;
import org.talend.sdk.components.vault.client.VaultClient;

class ReadinessResourceImplTest {

    private AutoCloseable closeable;

    @Mock
    private ComponentManagerService componentManagerService;

    @Mock
    private ComponentServerConfiguration configuration;

    @Mock
    private VaultClient vaultClient;

    @InjectMocks
    private ReadinessResourceImpl readinessResource;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void returns503WhenIndexNotReady() {
        when(componentManagerService.isStarted()).thenReturn(false);

        final Response response = readinessResource.getReadiness();

        assertEquals(503, response.getStatus());
        final HealthStatus status = (HealthStatus) response.getEntity();
        assertEquals("DOWN", status.getStatus());
        assertEquals("Component index not ready", status.getCause());
    }

    @Test
    void returns200WhenIndexReadyAndVaultReachable() {
        when(componentManagerService.isStarted()).thenReturn(true);
        when(configuration.getHealthVaultEnabled()).thenReturn(false);

        final Response response = readinessResource.getReadiness();

        assertEquals(200, response.getStatus());
        final HealthStatus status = (HealthStatus) response.getEntity();
        assertEquals("UP", status.getStatus());
    }

    @Test
    void returns503WhenVaultNotReachable() {
        when(componentManagerService.isStarted()).thenReturn(true);
        when(configuration.getHealthVaultEnabled()).thenReturn(true);
        when(vaultClient.ping()).thenReturn(false);

        final Response response = readinessResource.getReadiness();

        assertEquals(503, response.getStatus());
        final HealthStatus status = (HealthStatus) response.getEntity();
        assertEquals("DOWN", status.getStatus());
        assertEquals("Vault is not reachable", status.getCause());
    }

    @Test
    void returns503WhenVaultThrows() {
        when(componentManagerService.isStarted()).thenReturn(true);
        when(configuration.getHealthVaultEnabled()).thenReturn(true);
        when(vaultClient.ping()).thenThrow(new RuntimeException("connection refused"));

        final Response response = readinessResource.getReadiness();

        assertEquals(503, response.getStatus());
        final HealthStatus status = (HealthStatus) response.getEntity();
        assertEquals("DOWN", status.getStatus());
    }

    @Test
    void concurrentReadinessChecksTriggerVaultPingOnlyOnceAtTtlExpiry() throws Exception {
        when(componentManagerService.isStarted()).thenReturn(true);
        when(configuration.getHealthVaultEnabled()).thenReturn(true);
        when(vaultClient.ping()).thenAnswer(invocation -> {
            // widen the race window so concurrent callers are likely to observe the expired cache
            Thread.sleep(50);
            return true;
        });

        final int threadCount = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch ready = new CountDownLatch(threadCount);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Response>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return readinessResource.getReadiness();
                }));
            }
            ready.await();
            start.countDown();
            for (final Future<Response> future : futures) {
                assertEquals(200, future.get().getStatus());
            }
        } finally {
            executor.shutdown();
        }

        // exactly one thread should have performed the actual Vault ping for this TTL window
        verify(vaultClient, times(1)).ping();
    }
}
