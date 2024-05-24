/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.beam.sdk.util.VarInt;
import org.apache.ziplock.IO;
import org.junit.jupiter.api.Test;

import lombok.Data;

public class JsonbCoderTest {

    @Test
    void roundTrip() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final JsonbCoder<Model> coder = JsonbCoder.of(Model.class, jarLocation(JsonbCoderTest.class).getAbsolutePath());
        final Model model = new Model();
        model.name = "test";
        coder.encode(model, outputStream);
        final Model decoded = coder.decode(new ByteArrayInputStream(outputStream.toByteArray()));
        final InputStream in = new ByteArrayInputStream(outputStream.toByteArray());
        VarInt.decodeLong(in);
        try (final InputStream stream = new GZIPInputStream(in)) {
            assertTrue(IO.slurp(stream).endsWith("{\"name\":\"test\"}"));
        }
        assertEquals(model.name, decoded.name);
    }

    @Data
    public static class Model {

        public String name;
    }
}
