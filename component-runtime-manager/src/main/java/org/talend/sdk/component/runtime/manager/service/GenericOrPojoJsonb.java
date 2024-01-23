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
package org.talend.sdk.component.runtime.manager.service;

import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbException;

import org.talend.sdk.component.runtime.record.json.PojoJsonbProvider;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GenericOrPojoJsonb implements Jsonb, PojoJsonbProvider, Serializable {

    private final String plugin;

    private final Jsonb jsonb;

    @Getter
    private final Jsonb pojoMapper;

    @Override
    public Jsonb get() {
        return pojoMapper;
    }

    @Override
    public void close() {
        Stream.of(jsonb, pojoMapper).filter(Objects::nonNull).forEach(it -> {
            try {
                it.close();
            } catch (final Exception e) {
                log.warn(e.getMessage(), e);
            }
        });
    }

    @Override
    public <T> T fromJson(final String str, final Class<T> type) throws JsonbException {
        return jsonb.fromJson(str, type);
    }

    @Override
    public <T> T fromJson(final String str, final Type runtimeType) throws JsonbException {
        return jsonb.fromJson(str, runtimeType);
    }

    @Override
    public <T> T fromJson(final Reader reader, final Class<T> type) throws JsonbException {
        return jsonb.fromJson(reader, type);
    }

    @Override
    public <T> T fromJson(final Reader reader, final Type runtimeType) throws JsonbException {
        return jsonb.fromJson(reader, runtimeType);
    }

    @Override
    public <T> T fromJson(final InputStream stream, final Class<T> type) throws JsonbException {
        return jsonb.fromJson(stream, type);
    }

    @Override
    public <T> T fromJson(final InputStream stream, final Type runtimeType) throws JsonbException {
        return jsonb.fromJson(stream, runtimeType);
    }

    @Override
    public String toJson(final Object object) throws JsonbException {
        return jsonb.toJson(object);
    }

    @Override
    public String toJson(final Object object, final Type runtimeType) throws JsonbException {
        return jsonb.toJson(object, runtimeType);
    }

    @Override
    public void toJson(final Object object, final Writer writer) throws JsonbException {
        jsonb.toJson(object, writer);
    }

    @Override
    public void toJson(final Object object, final Type runtimeType, final Writer writer) throws JsonbException {
        jsonb.toJson(object, runtimeType, writer);
    }

    @Override
    public void toJson(final Object object, final OutputStream stream) throws JsonbException {
        jsonb.toJson(object, stream);
    }

    @Override
    public void toJson(final Object object, final Type runtimeType, final OutputStream stream) throws JsonbException {
        jsonb.toJson(object, runtimeType, stream);
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, Jsonb.class.getName());
    }
}
