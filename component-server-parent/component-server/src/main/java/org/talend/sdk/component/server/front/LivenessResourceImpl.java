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

import org.talend.sdk.component.server.api.LivenessResource;
import org.talend.sdk.component.server.front.model.HealthStatus;
import org.talend.sdk.component.server.service.FatalState;

@ApplicationScoped
public class LivenessResourceImpl implements LivenessResource {

    private static final String STATUS_UP = "UP";

    private static final String STATUS_DOWN = "DOWN";

    @Inject
    private FatalState fatalState;

    @Override
    public Response getLiveness() {
        if (fatalState.hasFatalError()) {
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new HealthStatus(STATUS_DOWN, fatalState.getCause()))
                    .build();
        }
        return Response.ok(new HealthStatus(STATUS_UP, null)).build();
    }
}
