/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import java.util.concurrent.CompletionException;

import javax.enterprise.context.Dependent;

import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

@Provider
@Dependent
public class CompletionExceptionExceptionMapper implements ExceptionMapper<CompletionException> {

    @Context
    private Providers providers;

    @Override
    public Response toResponse(final CompletionException exception) {
        final Throwable cause = exception.getCause();
        if (cause != null) {
            final Class type = cause.getClass();
            return providers.getExceptionMapper(type).toResponse(cause);
        }
        return Response
                .serverError()
                .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, exception.getMessage()))
                .build();
    }
}
