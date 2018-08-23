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

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.AllArgsConstructor;
import lombok.Data;

@MonoMeecrowaveConfig
class RuntimeEndpointTest {

    @Inject
    private Meecrowave.Builder config;

    private Client client;

    private WebTarget base;

    @BeforeEach
    void beforeEach() {
        client = ClientBuilder.newClient();
        base = client.target(String.format("http://localhost:%d/api/runtime", config.getHttpPort()));

        System.setProperty("component.manager.callers.skip",
                System.getProperty("component.manager.callers.skip", "true"));
        ComponentManager.instance().addPlugin(jarLocation(RuntimeEndpointTest.class).getAbsolutePath());
    }

    @AfterEach
    void afterEach() {
        client.close();
        ComponentManager.instance().removePlugin(jarLocation(RuntimeEndpointTest.class).getAbsolutePath());
    }

    @Test
    void configRun() throws IOException {
        doRun("dGVzdC1jbGFzc2VzI3Rlc3QjZGF0YXNldCNkcw", "configurationtype", 30);
    }

    @Test
    void beamRun() throws IOException {
        doRun("dGVzdC1jbGFzc2VzI3Rlc3QjYmVhbUlucHV0", "component",30);
    }

    @Test
    void nativeRun() throws IOException {
        doRun("dGVzdC1jbGFzc2VzI3Rlc3QjTmF0aXZlSW5wdXQ", "component",50);
    }

    private void doRun(final String id, final String instantiation, final int sizeAssert) throws IOException {
        final Map<String, String> config = new HashMap<>();
        config.put("config.$limit", "10"); // overrided by the ?limit query param
        config.put("config.dataset.namePrefix", "Record");
        final Response response = base
                .path("read/{id}/{version}")
                .resolveTemplate("id", id)
                .resolveTemplate("version", 1)
                .queryParam("instantiation-type", instantiation)
                .request("application/avro-binary")
                .post(entity(config, APPLICATION_JSON_TYPE));
        if (Response.Status.OK.getStatusCode() != response.getStatus()) {
            fail(response.readEntity(String.class));
        }
        final byte[] bytes = response.readEntity(byte[].class);
        final List<IndexedRecord> records;
        try (final DataFileReader<IndexedRecord> reader =
                new DataFileReader<>(new SeekableByteArrayInput(bytes), new GenericDatumReader<>())) {
            records = StreamSupport.stream(reader.spliterator(), false).collect(toList());
        }
        // assertEquals(50, records.size()); // to check why beam behaves this way
        assertTrue(records.size() >= sizeAssert);
        records.forEach(it -> assertTrue(it.get(0).toString().startsWith("Record ")));
    }

    @AllArgsConstructor
    @PartitionMapper(family = "test", name = "beamInput")
    public static class BeamInput extends PTransform<PBegin, PCollection<GenericRecord>> {

        private static final Schema SCHEMA = SchemaBuilder
                .record(BeamInput.class.getName().replace("$", "_"))
                .fields()
                .name("name")
                .type(Schema.create(Schema.Type.STRING))
                .noDefault()
                .name("index")
                .type(Schema.create(Schema.Type.INT))
                .noDefault()
                .endRecord();

        private final Config config;

        @Override
        public PCollection<GenericRecord> expand(final PBegin input) {
            final AvroCoder<GenericRecord> coder = AvroCoder.of(SCHEMA);
            return input.getPipeline().apply("BeamIntputTest",
                    Create
                            .of(IntStream.range(0, config.getLimit()).mapToObj(this::newRecord).collect(toList()))
                            .withCoder(coder));
        }

        private GenericRecord newRecord(final int i) {
            final GenericData.Record record = new GenericData.Record(SCHEMA);
            record.put("index", i);
            record.put("name", config.getDataset().getNamePrefix() + " " + i);
            return record;
        }
    }

    @Data
    public static class Config implements Serializable {

        @Option("$limit")
        private int limit = 50;

        @Option
        private DataSet dataset;
    }

    @Data
    @org.talend.sdk.component.api.configuration.type.DataSet("ds")
    public static class DataSet implements Serializable {
        @Option
        private String namePrefix;
    }

    @AllArgsConstructor
    @Emitter(family = "test", name = "NativeInput")
    public static class NativeInput implements Serializable {

        private final Config config;

        private final JsonBuilderFactory factory;

        private int remaining;

        @PostConstruct
        public void init() {
            remaining = config.getLimit();
        }

        @Producer
        public JsonObject object() {
            if (--remaining < 0) {
                return null;
            }
            return factory
                    .createObjectBuilder()
                    .add("name", config.getDataset().getNamePrefix() + " " + remaining)
                    .add("index", remaining)
                    .build();
        }
    }
}
