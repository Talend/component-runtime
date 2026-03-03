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
package org.talend.sdk.component.api.service;

import java.util.Map;
import java.util.stream.Collector;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;

public class JsonProviderCache {

    public static final JsonProvider JSON_PROVIDER = JsonProvider.provider();

    public static final JsonbProvider JSONB_PROVIDER = JsonbProvider.provider();

    public static final Collector<JsonValue, JsonArrayBuilder, JsonArray> JSON_ARRAY_COLLECTOR =
            Collector.of(JSON_PROVIDER::createArrayBuilder,
                    JsonArrayBuilder::add,
                    JsonArrayBuilder::addAll,
                    JsonArrayBuilder::build);

    public static final Collector<Map.Entry<String, JsonValue>, JsonObjectBuilder, JsonObject> JSON_OBJECT_COLLECTOR =
            Collector.of(JSON_PROVIDER::createObjectBuilder,
                    JsonProviderCache::addEntry,
                    JsonObjectBuilder::addAll,
                    JsonObjectBuilder::build);

    public static void addEntry(final JsonObjectBuilder objectBuilder, final Map.Entry<String, JsonValue> entry) {
        objectBuilder.add(entry.getKey(), entry.getValue());
    }

    private static final Jsonb JSONBI = JSONB_PROVIDER.create().withProvider(JSON_PROVIDER).build();

    static {
        System.setProperty(JsonProvider.class.getName(), JSON_PROVIDER.getClass().getName());
        System.out.println(JSONBI);
    }
}
