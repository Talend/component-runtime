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
package org.talend.sdk.component.api.service.http;

/**
 * HttpException that wrap an http {@link Response} with a status code and an error payload
 */
public class HttpException extends RuntimeException {

    private Response response;

    public HttpException(final Response response) {
        super("code: " + response.status() + ", message: " + response.error(String.class));
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}
