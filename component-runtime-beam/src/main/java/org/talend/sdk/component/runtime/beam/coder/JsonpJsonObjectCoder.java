/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;

import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.util.VarInt;
import org.talend.sdk.component.runtime.beam.io.CountingOutputStream;
import org.talend.sdk.component.runtime.beam.io.NoCloseInputStream;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class JsonpJsonObjectCoder extends CustomCoder<JsonObject> {

    private JsonReaderFactory readerFactory;

    private JsonWriterFactory writerFactory;

    @Override
    public void encode(final JsonObject jsonObject, final OutputStream outputStream) throws IOException {
        final CountingOutputStream buffer = new CountingOutputStream();
        try (final JsonWriter writer = writerFactory.createWriter(new GZIPOutputStream(buffer))) {
            writer.write(jsonObject);
        }
        VarInt.encode(buffer.getCounter(), outputStream);
        outputStream.write(buffer.toByteArray());
    }

    @Override
    public JsonObject decode(final InputStream inputStream) throws IOException {
        final long maxBytes = VarInt.decodeLong(inputStream);
        try (final JsonReader reader =
                readerFactory.createReader(new GZIPInputStream(new NoCloseInputStream(inputStream, maxBytes)))) {
            final JsonObject jsonObject = reader.readObject();
            return jsonObject;
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return JsonpJsonObjectCoder.class.isInstance(obj) && JsonpJsonObjectCoder.class.cast(obj).isValid();
    }

    @Override
    public int hashCode() {
        return JsonpJsonObjectCoder.class.hashCode();
    }

    private boolean isValid() {
        return readerFactory != null && writerFactory != null;
    }

    public static JsonpJsonObjectCoder of(final String plugin) {
        if (plugin == null) {
            final ComponentManager instance = ComponentManager.instance();
            return new JsonpJsonObjectCoder(instance.getJsonpReaderFactory(), instance.getJsonpWriterFactory());
        }
        final LightContainer container = ContainerFinder.Instance.get().find(plugin);
        return new JsonpJsonObjectCoder(container.findService(JsonReaderFactory.class),
                container.findService(JsonWriterFactory.class));
    }
}
