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
package org.talend.sdk.component.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.front.model.HealthStatus;
import org.talend.sdk.components.vault.client.VaultClient;

class HealthServiceTest {

    private AutoCloseable closeable;

    @Mock
    private ComponentManagerService componentManagerService;

    @Mock
    private VaultClient vaultClient;

    @Mock
    private ComponentServerConfiguration configuration;

    @InjectMocks
    private HealthService healthService;

    @BeforeEach
    void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        when(configuration.getHealthMemoryThreshold()).thenReturn(10);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void livenessReturnUpWhenAllChecksPass() {
        final ComponentManager manager = mock(ComponentManager.class);
        final ContainerManager containerManager = mock(ContainerManager.class);
        when(componentManagerService.manager()).thenReturn(manager);
        when(manager.getContainer()).thenReturn(containerManager);
        when(vaultClient.ping()).thenReturn(true);

        final HealthStatus status = healthService.checkLiveness();

        assertEquals("UP", status.getStatus());
        assertNull(status.getCause());
    }

    @Test
    void livenessReturnDownWhenHeapBelowThreshold() {
        when(configuration.getHealthMemoryThreshold()).thenReturn(101);

        final HealthStatus status = healthService.checkLiveness();

        assertEquals("DOWN", status.getStatus());
        assertNotNull(status.getCause());
    }

    @Test
    void livenessReturnDownWhenComponentIndexThrows() {
        final ComponentManager manager = mock(ComponentManager.class);
        when(componentManagerService.manager()).thenReturn(manager);
        when(manager.getContainer()).thenThrow(new RuntimeException("index failure"));

        final HealthStatus status = healthService.checkLiveness();

        assertEquals("DOWN", status.getStatus());
        assertNotNull(status.getCause());
    }

    @Test
    void livenessReturnDownWhenVaultPingFails() {
        final ComponentManager manager = mock(ComponentManager.class);
        final ContainerManager containerManager = mock(ContainerManager.class);
        when(componentManagerService.manager()).thenReturn(manager);
        when(manager.getContainer()).thenReturn(containerManager);
        when(vaultClient.ping()).thenReturn(false);

        final HealthStatus status = healthService.checkLiveness();

        assertEquals("DOWN", status.getStatus());
        assertEquals("Vault is not reachable", status.getCause());
    }

    @Test
    void readinessReturnUpWhenStarted() {
        when(componentManagerService.isStarted()).thenReturn(true);

        final HealthStatus status = healthService.checkReadiness();

        assertEquals("UP", status.getStatus());
        assertNull(status.getCause());
    }

    @Test
    void readinessReturnDownWhenNotStarted() {
        when(componentManagerService.isStarted()).thenReturn(false);

        final HealthStatus status = healthService.checkReadiness();

        assertEquals("DOWN", status.getStatus());
        assertEquals("Component index not ready", status.getCause());
    }

    @Test
    void environmentRemainsIndependentOfHealthChecks() {
        when(componentManagerService.isStarted()).thenReturn(false);

        final HealthStatus readiness = healthService.checkReadiness();

        assertEquals("DOWN", readiness.getStatus());
    }
}
