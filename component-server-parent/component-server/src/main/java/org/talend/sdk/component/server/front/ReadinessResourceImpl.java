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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.server.api.ReadinessResource;
import org.talend.sdk.component.server.front.model.HealthStatus;
import org.talend.sdk.component.server.service.ComponentManagerService;
import org.talend.sdk.components.vault.client.VaultClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ReadinessResourceImpl implements ReadinessResource {

    private static final String STATUS_UP = "UP";

    private static final String STATUS_DOWN = "DOWN";

    private static final long VAULT_CACHE_TTL_MS = 5_000L;

    @Inject
    private ComponentManagerService componentManagerService;

    @Inject
    private VaultClient vaultClient;

    private final AtomicBoolean cachedVaultResult = new AtomicBoolean(true);

    private final AtomicLong lastVaultCheck = new AtomicLong(0L);

    @Override
    public Response getReadiness() {
        if (!componentManagerService.isStarted()) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new HealthStatus(STATUS_DOWN, "Component index not ready"))
                    .build();
        }
        final HealthStatus vaultStatus = checkVaultCached();
        if (STATUS_DOWN.equals(vaultStatus.getStatus())) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(vaultStatus).build();
        }
        return Response.ok(new HealthStatus(STATUS_UP, null)).build();
    }

    private HealthStatus checkVaultCached() {
        final long now = System.currentTimeMillis();
        if (now - lastVaultCheck.get() < VAULT_CACHE_TTL_MS) {
            return cachedVaultResult.get()
                    ? new HealthStatus(STATUS_UP, null)
                    : new HealthStatus(STATUS_DOWN, "Vault is not reachable");
        }
        lastVaultCheck.set(now);
        try {
            final boolean reachable = vaultClient.ping();
            cachedVaultResult.set(reachable);
            if (!reachable) {
                log.warn("Readiness check: Vault is not reachable");
                return new HealthStatus(STATUS_DOWN, "Vault is not reachable");
            }
        } catch (final Throwable t) {
            cachedVaultResult.set(false);
            final String cause = "Vault connectivity check failed: " + t.getMessage();
            log.warn("Readiness check failed: {}", cause);
            return new HealthStatus(STATUS_DOWN, cause);
        }
        return new HealthStatus(STATUS_UP, null);
    }
}
