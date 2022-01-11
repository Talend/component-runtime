/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front.security;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.talend.sdk.component.server.front.model.ErrorDictionary.UNAUTHORIZED;

import java.io.IOException;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.security.event.OnConnection;

@Provider
@Dependent
@PreMatching
public class ConnectionSecurityProvider implements ContainerRequestFilter {

    public static final String SKIP = ConnectionSecurityProvider.class.getName() + ".skip";

    @Context
    private HttpServletRequest request;

    @Inject
    private Event<OnConnection> onConnectionEvent;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if (Boolean.TRUE.equals(request.getAttribute(SKIP))) {
            return;
        }

        final OnConnection onConnection = new OnConnection();
        onConnectionEvent.fire(onConnection);
        if (!onConnection.isValid()) {
            requestContext
                    .abortWith(Response
                            .status(Response.Status.UNAUTHORIZED)
                            .entity(new ErrorPayload(UNAUTHORIZED, "Invalid connection credentials"))
                            .type(APPLICATION_JSON_TYPE)
                            .build());
        }
    }
}
