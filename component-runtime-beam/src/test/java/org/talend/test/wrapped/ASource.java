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
package org.talend.test.wrapped;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.BoundedSource;
import org.apache.beam.sdk.io.Read;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;

import lombok.Data;
import lombok.EqualsAndHashCode;

public class ASource extends BoundedSource<JsonObject> {

    @PartitionMapper(family = "beamtest", name = "source")
    public static class Tfn extends PTransform<PBegin, PCollection<JsonObject>> {

        @Override
        public PCollection<JsonObject> expand(final PBegin input) {
            return input.apply(Read.from(new ASource()));
        }
    }

    @Override
    public List<? extends BoundedSource<JsonObject>> split(final long desiredBundleSizeBytes,
            final PipelineOptions options) {
        assertClassLoader();
        return singletonList(this);
    }

    @Override
    public long getEstimatedSizeBytes(final PipelineOptions options) {
        assertClassLoader();
        return 1;
    }

    @Override
    public BoundedReader<JsonObject> createReader(final PipelineOptions options) {
        assertClassLoader();
        return new AReader(this);
    }

    @Override
    public Coder<JsonObject> getOutputCoder() {
        return JsonpJsonObjectCoder.of("test-classes");
    }

    private static void assertClassLoader() {
        assertTrue(ConfigurableClassLoader.class.isInstance(Thread.currentThread().getContextClassLoader()));
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class AReader extends BoundedReader<JsonObject> {

        private final ASource source;

        @Override
        public boolean start() throws IOException {
            assertClassLoader();
            return true;
        }

        @Override
        public boolean advance() throws IOException {
            return false;
        }

        @Override
        public JsonObject getCurrent() throws NoSuchElementException {
            assertClassLoader();
            return Json.createObjectBuilder().add("id", "test").build();
        }

        @Override
        public void close() throws IOException {
            assertClassLoader();
        }

        @Override
        public BoundedSource<JsonObject> getCurrentSource() {
            assertClassLoader();
            return source;
        }
    }
}
