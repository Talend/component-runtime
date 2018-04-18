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
package org.talend.sdk.component.runtime.manager.service.http;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.talend.sdk.component.api.service.http.Configurer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class HttpRequestCreator implements BiFunction<String, Object[], HttpRequest> {

    private final Function<Object[], String> httpMethodProvider;

    private final Function<Object[], String> urlProvide;

    private final String pathTemplate;

    private final BiFunction<String, Object[], String> pathProvider;

    private final Function<Object[], Map<String, String>> queryParamsProvider;

    private final Function<Object[], Map<String, String>> headersProvider;

    private final BiFunction<String, Object[], Optional<byte[]>> payloadProvider;

    private final Configurer configurer;

    private final Map<String, Function<Object[], Object>> configurerOptions;

    @Override
    public HttpRequest apply(final String base, final Object[] params) {
        return new HttpRequest(buildUrl(base, params), httpMethodProvider.apply(params),
                queryParamsProvider.apply(params), headersProvider.apply(params), configurer, configurerOptions,
                payloadProvider, params);
    }

    private String buildUrl(final String base, final Object[] params) {
        if (urlProvide == null) {
            String path = pathProvider.apply(pathTemplate, params);
            return base + (path.isEmpty() ? "" : "/" + path);
        }
        return pathProvider.apply(urlProvide.apply(params), params);
    }

}
