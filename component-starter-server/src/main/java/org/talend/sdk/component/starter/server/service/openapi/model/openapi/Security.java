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
package org.talend.sdk.component.starter.server.service.openapi.model.openapi;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;

/**
 * Security Requirement Object
 *
 * Lists the required security schemes to execute this operation. The name used for each property MUST correspond to a
 * security scheme declared in the Security Schemes under the Components Object.
 *
 * Security Requirement Objects that contain multiple schemes require that all schemes MUST be satisfied for a request
 * to be authorized. This enables support for scenarios where multiple query parameters or HTTP headers are required to
 * convey security information.
 *
 * When a list of Security Requirement Objects is defined on the OpenAPI Object or Operation Object, only one of the
 * Security Requirement Objects in the list needs to be satisfied to authorize the request.
 */
@Data
public class Security implements Map<String, List<String>> {

    /**
     * {name} [string] Each name MUST correspond to a security scheme which is declared in the Security Schemes under
     * the Components Object. If the security scheme is of type "oauth2" or "openIdConnect", then the value is a list of
     * scope names required for the execution, and the list MAY be empty if authorization does not require a specified
     * scope. For other security scheme types, the array MUST be empty.
     */

    private Map<String, List<String>> security;

    @Override
    public int size() {
        throw new UnsupportedOperationException("#size()");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("#isEmpty()");
    }

    @Override
    public boolean containsKey(final Object o) {
        throw new UnsupportedOperationException("#containsKey()");
    }

    @Override
    public boolean containsValue(final Object o) {
        throw new UnsupportedOperationException("#containsValue()");
    }

    @Override
    public List<String> get(final Object o) {
        throw new UnsupportedOperationException("#get()");
    }

    @Override
    public List<String> put(final String s, final List<String> strings) {
        throw new UnsupportedOperationException("#put()");
    }

    @Override
    public List<String> remove(final Object o) {
        throw new UnsupportedOperationException("#remove()");
    }

    @Override
    public void putAll(final Map<? extends String, ? extends List<String>> map) {
        throw new UnsupportedOperationException("#putAll()");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("#clear()");
    }

    @Override
    public Set<String> keySet() {
        throw new UnsupportedOperationException("#keySet()");
    }

    @Override
    public Collection<List<String>> values() {
        throw new UnsupportedOperationException("#values()");
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        throw new UnsupportedOperationException("#entrySet()");
    }
}
