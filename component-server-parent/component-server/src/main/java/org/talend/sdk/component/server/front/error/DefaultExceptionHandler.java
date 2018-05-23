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
package org.talend.sdk.component.server.front.error;

import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;

import java.util.logging.Level;

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

import lombok.extern.java.Log;

@Log
@Dependent
@Provider
public class DefaultExceptionHandler implements ExceptionMapper<Throwable> {

    @Inject
    private ComponentServerConfiguration configuration;

    private boolean replaceException;

    @PostConstruct
    private void init() {
        replaceException = !"false".equalsIgnoreCase(configuration.defaultExceptionMessage());
    }

    @Override
    public Response toResponse(final Throwable exception) {
        log.log(Level.SEVERE, exception.getMessage(), exception);
        if (WebApplicationException.class.isInstance(exception)) {
            return WebApplicationException.class.cast(exception).getResponse();
        }
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED,
                        replaceException ? configuration.defaultExceptionMessage() : exception.getMessage()))
                .type(WILDCARD_TYPE)
                .build();
    }
}
