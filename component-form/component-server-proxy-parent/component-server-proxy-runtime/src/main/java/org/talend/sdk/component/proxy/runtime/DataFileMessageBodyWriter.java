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
package org.talend.sdk.component.proxy.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.IndexedRecord;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
@ApplicationScoped
@Produces("application/avro-binary")
public class DataFileMessageBodyWriter implements MessageBodyWriter<Collection<IndexedRecord>> {

    @Override
    public boolean isWriteable(final Class<?> type, final Type genericType, final Annotation[] annotations,
            final MediaType mediaType) {
        if (!Collection.class.isAssignableFrom(type) || !ParameterizedType.class.isInstance(genericType)) {
            return false;
        }
        final ParameterizedType parameterizedType = ParameterizedType.class.cast(genericType);
        return parameterizedType.getActualTypeArguments().length == 1
                && Class.class.isInstance(parameterizedType.getActualTypeArguments()[0]) && IndexedRecord.class
                        .isAssignableFrom(Class.class.cast(parameterizedType.getActualTypeArguments()[0]));
    }

    @Override
    public void writeTo(final Collection<IndexedRecord> records, final Class<?> type, final Type genericType,
            final Annotation[] annotations, final MediaType mediaType, final MultivaluedMap<String, Object> httpHeaders,
            final OutputStream entityStream) throws IOException, WebApplicationException {
        final Schema schema = extractSchema(records);
        try (final DataFileWriter<IndexedRecord> writer = new DataFileWriter<>(new GenericDatumWriter<>(schema))) {
            writer.create(schema, entityStream);
            records.stream().filter(Objects::nonNull).forEach(it -> {
                try {
                    writer.append(it);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    private Schema extractSchema(final Collection<IndexedRecord> records) {
        Schema schema = null;
        for (final IndexedRecord record : records) {
            if (record != null && (schema == null || !new GenericData().validate(schema, record))) {
                schema = record.getSchema();
            }
        }
        return schema == null ? Schema.create(Schema.Type.NULL) : schema;
    }
}
