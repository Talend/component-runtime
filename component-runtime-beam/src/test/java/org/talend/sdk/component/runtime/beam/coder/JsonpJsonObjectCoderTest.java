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
package org.talend.sdk.component.runtime.beam.coder;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.runtime.beam.TalendIOTest;

class JsonpJsonObjectCoderTest {

    private static final String PLUGIN = jarLocation(TalendIOTest.class).getAbsolutePath();

    @Test
    void encode() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonpJsonObjectCoder.of(PLUGIN).encode(Json.createObjectBuilder().add("test", "foo").build(), outputStream);
        assertEquals("{\"test\":\"foo\"}", new String(outputStream.toByteArray()));
    }

    @Test
    void decode() throws IOException {
        final JsonObject jsonObject = JsonpJsonObjectCoder.of(PLUGIN).decode(
                new ByteArrayInputStream("{\"name\":\"test\"}".getBytes(StandardCharsets.UTF_8)));
        assertEquals("test", jsonObject.getString("name"));
        assertEquals(1, jsonObject.size());
    }
}
