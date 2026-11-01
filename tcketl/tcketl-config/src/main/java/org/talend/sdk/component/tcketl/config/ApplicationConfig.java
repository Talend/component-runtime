/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.tcketl.config;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;

/**
 * Application settings: where the component server is, which language its answers are requested in, and how long to
 * wait for it. The language is a locale code such as {@code en} or {@code zh_CN}; the server maps the codes it does
 * not know to English.
 */
public record ApplicationConfig(ServerEndpoint server, String language, ServerTimeouts timeouts) {

    public static final String DEFAULT_LANGUAGE = "en";

    public ApplicationConfig {
        Preconditions.requireNonNull(server, "server");
        language = Preconditions.orDefault(language == null ? null : language.strip(), DEFAULT_LANGUAGE);
        Preconditions.requireNonNull(timeouts, "timeouts");
    }

    public static ApplicationConfig defaults() {
        return new ApplicationConfig(ServerEndpoint.defaults(), DEFAULT_LANGUAGE, ServerTimeouts.defaults());
    }

    public ApplicationConfig withServer(final ServerEndpoint newServer) {
        return new ApplicationConfig(newServer, language, timeouts);
    }

    public ApplicationConfig withLanguage(final String newLanguage) {
        return new ApplicationConfig(server, newLanguage, timeouts);
    }

    public ApplicationConfig withTimeouts(final ServerTimeouts newTimeouts) {
        return new ApplicationConfig(server, language, newTimeouts);
    }
}
