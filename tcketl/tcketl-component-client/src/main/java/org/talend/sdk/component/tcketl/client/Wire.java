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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.talend.sdk.component.tcketl.domain.server.ServerProtocolException;

/**
 * The shape of the component server payloads as the client reads them, and the decoding helpers. Only the fields the
 * designer uses are declared; the others are ignored, so that a newer server does not break an older designer.
 */
final class Wire {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private Wire() {
        // static helpers only
    }

    record Environment(String commit, String lastUpdated, String version, Connectors connectors) {
    }

    record Connectors(String pluginsHash, String version) {
    }

    record ComponentId(String family, String familyId, String id, String name, String plugin) {
    }

    record ComponentIndexes(List<ComponentIndex> components) {
    }

    record ComponentIndex(
            List<String> categories,
            String displayName,
            String familyDisplayName,
            ComponentId id,
            String type,
            int version,
            Map<String, String> metadata
    ) {
    }

    record ComponentDetails(List<ComponentDetail> details) {
    }

    record ComponentDetail(
            List<ActionReference> actions,
            String displayName,
            ComponentId id,
            List<String> inputFlows,
            List<String> outputFlows,
            List<Property> properties,
            int version,
            Map<String, String> metadata
    ) {
    }

    record ActionReference(String family, String name, String type, String displayName, List<Property> properties) {
    }

    record Property(
            String path,
            String name,
            String displayName,
            String type,
            String defaultValue,
            Validation validation,
            Map<String, String> metadata,
            String placeholder,
            Map<String, String> proposalDisplayNames
    ) {
    }

    record Validation(
            Boolean required,
            Integer min,
            Integer max,
            Integer minLength,
            Integer maxLength,
            Integer minItems,
            Integer maxItems,
            Boolean uniqueItems,
            String pattern,
            List<String> enumValues
    ) {
    }

    record ErrorPayload(String code, String description) {
    }

    record Documentation(String type, String source) {
    }

    /** Decodes a successful answer, a payload that cannot be read being reported as a protocol error. */
    static <T> T read(final HttpTransport.Response response, final Class<T> type) {
        try {
            final T value = MAPPER.readValue(response.body(), type);
            if (value == null) {
                throw new ServerProtocolException(response.status(), "Empty " + type.getSimpleName() + " payload",
                        null);
            }
            return value;
        } catch (final IOException e) {
            throw new ServerProtocolException(response.status(),
                    "Unexpected " + type.getSimpleName() + " payload: " + describe(e), e);
        }
    }

    /** The error payload of a failed call, empty when the body is not one (a proxy page, nothing...). */
    static Optional<ErrorPayload> error(final byte[] body) {
        try {
            final JsonNode node = MAPPER.readTree(body);
            if (node == null || !node.isObject() || !node.hasNonNull("code") && !node.hasNonNull("description")) {
                return Optional.empty();
            }
            return Optional.of(new ErrorPayload(node.path("code").asText(""), node.path("description").asText("")));
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    /**
     * The identifiers a rejected {@code /component/details} call complains about: its body maps each of them to an
     * error payload. A body that is a single error payload names none.
     */
    static Set<String> rejectedIds(final byte[] body) {
        final Set<String> ids = new LinkedHashSet<>();
        try {
            final JsonNode node = MAPPER.readTree(body);
            if (node != null && node.isObject()) {
                final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
                while (fields.hasNext()) {
                    final Map.Entry<String, JsonNode> field = fields.next();
                    if (field.getValue().isObject()) {
                        ids.add(field.getKey());
                    }
                }
            }
        } catch (final IOException e) {
            return Set.of();
        }
        return ids;
    }

    private static String describe(final IOException error) {
        final String message =
                error instanceof JsonProcessingException json ? json.getOriginalMessage() : error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
