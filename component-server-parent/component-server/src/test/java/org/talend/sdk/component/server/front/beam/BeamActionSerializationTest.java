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
package org.talend.sdk.component.server.front.beam;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;

import org.apache.meecrowave.junit5.MeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// jsonbPrettify=true restores what InitTestInfra's Meecrowave.ConfigurationCustomizer already sets: any
// @MeecrowaveConfig-annotated test unconditionally re-applies every annotation attribute - including
// jsonbPrettify's own "false" default - onto the builder via reflection, silently overriding the
// customizer once a test stops using @MonoMeecrowaveConfig (which never applies annotation attributes at all).
@MeecrowaveConfig(scanningExcludes = "smallrye-config", jsonbPrettify = true)
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
        final String attended = """
                                {
                                  "entries":[
                                    {
                                      "elementSchema":{
                                        "entries":[
                                        ],
                                        "metadata":[
                                        ],
                                        "props":{

                                        },
                                        "type":"STRING"
                                      },
                                      "errorCapable":false,
                                      "metadata":false,
                                      "name":"array",
                                      "nullable":false,
                                      "props":{

                                      },
                                      "type":"ARRAY",
                                      "valid":true
                                    }
                                  ],
                                  "metadata":[
                                  ],
                                  "props":{
                                    "talend.fields.order":"array"
                                  },
                                  "type":"RECORD"
                                }""";
        assertEquals(attended, schema);
    }
}
