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

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonMergePatch;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

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
    public JsonObjectBuilder createObjectBuilder(final Map<String, Object> map) {
        return builderFactory.createObjectBuilder(map);
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
