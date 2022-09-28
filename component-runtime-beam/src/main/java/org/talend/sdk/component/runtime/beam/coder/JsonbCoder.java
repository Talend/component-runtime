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
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.json.bind.Jsonb;

import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.util.VarInt;
import org.talend.sdk.component.runtime.beam.io.CountingOutputStream;
import org.talend.sdk.component.runtime.beam.io.NoCloseInputStream;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class JsonbCoder<T> extends CustomCoder<T> {

    @Getter
    private Type type;

    private Jsonb jsonb;

    @Override
    public void encode(final T object, final OutputStream outputStream) throws IOException {
        final CountingOutputStream buffer = new CountingOutputStream();
        try (final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(buffer)) {
            jsonb.toJson(object, gzipOutputStream);
        }
        VarInt.encode(buffer.getCounter(), outputStream);
        outputStream.write(buffer.toByteArray());
    }

    @Override
    public T decode(final InputStream inputStream) throws IOException {
        final long maxBytes = VarInt.decodeLong(inputStream);
        return jsonb.fromJson(new GZIPInputStream(new NoCloseInputStream(inputStream, maxBytes)), type);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JsonbCoder<?> that = JsonbCoder.class.cast(o);
        return Objects.equals(type, that.type) && (jsonb != null && that.jsonb != null);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    public static <T> JsonbCoder<T> of(final Class<T> type, final String plugin) {
        return new JsonbCoder<>(type, ContainerFinder.Instance.get().find(plugin).findService(Jsonb.class));
    }

    public static JsonbCoder of(final Type type, final String plugin) {
        return new JsonbCoder<>(type, ContainerFinder.Instance.get().find(plugin).findService(Jsonb.class));
    }
}
