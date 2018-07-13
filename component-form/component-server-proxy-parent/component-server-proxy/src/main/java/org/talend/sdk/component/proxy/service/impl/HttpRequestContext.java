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
package org.talend.sdk.component.proxy.service.impl;

import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.talend.sdk.component.proxy.api.service.RequestContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HttpRequestContext implements RequestContext {

    private final String language;

    private final Function<String, String> placeholderProvider;

    private final HttpServletRequest request;

    @Override
    public String language() {
        return language;
    }

    @Override
    public String findPlaceholder(final String attributeName) {
        return placeholderProvider.apply(attributeName);
    }

    @Override
    public Object attribute(final String key) {
        return request.getAttribute(key);
    }
}
