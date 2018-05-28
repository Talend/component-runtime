/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.proxy.service;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

@ApplicationScoped
public class ErrorProcessor {

    public interface Constants {

        String HEADER_TALEND_COMPONENT_SERVER_ERROR = "Talend-Component-Server-Error";
    }

    public Object handleResponse(final AsyncResponse response, final Object result, final Throwable throwable) {
        if (throwable != null) {
            response.resume(handleSingleError(throwable));
        } else {
            response.resume(result);
        }
        return null;
    }

    private WebApplicationException handleSingleError(final Throwable throwable) {
        if (WebApplicationException.class.isInstance(throwable)) {
            final WebApplicationException error = WebApplicationException.class.cast(throwable);
            try {
                final ErrorPayload serverError = error.getResponse().readEntity(ErrorPayload.class);
                return new WebApplicationException(Response
                        .status(error.getResponse().getStatus())
                        .entity(new ProxyErrorPayload(serverError.getCode().name(), serverError.getDescription()))
                        .header(Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, true)
                        .build());
            } catch (final ProcessingException pe) {
                return new WebApplicationException(Response
                        .status(HTTP_INTERNAL_ERROR)
                        .entity(new ProxyErrorPayload("UNEXPECTED",
                                "Component server failed with status '" + error.getResponse().getStatus() + "'"))
                        .header(Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, true)
                        .build());
            }
        }

        return new WebApplicationException(Response
                .status(HTTP_INTERNAL_ERROR)
                .entity(new ProxyErrorPayload("UNEXPECTED", throwable.getLocalizedMessage()))
                .header(Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, true)
                .build());
    }
}
