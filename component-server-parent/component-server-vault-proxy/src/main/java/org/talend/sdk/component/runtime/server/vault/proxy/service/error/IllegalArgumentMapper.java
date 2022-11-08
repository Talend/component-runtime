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
package org.talend.sdk.component.runtime.server.vault.proxy.service.error;

import static javax.ws.rs.RuntimeType.SERVER;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
@ConstrainedTo(SERVER)
public class IllegalArgumentMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(final IllegalArgumentException exception) {
        log.error(exception.getMessage(), exception);
        return Response
                .serverError()
                .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, exception.getMessage()))
                .build();
    }
}
