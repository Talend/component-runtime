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

import java.io.Serializable;
import java.util.Map;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.input.Input;

import lombok.RequiredArgsConstructor;

// see https://www.stitchdata.com/docs/stitch-connect/api
@RequiredArgsConstructor
public class StitchInput implements Input, Serializable {

    private final String plugin;

    private final String name;

    private final Map<String, String> configuration;

    private final JsonBuilderFactory jsonBuilderFactory;

    private final RecordBuilderFactory recordBuilderFactory;

    private final HttpClientFactory clientFactory;

    private transient StitchClient client;

    private transient String authorization;

    @Override
    public Object next() {
        ensureData();
        return null;
    }

    private void ensureData() {
        if (client == null) {
            final String base = configuration.get("configuration.dataset.configuration.datastore.stitchConnection.url");
            final String token =
                    configuration.get("configuration.dataset.configuration.datastore.stitchConnection.token");
            if (base == null || token == null) {
                throw new IllegalArgumentException("Missing stitch connection configuration");
            }
            this.authorization = "Bearer " + token;
            this.client = clientFactory.create(StitchClient.class, base);
        }

        // TODO

        // 1. create the source
        /*
         * curl -X POST https://api.stitchdata.com/v4/sources
         * -H "Authorization: Bearer <ACCESS_TOKEN>"
         * -H "Content-Type: application/json"
         * -d "{
         * "type":"platform.hubspot",
         * "display_name":"HubSpot",
         * "properties":{
         * "start_date":"2017-01-01T00:00:00Z",
         * "frequency_in_minutes":"30"
         * }
         * }"
         */
        final JsonObject source = client
                .createSource(authorization, "application/json", jsonBuilderFactory.createObjectBuilder().build());
        final String sourceId = source.getString("id");
        try {

            // 2. get schema for the stream
            /*
             * curl -X GET https://api.stitchdata.com/v4/sources/{source_id}/streams/{stream_id}
             * -H "Authorization: Bearer <ACCESS_TOKEN>"
             * -H "Content-Type: application/json"
             */
            final JsonObject schema = client.getSchema(authorization, "application/json", sourceId, null /* TODO */);

            // 3. customize schema to retrieve
            /*
             * curl -X PUT https://api.stitchdata.com/v4/sources/{source_id}/streams/metadata
             * -H "Authorization: Bearer <ACCESS_TOKEN>"
             * -H "Content-Type: application/json"
             * -d "{
             * "streams": [
             * {
             * "tap_stream_id": "custom_collections",
             * "metadata": [
             * {
             * "breadcrumb": [],
             * "metadata": {
             * "selected": "true"
             * }
             * }
             * ]
             * }
             * ]
             * }"
             */
            // TODO

            // 4. get data
            /*
             * TBD
             */
            // TODO: load and map to Record
        } finally {
            // 5. delete the source
            /*
             * curl -X DELETE https://api.stitchdata.com/v4/sources/{id}
             * -H "Authorization: Bearer <ACCESS_TOKEN>"
             * -H "Content-Type: application/json"
             * -d "{}"
             */
            client
                    .deleteSource(authorization, "application/json", sourceId,
                            jsonBuilderFactory.createObjectBuilder().build());
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
