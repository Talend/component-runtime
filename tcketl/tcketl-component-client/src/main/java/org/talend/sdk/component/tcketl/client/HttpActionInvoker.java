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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome;
import org.talend.sdk.component.tcketl.domain.action.ActionOutcome.Choice;
import org.talend.sdk.component.tcketl.domain.action.ActionRequest;
import org.talend.sdk.component.tcketl.domain.property.ActionTypes;
import org.talend.sdk.component.tcketl.domain.server.ActionInvoker;
import org.talend.sdk.component.tcketl.domain.server.ServerProtocolException;

/**
 * {@code POST /action/execute}: the body is the flat parameter map, the answer is decoded by the type of the request.
 * The language parameter of this endpoint is {@code lang}, not {@code language}.
 */
final class HttpActionInvoker implements ActionInvoker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String OK = "OK";

    private final HttpTransport transport;

    private final String language;

    HttpActionInvoker(final HttpTransport transport, final String language) {
        this.transport = Preconditions.requireNonNull(transport, "transport");
        this.language = Preconditions.requireNonNull(language, "language");
    }

    @Override
    public ActionOutcome execute(final ActionRequest request) {
        final HttpTransport.Query query = new HttpTransport.Query()
                .add("family", request.family())
                .add("type", request.type())
                .add("action", request.name())
                .add("lang", language);
        final HttpTransport.Response response;
        try {
            response = transport.post(query, MAPPER.writeValueAsString(request.body()), "action", "execute");
        } catch (final IOException e) {
            throw new ServerProtocolException(0, "Cannot write the request: " + e.getMessage(), e);
        }
        if (!response.isSuccess()) {
            throw response.toException();
        }
        try {
            return decode(request.type(), MAPPER.readTree(response.body()));
        } catch (final IOException e) {
            throw new ServerProtocolException(response.status(), "Malformed action answer: " + e.getMessage(), e);
        }
    }

    static ActionOutcome decode(final String type, final JsonNode answer) {
        return switch (type) {
            case ActionTypes.HEALTHCHECK, ActionTypes.VALIDATION -> new ActionOutcome.Checked(
                    OK.equalsIgnoreCase(answer.path("status").asText()),
                    answer.path("comment").asText(""));
            case ActionTypes.SUGGESTIONS, ActionTypes.DYNAMIC_VALUES -> choices(answer);
            case ActionTypes.UPDATE -> {
                final Map<String, String> flat = new TreeMap<>();
                flatten("", answer, flat);
                yield new ActionOutcome.Replacement(flat);
            }
            case ActionTypes.SCHEMA -> columns(answer);
            default -> new ActionOutcome.Failed("The type " + type + " is not supported by the designer");
        };
    }

    private static ActionOutcome choices(final JsonNode answer) {
        final List<Choice> items = new ArrayList<>();
        for (final JsonNode item : answer.path("items")) {
            final String id = item.path("id").asText("");
            if (!id.isBlank()) {
                items.add(new Choice(id, item.path("label").asText(id)));
            }
        }
        return new ActionOutcome.Choices(answer.path("cacheable").asBoolean(false), items);
    }

    private static ActionOutcome columns(final JsonNode answer) {
        final List<String> names = new ArrayList<>();
        for (final JsonNode entry : answer.path("entries")) {
            final String name = entry.path("name").asText("");
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return new ActionOutcome.Columns(names);
    }

    /** Objects nest with {@code .name}, arrays with {@code [i]}, nulls are dropped and scalars are their text. */
    static void flatten(final String path, final JsonNode node, final Map<String, String> target) {
        if (node.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();
                flatten(path + '.' + field.getKey(), field.getValue(), target);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flatten(path + '[' + i + ']', node.get(i), target);
            }
        } else if (!node.isNull() && !node.isMissingNode()) {
            target.put(path, node.asText());
        }
    }
}
