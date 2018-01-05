/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.front;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.ws.rs.client.WebTarget;

import org.talend.sdk.component.server.test.meecrowave.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.talend.sdk.component.server.front.model.execution.WriteStatistics;
import org.talend.sdk.component.server.test.websocket.WebsocketClient;

@MonoMeecrowaveConfig
class ExecutionResourceTest {

    @Inject
    private WebTarget base;

    @Inject
    private WebsocketClient ws;

    @Test
    void websocketRead() {
        final String result = ws.read(String.class, "post", "/execution/read/chain/list",
                Json.createObjectBuilder().add("values[0]", "v1").add("values[1]", "v2").build().toString(),
                "talend/stream");
        assertEquals("{\"value\":\"v1\"}\n{\"value\":\"v2\"}\n", result);
    }

    @Test
    void websocketBusRead() {
        final String result = ws.read(String.class, "post", "/execution/read/chain/list",
                Json.createObjectBuilder().add("values[0]", "v1").add("values[1]", "v2").build().toString(),
                "talend/stream");
        assertEquals("{\"value\":\"v1\"}\n{\"value\":\"v2\"}\n", result);
    }

    @Test
    void read() {
        final String output = base
                .path("execution/read/{family}/{component}")
                .resolveTemplate("family", "chain")
                .resolveTemplate("component", "list")
                .request("talend/stream")
                .post(entity(Json.createObjectBuilder().add("values[0]", "v1").add("values[1]", "v2").build(),
                        APPLICATION_JSON_TYPE), String.class);
        assertEquals("{\"value\":\"v1\"}\n{\"value\":\"v2\"}\n", output);
    }

    @Test
    void write(final TestInfo info) throws IOException {
        final File outputFile = new File(jarLocation(ExecutionResourceTest.class).getParentFile(),
                getClass().getSimpleName() + "_" + info.getTestMethod().get().getName() + ".output");
        final JsonBuilderFactory objectFactory = Json.createBuilderFactory(emptyMap());
        final WriteStatistics stats = base
                .path("execution/write/{family}/{component}")
                .resolveTemplate("family", "file")
                .resolveTemplate("component", "output")
                .request(APPLICATION_JSON_TYPE)
                .post(entity(objectFactory.createObjectBuilder().add("file", outputFile.getAbsolutePath()).build()
                        + "\n" + objectFactory.createObjectBuilder().add("line1", "v1").build() + "\n"
                        + objectFactory.createObjectBuilder().add("line2", "v2").build(), "talend/stream"),
                        WriteStatistics.class);
        assertTrue(outputFile.isFile());
        assertEquals(2, stats.getCount());
        assertEquals("{\"line1\":\"v1\"}\n{\"line2\":\"v2\"}",
                Files.readAllLines(outputFile.toPath()).stream().collect(joining("\n")));
    }
}
