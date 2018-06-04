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
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.proxy.model.ProxyErrorPayload;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ErrorProcessor {

    public interface Constants {

        String HEADER_TALEND_COMPONENT_SERVER_ERROR = "Talend-Component-Server-Error";
    }

    private final GenericType<Map<String, ErrorPayload>> multipleErrorType =
            new GenericType<Map<String, ErrorPayload>>() {

            };

    public <T> CompletionStage<T> handleResponse(final CompletionStage<T> stage) {
        return stage.exceptionally(t -> {
            throw handleSingleError(unwrap(t));
        });
    }

    public <T> CompletionStage<T> handleResponses(final CompletionStage<T> stage) {
        return stage.exceptionally(t -> {
            throw multipleToSingleError(unwrap(t));
        });
    }

    private Throwable unwrap(final Throwable throwable) {
        return CompletionException.class.isInstance(throwable)
                ? unwrap(CompletionException.class.cast(throwable).getCause())
                : throwable;
    }

    /**
     * unwrap multiple errors to single one when we request only one resource using batch endpoint
     *
     * @param throwable WebApplicationException to unwrap
     * @return WebApplicationException with a single error
     */
    private WebApplicationException multipleToSingleError(final Throwable throwable) {
        final WebApplicationException error = WebApplicationException.class.cast(unwrap(throwable));
        ErrorPayload errorDetails =
                error.getResponse().readEntity(multipleErrorType).entrySet().iterator().next().getValue();
        return new WebApplicationException(Response
                .status(error.getResponse().getStatus())
                .entity(new ProxyErrorPayload(errorDetails.getCode().name(), errorDetails.getDescription()))
                .type(APPLICATION_JSON_TYPE)
                .header(Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, true)
                .build());
    }

    private WebApplicationException handleSingleError(final Throwable throwable) {
        if (WebApplicationException.class.isInstance(throwable)) {
            final WebApplicationException error = WebApplicationException.class.cast(throwable);
            try {
                final ErrorPayload serverError = error.getResponse() != null && error.getResponse().bufferEntity()
                        ? error.getResponse().readEntity(ErrorPayload.class)
                        : new ErrorPayload(ErrorDictionary.UNEXPECTED, "No error entity available");
                return new WebApplicationException(Response
                        .status(error.getResponse().getStatus())
                        .entity(new ProxyErrorPayload(serverError.getCode().name(), serverError.getDescription()))
                        .type(APPLICATION_JSON_TYPE)
                        .header(Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, true)
                        .build());
            } catch (final RuntimeException | Error pe) {
                log.error(pe.getMessage(), pe);
                return new WebApplicationException(Response
                        .status(HTTP_INTERNAL_ERROR)
                        .entity(new ProxyErrorPayload(ErrorDictionary.UNEXPECTED.name(),
                                "Error while processing server error : '" + error.getResponse().getStatus() + "'"))
                        .type(APPLICATION_JSON_TYPE)
                        .header(Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, false)
                        .build());
            }
        }

        return new WebApplicationException(Response
                .status(HTTP_INTERNAL_ERROR)
                .entity(new ProxyErrorPayload(ErrorDictionary.UNEXPECTED.name(),
                        "Component server failed with : '" + throwable.getLocalizedMessage() + "'"))
                .type(APPLICATION_JSON_TYPE)
                .header(Constants.HEADER_TALEND_COMPONENT_SERVER_ERROR, true)
                .build());
    }
}
