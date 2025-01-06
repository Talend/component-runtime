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
package org.talend.sdk.component.runtime.beam.coder;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.json.Json;

import org.junit.jupiter.api.Test;

import lombok.Data;

class JsonRoundTripTest {

    private static final String PLUGIN = jarLocation(JsonpJsonObjectCoderTest.class).getAbsolutePath();

    @Test
    void jsonpToJsonb() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final JsonbCoder<Model> jsonbCoder = JsonbCoder.of(Model.class, PLUGIN);
        final JsonpJsonObjectCoder jsonpCoder = JsonpJsonObjectCoder.of(PLUGIN);
        jsonpCoder.encode(Json.createObjectBuilder().add("test", "foo").build(), outputStream);
        final Model decoded = jsonbCoder.decode(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("foo", decoded.test);
    }

    @Data
    public static class Model {

        public String test;
    }
}
