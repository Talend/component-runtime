/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch;

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collector;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.server.front.model.ActionReference;
import org.talend.sdk.component.server.front.model.SimplePropertyDefinition;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;

@ApplicationScoped
public class ActionHandler {

    @Inject
    private StitchConfiguration configuration;

    @Ext
    @Inject
    private WebTarget base;

    @Ext
    @Inject
    private JsonBuilderFactory builderFactory;

    // todo: cache to avoid to do it all the time, both triggers are actually the same
    public CompletionStage<Response> onAction(final ActionReference reference, final Map<String, String> payload,
            final String lang) {
        return base
                .path("executor/discover")
                .queryParam("language", lang)
                .request(APPLICATION_JSON_TYPE)
                .rx()
                .post(entity(builderFactory
                        .createObjectBuilder()
                        .add("tap", reference.getName().substring(reference.getName().indexOf('_') + 1))
                        .add("configuration", toJson(reference, payload, "step_"))
                        .add("schema", toJson(reference, payload, "schema"))
                        .build(), APPLICATION_JSON_TYPE))
                .thenApply(response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        final String type = reference.getName().substring(0, reference.getName().indexOf('_'));
                        switch (type) {
                        case "schema": {
                            final JsonObject result = extractResult(response.readEntity(JsonObject.class));
                            final JsonArray streams = extractStreams(result);
                            return copy(response,
                                    builderFactory
                                            .createObjectBuilder()
                                            .add("items", streams)
                                            .add("cacheable", true)
                                            .build());
                        }
                        case "fields": {
                            final JsonObject result = extractResult(response.readEntity(JsonObject.class));
                            final JsonObject data = extractDiscoverData(result);
                            final Collection<String> selectedSchemas = payload
                                    .entrySet()
                                    .stream()
                                    .filter(entry -> entry.getKey().contains(".schema[")
                                            && entry.getKey().contains("]"))
                                    /*
                                     * not important for what we do here
                                     * .sorted(comparing(key -> {
                                     * final int from = key.getKey().indexOf(".schema[") + ".schema[".length();
                                     * final int to = key.getKey().indexOf("]", from);
                                     * return Integer.parseInt(key.getKey().substring(from, to));
                                     * }))
                                     */
                                    .map(Map.Entry::getValue)
                                    .map(String::valueOf)
                                    .collect(toList());
                            final JsonArray firstLevelProperties = data
                                    .getJsonArray("streams")
                                    .stream()
                                    .map(JsonValue::asJsonObject)
                                    .filter(stream -> selectedSchemas.contains(getStreamName(stream)))
                                    .flatMap(stream -> stream
                                            .getJsonObject("schema")
                                            .getJsonObject("properties")
                                            .keySet()
                                            .stream())
                                    .collect(Collector
                                            .of(builderFactory::createArrayBuilder,
                                                    (array, stream) -> array
                                                            .add(builderFactory
                                                                    .createObjectBuilder()
                                                                    .add("id", stream)
                                                                    .add("label", stream)),
                                                    JsonArrayBuilder::addAll, JsonArrayBuilder::build));
                            return copy(response,
                                    builderFactory
                                            .createObjectBuilder()
                                            .add("items", firstLevelProperties)
                                            .add("cacheable", true)
                                            .build());
                        }
                        default:
                        }
                    }
                    return response;
                });
    }

    private String getStreamName(final JsonObject stream) {
        final JsonValue value = ofNullable(stream.get("tap_stream_id")).orElseGet(() -> stream.get("stream"));
        return value.getValueType() == JsonValue.ValueType.STRING ? JsonString.class.cast(value).getString() : null;
    }

    private JsonArray extractStreams(final JsonObject result) {
        return extractDiscoverData(result)
                .getJsonArray("streams")
                .stream()
                .map(JsonValue::asJsonObject)
                .map(this::getStreamName)
                .filter(Objects::nonNull)
                .collect(Collector
                        .of(builderFactory::createArrayBuilder, (array, stream) -> array
                                .add(builderFactory.createObjectBuilder().add("id", stream).add("label", stream)),
                                JsonArrayBuilder::addAll, JsonArrayBuilder::build));
    }

    private JsonObject extractDiscoverData(final JsonObject result) {
        if (!result.containsKey("data") || result.get("data").getValueType() != JsonValue.ValueType.OBJECT) {
            throw new WebApplicationException(Response
                    .status(521)
                    .entity(new ErrorPayload(null, "Invalid tap data, missing result data"))
                    .build());
        }
        final JsonObject data = result.getJsonObject("data");
        if (!data.containsKey("streams") || data.get("streams").getValueType() != JsonValue.ValueType.ARRAY) {
            throw new WebApplicationException(
                    Response.status(521).entity(new ErrorPayload(null, "Invalid tap data, no streams")).build());
        }
        return data;
    }

    private JsonObject extractResult(final JsonObject result) {
        if (!result.containsKey("exitCode") || result.getInt("exitCode") != 0) {
            throw new WebApplicationException(Response
                    .status(521)
                    .entity(new ErrorPayload(null,
                            "Invalid tap status (" + result.get("exitCode") + mapError(result) + ")"))
                    .build());
        }
        final JsonValue data1 = result.get("data");
        if (data1 == null || data1.getValueType() != JsonValue.ValueType.OBJECT) {
            throw new WebApplicationException(Response
                    .status(521)
                    .entity(new ErrorPayload(null, "No tap data (" + result.get("exitCode") + mapError(result) + ")"))
                    .build());
        }
        final JsonObject dataObj = data1.asJsonObject();
        if (!dataObj.containsKey("success") || !dataObj.getBoolean("success")) {
            throw new WebApplicationException(Response
                    .status(521)
                    .entity(new ErrorPayload(null, "Tap error (" + result.get("error") + ")"))
                    .build());
        }
        return dataObj;
    }

    private String mapError(final JsonObject result) {
        final StringBuilder detail = new StringBuilder();
        int index = 1;
        boolean hasCritical = false;
        while (result.containsKey("log." + index)) {
            final JsonObject log = result.getJsonObject("log." + index);
            final String level = log.getString("level");
            if (!hasCritical) {
                hasCritical = "critical".equalsIgnoreCase(level);
            }
            detail.append(", [").append(level).append("] ").append(log.getString("message"));
            index++;
        }
        if (!hasCritical) {
            index = 1;
            while (result.containsKey("exception." + index)) {
                detail
                        .append(", ")
                        .append(result.getJsonObject("exception." + index).getString("exception").replace("\n", ". "));
                index++;
            }
        }
        if (detail.length() == 0) {
            return "";
        }
        return " " + detail;
    }

    private Response copy(final Response response, final JsonObject result) {
        final Response.ResponseBuilder builder = Response.status(response.getStatus());
        response
                .getStringHeaders()
                .entrySet()
                .stream()
                .filter(it -> !isNotForwardedHeader(it.getKey()))
                .forEach(e -> builder.header(e.getKey(), String.join(",", e.getValue())));
        return builder.entity(result).build();
    }

    private static boolean isNotForwardedHeader(final String name) {
        final String header = name.toLowerCase(ROOT);
        return header.startsWith("x-b3-") || header.startsWith("baggage-") || header.startsWith("content-type")
                || header.startsWith("transfer-encoding");
    }

    // simplified logic since the model is very simple here
    private JsonObject toJson(final ActionReference reference, final Map<String, String> payload, final String prefix) {
        return reference
                .getProperties()
                .stream()
                .filter(it -> !"OBJECT".equalsIgnoreCase(it.getType()) && !"ARRAY".equalsIgnoreCase(it.getType())
                        && it.getPath().startsWith(prefix))
                .map(SimplePropertyDefinition::getPath)
                .filter(payload::containsKey)
                .collect(Collector
                        .of(builderFactory::createObjectBuilder,
                                (b, i) -> b.add(i.substring(i.lastIndexOf('.') + 1), payload.get(i)),
                                JsonObjectBuilder::addAll, JsonObjectBuilder::build));
    }
}
