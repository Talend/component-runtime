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
package org.talend.sdk.component.runtime.server.vault.proxy.service.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;

import lombok.AllArgsConstructor;

// mainly here to handle nulls for queries
@AllArgsConstructor
public class RequestBuilder {

    private WebTarget current;

    public RequestBuilder path(final String path) {
        current = current.path(path);
        return this;
    }

    public RequestBuilder queryParam(final String key, final Object values) {
        if (values != null) {
            current = current.queryParam(key, values);
        }
        return this;
    }

    public Invocation.Builder request() {
        return current.request(APPLICATION_JSON_TYPE);
    }
}
