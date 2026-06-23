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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.server.api.ReadinessResource;
import org.talend.sdk.component.server.front.model.HealthStatus;
import org.talend.sdk.component.server.service.HealthService;

@ApplicationScoped
public class ReadinessResourceImpl implements ReadinessResource {

    @Inject
    private HealthService healthService;

    @Override
    public Response getReadiness() {
        final HealthStatus status = healthService.checkReadiness();
        if ("UP".equals(status.getStatus())) {
            return Response.ok(status).build();
        }
        return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(status).build();
    }
}
