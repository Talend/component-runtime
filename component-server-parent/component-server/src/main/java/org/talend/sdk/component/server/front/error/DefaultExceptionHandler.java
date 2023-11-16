/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front.error;

import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.talend.sdk.component.server.configuration.ComponentServerConfiguration;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Dependent
@Provider
public class DefaultExceptionHandler implements ExceptionMapper<Throwable> {

    @Inject
    private ComponentServerConfiguration configuration;

    private boolean replaceException;

    @PostConstruct
    private void init() {
        replaceException = !"false".equalsIgnoreCase(configuration.getDefaultExceptionMessage());
    }

    @Override
    public Response toResponse(final Throwable exception) {
        final Response response;
        if (WebApplicationException.class.isInstance(exception)) {
            response = WebApplicationException.class.cast(exception).getResponse();
        } else {
            final Optional<Throwable> optCause = Optional.ofNullable(exception.getCause());
            if (optCause.isPresent()) {
                final Throwable cause = optCause.get();
                if (WebApplicationException.class.isInstance(cause)) {
                    response = WebApplicationException.class.cast(cause).getResponse();
                } else {
                    response = Response
                            .status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED,
                                    replaceException ? configuration.getDefaultExceptionMessage() : cause.getMessage()))
                            .type(WILDCARD_TYPE)
                            .build();
                }
            } else {
                response = Response
                        .status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED,
                                replaceException ? configuration.getDefaultExceptionMessage() : exception.getMessage()))
                        .type(WILDCARD_TYPE)
                        .build();
            }
        }
        return response;
    }

}
