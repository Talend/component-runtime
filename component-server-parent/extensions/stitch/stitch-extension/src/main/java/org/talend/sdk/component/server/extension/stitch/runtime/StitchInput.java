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
package org.talend.sdk.component.server.extension.stitch.runtime;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collector;

import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.input.Input;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// todo: actual type mapping
@Slf4j
@RequiredArgsConstructor
public class StitchInput implements Input, Serializable {

    private final String plugin;

    private final String name;

    private final Map<String, String> configuration;

    private final JsonReaderFactory jsonReaderFactory;

    private final JsonBuilderFactory jsonBuilderFactory;

    private final RecordBuilderFactory recordBuilderFactory;

    private final HttpClientFactory clientFactory;

    private transient BufferedReader reader;

    private transient Schema schema;

    private transient String lastEvent;

    @Override
    public Object next() {
        ensureConnected();
        return readNext();
    }

    private void ensureConnected() {
        if (reader != null) {
            return;
        }
        final String base = configuration.get("configuration.dataset.configuration.datastore.stitchConnection.url");
        final String token = configuration.get("configuration.dataset.configuration.datastore.stitchConnection.token");
        if (base == null || token == null) {
            throw new IllegalArgumentException("Missing stitch connection configuration");
        }
        // substring normally not needed but a protection
        final String tap = "tap-" + name.substring(name.lastIndexOf('.') + 1);
        final StitchClient client = clientFactory.create(StitchClient.class, base);

        final String jobId = client
                .createJob(token, "application/json", tap, configuration
                        .entrySet()
                        .stream()
                        .filter(it -> it.getKey().contains("step_form.") || it.getKey().contains("step_oauth.")
                                || it.getKey().contains("step_profile."))
                        .collect(Collector
                                .of(jsonBuilderFactory::createObjectBuilder,
                                        (obj, entry) -> obj
                                                .add(entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1),
                                                        entry.getValue()),
                                        JsonObjectBuilder::addAll, JsonObjectBuilder::build)))
                .getString("id");
        reader = new BufferedReader(new InputStreamReader(client.readOutput(token, "text/event-stream", jobId)));
    }

    private Record readNext() {
        while (true) {
            try {
                final String next = reader.readLine();
                if (next == null) {
                    return null;
                }
                if (next.trim().isEmpty()) {
                    continue;
                }
                if (next.startsWith("event:")) {
                    lastEvent = next.substring("event:".length()).trim();
                    continue;
                }
                if (next.startsWith("data:")) {
                    if (lastEvent == null) {
                        throw new IllegalArgumentException("Invalid server stream, missing event type");
                    }
                    switch (lastEvent) {
                    case "schema":
                        try (final JsonReader reader =
                                jsonReaderFactory.createReader(new StringReader(next.substring("data:".length())))) {
                            schema = toSchema(reader.readObject());
                        }
                        break;
                    case "record":
                        if (schema == null) {
                            throw new IllegalArgumentException("Invalid server stream, missing schema event");
                        }
                        final Record record;
                        try (final JsonReader reader =
                                jsonReaderFactory.createReader(new StringReader(next.substring("data:".length())))) {
                            record = toRecord(reader.readObject());
                        }
                        return record;
                    case "exception":
                        throw new IllegalStateException(next);
                    default:
                    }
                }
                // else ignore
            } catch (final IOException e) { // disconnected
                log.warn(e.getMessage(), e);
                reader = null; // let's try to reconnect
                return null;
            }
        }
    }

    private Record toRecord(final JsonObject root) {
        if (!"record".equalsIgnoreCase(root.getString("type")) || !root.containsKey("record")) {
            throw new IllegalArgumentException("Not a record: " + root);
        }

        final JsonObject data = root.getJsonObject("record");
        return buildRecord(schema, data);
    }

    private Record buildRecord(final Schema elementSchema, final JsonObject object) {
        final Record.Builder record = recordBuilderFactory.newRecordBuilder(elementSchema);
        elementSchema.getEntries().forEach(entry -> {
            final JsonValue value = object.get(entry.getName());
            final boolean isNull = JsonValue.NULL.equals(value) || value == null;
            switch (entry.getType()) {
            case STRING:
                record.withString(entry, JsonString.class.cast(value).getString());
                break;
            case FLOAT:
                if (isNull) {
                    return;
                }
                record.withFloat(entry, (float) JsonNumber.class.cast(value).doubleValue());
                break;
            case DOUBLE:
                if (isNull) {
                    return;
                }
                record.withDouble(entry, JsonNumber.class.cast(value).doubleValue());
                break;
            case INT:
                if (isNull) {
                    return;
                }
                record.withInt(entry, JsonNumber.class.cast(value).intValue());
                break;
            case LONG:
                if (isNull) {
                    return;
                }
                record.withLong(entry, JsonNumber.class.cast(value).longValue());
                break;
            case BOOLEAN:
                record.withBoolean(entry, value.equals(JsonValue.TRUE));
                break;
            case DATETIME:
                if (isNull) {
                    return;
                }
                record.withDateTime(entry, Date.from(Instant.parse(JsonString.class.cast(value).getString())));
                break;
            case ARRAY:
                record.withArray(entry, isNull ? null : buildArray(entry.getElementSchema(), value.asJsonArray()));
                break;
            case RECORD:
                record.withRecord(entry, isNull ? null : buildRecord(entry.getElementSchema(), value.asJsonObject()));
                break;
            default:
                throw new IllegalArgumentException("Unsupported value: " + value);
            }
        });
        return record.build();
    }

    private Collection<?> buildArray(final Schema elementSchema, final JsonArray array) {
        switch (elementSchema.getType()) {
        case STRING:
            return array
                    .stream()
                    .map(it -> it.getValueType() == JsonValue.ValueType.STRING ? JsonString.class.cast(it).getString()
                            : null)
                    .collect(toList());
        case FLOAT:
            return array
                    .stream()
                    .map(it -> it.getValueType() == JsonValue.ValueType.NUMBER
                            ? (float) JsonNumber.class.cast(it).doubleValue()
                            : null)
                    .collect(toList());
        case DOUBLE:
            return array
                    .stream()
                    .map(it -> it.getValueType() == JsonValue.ValueType.NUMBER ? JsonNumber.class.cast(it).doubleValue()
                            : null)
                    .collect(toList());
        case INT:
            return array
                    .stream()
                    .map(it -> it.getValueType() == JsonValue.ValueType.NUMBER ? JsonNumber.class.cast(it).intValue()
                            : null)
                    .collect(toList());
        case LONG:
            return array
                    .stream()
                    .map(it -> it.getValueType() == JsonValue.ValueType.NUMBER
                            ? (float) JsonNumber.class.cast(it).longValue()
                            : null)
                    .collect(toList());
        case BOOLEAN:
            return array.stream().map(JsonValue.TRUE::equals).collect(toList());
        case DATETIME:
            return array
                    .stream()
                    .map(it -> it.getValueType() == JsonValue.ValueType.STRING
                            ? Date.from(Instant.parse(JsonString.class.cast(it).getString()))
                            : null)
                    .collect(toList());
        case ARRAY:
            return array
                    .stream()
                    .map(it -> it.getValueType() == JsonValue.ValueType.ARRAY
                            ? buildArray(elementSchema.getElementSchema(), it.asJsonArray())
                            : null)
                    .collect(toList());
        case RECORD:
            return array
                    .stream()
                    .map(it -> it.getValueType() == JsonValue.ValueType.OBJECT
                            ? buildRecord(elementSchema, it.asJsonObject())
                            : null)
                    .collect(toList());
        default:
            throw new IllegalArgumentException("Unsupported schema: " + elementSchema);
        }
    }

    private Schema toSchema(final JsonObject schema) {
        if (!"schema".equalsIgnoreCase(schema.getString("type")) || !schema.containsKey("schema")) {
            throw new IllegalArgumentException("Not a schema: " + schema);
        }
        return buildSchema(schema.asJsonObject().getJsonObject("schema").getJsonObject("properties"));
    }

    private Schema buildSchema(final JsonObject props) {
        final Schema.Builder builder = recordBuilderFactory.newSchemaBuilder(Schema.Type.RECORD);
        props.forEach((name, definition) -> {
            final JsonObject jsonSchema = definition.asJsonObject();
            final JsonValue typeJs = jsonSchema.get("type");
            switch (typeJs.getValueType()) {
            case STRING:
                addEntry(builder, name, singletonList(JsonString.class.cast(typeJs).getString()), jsonSchema);
                break;
            case ARRAY:
                addEntry(builder, name,
                        typeJs
                                .asJsonArray()
                                .stream()
                                .map(JsonString.class::cast)
                                .map(JsonString::getString)
                                .collect(toList()),
                        jsonSchema);
                break;
            default:
                log.warn("Ignoring {} cause its schema is unknown", name);
                log.debug("{} schema: {}", name, jsonSchema);
            }
        });
        return builder.build();
    }

    private void addEntry(final Schema.Builder builder, final String name, final Collection<String> types,
            final JsonObject jsonSchema) {
        final Schema.Type entryType = mapType(types
                .stream()
                .filter(it -> !"null".equalsIgnoreCase(it))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid type: " + types)));
        final Schema.Entry.Builder entry = recordBuilderFactory.newEntryBuilder().withName(name).withType(entryType);
        if (types.stream().anyMatch("null"::equalsIgnoreCase)) {
            entry.withNullable(true);
        }
        switch (entryType) {
        case ARRAY:
            entry.withElementSchema(buildSchema(jsonSchema.getJsonObject("items")));
            break;
        case RECORD:
            entry.withElementSchema(buildSchema(jsonSchema.getJsonObject("properties")));
            break;
        default:
        }
        builder.withEntry(entry.build());
    }

    private Schema.Type mapType(final String string) {
        switch (string) {
        case "string":
            return Schema.Type.STRING;
        case "array":
            return Schema.Type.ARRAY;
        case "object":
            return Schema.Type.RECORD;
        case "integer":
            return Schema.Type.INT;
        default:
            throw new IllegalArgumentException("Not yet handled type: " + string);
        }
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String rootName() {
        return plugin;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        // no-op
    }
}
