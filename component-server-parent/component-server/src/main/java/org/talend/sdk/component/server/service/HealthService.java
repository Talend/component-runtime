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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.front.model.HealthStatus;
import org.talend.sdk.components.vault.client.VaultClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class HealthService {

    private static final String STATUS_UP = "UP";

    private static final String STATUS_DOWN = "DOWN";

    @Inject
    private ComponentManagerService componentManagerService;

    @Inject
    private VaultClient vaultClient;

    @Inject
    private ComponentServerConfiguration configuration;

    /**
     * Performs the liveness check: heap availability, component index, and Vault connectivity.
     *
     * @return {@link HealthStatus} with {@code status="UP"} if all checks pass,
     * or {@code status="DOWN"} with a human-readable cause on failure.
     */
    public HealthStatus checkLiveness() {
        final HealthStatus memoryStatus = checkMemory();
        if (STATUS_DOWN.equals(memoryStatus.getStatus())) {
            return memoryStatus;
        }
        final HealthStatus indexStatus = checkComponentIndex();
        if (STATUS_DOWN.equals(indexStatus.getStatus())) {
            return indexStatus;
        }
        final HealthStatus vaultStatus = checkVault();
        if (STATUS_DOWN.equals(vaultStatus.getStatus())) {
            return vaultStatus;
        }
        return new HealthStatus(STATUS_UP, null);
    }

    /**
     * Performs the readiness check: verifies that the component index is loaded.
     *
     * @return {@link HealthStatus} with {@code status="UP"} if ready,
     * or {@code status="DOWN"} with cause otherwise.
     */
    public HealthStatus checkReadiness() {
        if (!componentManagerService.isStarted()) {
            return new HealthStatus(STATUS_DOWN, "Component index not ready");
        }
        return new HealthStatus(STATUS_UP, null);
    }

    private HealthStatus checkMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long maxMemory = runtime.maxMemory();
        if (maxMemory == Long.MAX_VALUE) {
            return new HealthStatus(STATUS_UP, null);
        }
        final long availableMemory = maxMemory - runtime.totalMemory() + runtime.freeMemory();
        final int availablePercent = (int) (availableMemory * 100L / maxMemory);
        final int threshold = configuration.getHealthMemoryThreshold();
        if (availablePercent < threshold) {
            final String cause = String
                    .format("Available heap is %d%% which is below the configured threshold of %d%%",
                            availablePercent, threshold);
            log.warn("Liveness check failed: {}", cause);
            return new HealthStatus(STATUS_DOWN, cause);
        }
        return new HealthStatus(STATUS_UP, null);
    }

    private HealthStatus checkComponentIndex() {
        try {
            componentManagerService.manager().getContainer().findAll();
            return new HealthStatus(STATUS_UP, null);
        } catch (final Throwable t) {
            final String cause = "Component index check failed: " + t.getMessage();
            log.warn("Liveness check failed: {}", cause);
            return new HealthStatus(STATUS_DOWN, cause);
        }
    }

    private HealthStatus checkVault() {
        if (!vaultClient.ping()) {
            final String cause = "Vault is not reachable";
            log.warn("Liveness check failed: {}", cause);
            return new HealthStatus(STATUS_DOWN, cause);
        }
        return new HealthStatus(STATUS_UP, null);
    }
}
