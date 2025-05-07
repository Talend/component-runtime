/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front.beam;

import static java.util.Collections.emptyMap;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;

import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;

@MonoMeecrowaveConfig
class BeamActionSerializationTest {

    @Inject
    private WebTarget base;

    @Test
    void checkSchemaSerialization() {
        final String schema = base
                .path("action/execute")
                .queryParam("type", "schema")
                .queryParam("family", "jdbc")
                .queryParam("action", "jdbc_discover_schema")
                .queryParam("lang", "it")
                .request(APPLICATION_JSON_TYPE)
                .post(Entity.entity(emptyMap(), APPLICATION_JSON_TYPE), String.class);
        final String attended = "{\n" + "  \"entries\":[\n" + "    {\n" + "      \"elementSchema\":{\n"
                + "        \"entries\":[\n" + "        ],\n" + "        \"metadata\":[\n" + "        ],\n"
                + "        \"props\":{\n" + "\n" + "        },\n" + "        \"type\":\"STRING\"\n" + "      },\n"
                + "      \"metadata\":false,\n" + "      \"name\":\"array\",\n" + "      \"nullable\":false,\n"
                + "      \"props\":{\n" + "\n" + "      },\n" + "      \"type\":\"ARRAY\",\n"  +
                "      \"valid\":true\n    }\n" + "  ],\n"
                + "  \"metadata\":[\n" + "  ],\n" + "  \"props\":{\n" + "    \"talend.fields.order\":\"array\"\n"
                + "  },\n" + "  \"type\":\"RECORD\"\n" + "}";
        assertEquals(attended, schema);
    }
}
