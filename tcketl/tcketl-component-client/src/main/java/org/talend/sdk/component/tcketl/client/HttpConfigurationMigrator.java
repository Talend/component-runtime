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
package org.talend.sdk.component.tcketl.client;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.connector.ConnectorId;
import org.talend.sdk.component.tcketl.domain.server.ConfigurationMigrator;
import org.talend.sdk.component.tcketl.domain.server.ServerProtocolException;

/** {@code POST /component/migrate/{id}/{version}}: the flat configuration in, the migrated one out. */
final class HttpConfigurationMigrator implements ConfigurationMigrator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpTransport transport;

    HttpConfigurationMigrator(final HttpTransport transport) {
        this.transport = Preconditions.requireNonNull(transport, "transport");
    }

    @Override
    public Map<String, String> migrate(final ConnectorId id, final int fromVersion,
            final Map<String, String> configuration) {
        final HttpTransport.Response response;
        try {
            response = transport
                    .post(new HttpTransport.Query(), MAPPER.writeValueAsString(new TreeMap<>(configuration)),
                            "component", "migrate", id.value(), Integer.toString(fromVersion));
        } catch (final IOException e) {
            throw new ServerProtocolException(0, "Cannot write the configuration: " + e.getMessage(), e);
        }
        if (!response.isSuccess()) {
            throw response.toException();
        }
        try {
            final JsonNode answer = MAPPER.readTree(response.body());
            final Map<String, String> migrated = new TreeMap<>();
            answer.fields().forEachRemaining(field -> {
                if (!field.getValue().isNull()) {
                    migrated.put(field.getKey(), field.getValue().asText());
                }
            });
            return migrated;
        } catch (final IOException e) {
            throw new ServerProtocolException(response.status(), "Malformed migration answer: " + e.getMessage(), e);
        }
    }
}
