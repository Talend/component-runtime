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

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;
import org.talend.sdk.component.tcketl.persistence.FileFormatException;
import org.talend.sdk.component.tcketl.persistence.FormatVersion;
import org.talend.sdk.component.tcketl.persistence.JsonFiles;

/**
 * On-disk shape of the settings, format 1.x. The JSON names are the file contract. The file is meant to be edited by
 * hand, so every part is optional and falls back to the default; a value that is present must be valid.
 */
record ConfigFile(
        @JsonProperty(JsonFiles.VERSION_FIELD) String formatVersion,
        @JsonProperty("server") ServerFile server,
        @JsonProperty("language") String language,
        @JsonProperty("timeouts") TimeoutsFile timeouts
) {

    static final FormatVersion CURRENT = new FormatVersion(1, 0);

    static ConfigFile from(final ApplicationConfig config) {
        return new ConfigFile(CURRENT.toString(), ServerFile.from(config.server()), config.language(),
                TimeoutsFile.from(config.timeouts()));
    }

    ApplicationConfig toConfig() throws FileFormatException {
        final ApplicationConfig defaults = ApplicationConfig.defaults();
        try {
            return new ApplicationConfig(server == null ? defaults.server() : server.toEndpoint(defaults.server()),
                    language, timeouts == null ? defaults.timeouts() : timeouts.toTimeouts(defaults.timeouts()));
        } catch (final IllegalArgumentException | NullPointerException e) {
            throw new FileFormatException("Invalid settings: " + e.getMessage(), e);
        }
    }

    record ServerFile(
            @JsonProperty("scheme") String scheme,
            @JsonProperty("host") String host,
            @JsonProperty("port") Integer port,
            @JsonProperty("base-path") String basePath
    ) {

        static ServerFile from(final ServerEndpoint endpoint) {
            return new ServerFile(endpoint.scheme(), endpoint.host(), endpoint.port(), endpoint.basePath());
        }

        ServerEndpoint toEndpoint(final ServerEndpoint defaults) {
            return new ServerEndpoint(scheme == null ? defaults.scheme() : scheme,
                    host == null ? defaults.host() : host, port == null ? defaults.port() : port,
                    basePath == null ? defaults.basePath() : basePath);
        }
    }

    record TimeoutsFile(
            @JsonProperty("connect-ms") Long connectMs,
            @JsonProperty("request-ms") Long requestMs,
            @JsonProperty("action-ms") Long actionMs
    ) {

        static TimeoutsFile from(final ServerTimeouts timeouts) {
            return new TimeoutsFile(timeouts.connect().toMillis(), timeouts.request().toMillis(),
                    timeouts.action().toMillis());
        }

        ServerTimeouts toTimeouts(final ServerTimeouts defaults) {
            return new ServerTimeouts(millisOr(connectMs, defaults.connect()), millisOr(requestMs, defaults.request()),
                    millisOr(actionMs, defaults.action()));
        }

        private static Duration millisOr(final Long millis, final Duration fallback) {
            return millis == null ? fallback : Duration.ofMillis(millis);
        }
    }
}
