/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.test.custom;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.talend.sdk.component.api.exception.ComponentException;
import org.talend.sdk.component.api.service.Action;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.SuggestionValues;
import org.talend.sdk.component.api.service.completion.Suggestions;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;

@Service
public class CustomService implements Serializable {

    @Suggestions("forTests")
    public SuggestionValues get(final LocalConfiguration configuration) throws IOException {
        final ClassLoader componentLoader = Thread.currentThread().getContextClassLoader();
        final Properties propertiesCustomJar = new Properties();
        try (final InputStream stream = componentLoader
                .getResourceAsStream(
                        "TALEND-INF/org.talend.sdk.component.server.test.custom.CustomService.properties")) {
            propertiesCustomJar.load(stream);
        }
        return new SuggestionValues(true,
                Stream
                        .concat(propertiesCustomJar
                                .stringPropertyNames()
                                .stream()
                                .map(key -> new SuggestionValues.Item(key, propertiesCustomJar.getProperty(key))),
                                Stream
                                        .of("i.m.a.virtual.configuration.entry",
                                                "i.m.another.virtual.configuration.entry")
                                        .map(key -> new SuggestionValues.Item(key, configuration.get(key))))
                        .collect(toList()));
    }

    @Action("unknownException")
    public Map<String, String> generateUnknownException(final LocalConfiguration configuration) {
        throw new ComponentException(ComponentException.ErrorOrigin.UNKNOWN, "unknown exception");
    }

    @Action("userException")
    public Map<String, String> generateUserException(final LocalConfiguration configuration) {
        throw new ComponentException(ComponentException.ErrorOrigin.USER, "user exception");
    }

    @Action("backendException")
    public Map<String, String> generateBackendException(final LocalConfiguration configuration) {
        throw new ComponentException(ComponentException.ErrorOrigin.BACKEND, "backend exception");
    }
}
