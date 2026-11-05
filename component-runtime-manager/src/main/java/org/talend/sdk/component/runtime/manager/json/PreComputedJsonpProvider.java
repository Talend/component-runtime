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
package org.talend.sdk.component.runtime.manager.json;

import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonPatch;
import jakarta.json.JsonPatchBuilder;
import jakarta.json.JsonPointer;
import jakarta.json.JsonReader;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParserFactory;

import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PreComputedJsonpProvider extends JsonProvider implements Serializable {

    private final String plugin;

    private final JsonProvider jsonpProvider;

    private final JsonParserFactory parserFactory;

    private final JsonWriterFactory writerFactory;

    private final JsonBuilderFactory builderFactory;

    private final JsonGeneratorFactory generatorFactory;

    private final JsonReaderFactory readerFactory;

    @Override
    public JsonReaderFactory createReaderFactory(final Map<String, ?> config) {
        return readerFactory;
    }

    @Override
    public JsonObjectBuilder createObjectBuilder() {
        return builderFactory.createObjectBuilder();
    }

    @Override
    public JsonObjectBuilder createObjectBuilder(final JsonObject jsonObject) {
        return builderFactory.createObjectBuilder(jsonObject);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonObjectBuilder createObjectBuilder(final Map<String, ?> map) {
        // JsonBuilderFactory#createObjectBuilder only accepts Map<String, Object> (jakarta.json-api
        // 2.1 widened JsonProvider's own signature to Map<String, ?> but not this one)
        return builderFactory.createObjectBuilder((Map<String, Object>) map);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder() {
        return builderFactory.createArrayBuilder();
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(final JsonArray initialData) {
        return builderFactory.createArrayBuilder(initialData);
    }

    @Override
    public JsonArrayBuilder createArrayBuilder(final Collection<?> initialData) {
        return builderFactory.createArrayBuilder(initialData);
    }

    @Override
    public JsonPointer createPointer(final String path) {
        return jsonpProvider.createPointer(path);
    }

    @Override
    public JsonBuilderFactory createBuilderFactory(final Map<String, ?> config) {
        return builderFactory;
    }

    @Override
    public JsonString createValue(final String value) {
        return jsonpProvider.createValue(value);
    }

    @Override
    public JsonNumber createValue(final int value) {
        return jsonpProvider.createValue(value);
    }

    @Override
    public JsonNumber createValue(final long value) {
        return jsonpProvider.createValue(value);
    }

    @Override
    public JsonNumber createValue(final double value) {
        return jsonpProvider.createValue(value);
    }

    @Override
    public JsonNumber createValue(final BigDecimal value) {
        return jsonpProvider.createValue(value);
    }

    @Override
    public JsonNumber createValue(final BigInteger value) {
        return jsonpProvider.createValue(value);
    }

    @Override
    public JsonPatch createPatch(final JsonArray array) {
        return jsonpProvider.createPatch(array);
    }

    @Override
    public JsonPatch createDiff(final JsonStructure source, final JsonStructure target) {
        return jsonpProvider.createDiff(source, target);
    }

    @Override
    public JsonPatchBuilder createPatchBuilder() {
        return jsonpProvider.createPatchBuilder();
    }

    @Override
    public JsonPatchBuilder createPatchBuilder(final JsonArray initialData) {
        return jsonpProvider.createPatchBuilder(initialData);
    }

    @Override
    public JsonMergePatch createMergePatch(final JsonValue patch) {
        return jsonpProvider.createMergePatch(patch);
    }

    @Override
    public JsonMergePatch createMergeDiff(final JsonValue source, final JsonValue target) {
        return jsonpProvider.createMergeDiff(source, target);
    }

    @Override
    public JsonGeneratorFactory createGeneratorFactory(final Map<String, ?> config) {
        return generatorFactory;
    }

    @Override
    public JsonReader createReader(final Reader reader) {
        return readerFactory.createReader(reader);
    }

    @Override
    public JsonReader createReader(final InputStream in) {
        return readerFactory.createReader(in);
    }

    @Override
    public JsonWriter createWriter(final Writer writer) {
        return writerFactory.createWriter(writer);
    }

    @Override
    public JsonWriter createWriter(final OutputStream out) {
        return writerFactory.createWriter(out);
    }

    @Override
    public JsonWriterFactory createWriterFactory(final Map<String, ?> config) {
        return writerFactory;
    }

    @Override
    public JsonParser createParser(final Reader reader) {
        return parserFactory.createParser(reader);
    }

    @Override
    public JsonParser createParser(final InputStream in) {
        return parserFactory.createParser(in);
    }

    @Override
    public JsonParserFactory createParserFactory(final Map<String, ?> config) {
        return parserFactory;
    }

    @Override
    public JsonGenerator createGenerator(final Writer writer) {
        return generatorFactory.createGenerator(writer);
    }

    @Override
    public JsonGenerator createGenerator(final OutputStream out) {
        return generatorFactory.createGenerator(out);
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, JsonProvider.class.getName());
    }
}
