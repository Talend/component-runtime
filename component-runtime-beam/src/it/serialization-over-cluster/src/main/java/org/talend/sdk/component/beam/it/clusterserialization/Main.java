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
package org.talend.sdk.component.beam.it.clusterserialization;

import static java.util.Collections.emptyMap;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;
import javax.json.spi.JsonProvider;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.TalendFn;
import org.talend.sdk.component.runtime.beam.TalendIO;
import org.talend.sdk.component.runtime.beam.spi.AvroRecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.beam.transform.ViewsMappingTransform;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.record.RecordConverters;

public class Main {

    public static void main(final String[] args) throws IOException {
        final Config options = PipelineOptionsFactory.fromArgs(args).as(Config.class);
        final Pipeline pipeline = Pipeline.create(options);
        try (final FileWriter writer = new FileWriter(options.getInputFile())) {
            writer.write("normal;6\nmarilyn;36");
        }

        final ComponentManager manager = ComponentManager.instance();
        pipeline.apply(TalendIO.read(manager.findMapper("sample", "reader", 1, new HashMap<String, String>() {

            {
                put("old_file", options.getInputFile()); // will be migrated to "file" with the migration handler
            }
        }).orElseThrow(() -> new IllegalArgumentException("No reader sample#reader, existing: " + manager.availablePlugins()))))
                .apply(new ViewsMappingTransform(emptyMap(), "sample"))
                .apply(TalendFn.asFn(manager.findProcessor("sample", "mapper", 1, emptyMap())
                        .orElseThrow(() -> new IllegalStateException("didn't find the processor"))))
                .apply(ParDo.of(new ToStringFn()))
                .apply(TextIO.write().to(ValueProvider.StaticValueProvider.of(options.getOutputFile())));
        final PipelineResult.State state = pipeline.run().waitUntilFinish();
        System.out.println(state);
    }

    static class ToStringFn extends DoFn<Record, String> {

        @ProcessElement
        public void processElement(final ProcessContext context) {
            // not the best conversion impl but this is really to simplify the asserts, not for "prod"
            final JsonObject asJson = JsonObject.class.cast(new RecordConverters().toType(
                    new RecordConverters.MappingMetaRegistry(), context.element(), JsonObject.class,
                    () -> Json.createBuilderFactory(emptyMap()), JsonProvider::provider, JsonbBuilder::create,
                    () -> new AvroRecordBuilderFactoryProvider().apply("serialization-over-cluster")));
            context.output(asJson.values().iterator().next().asJsonArray().getJsonObject(0).toString());
        }
    }

    public interface Config extends PipelineOptions {

        @Description("the read file")
        @Default.String("/tmp/org.talend.sdk.component.beam.it.clusterserialization.Main.input")
        String getInputFile();

        void setInputFile(String value);

        @Description("the write file")
        @Default.String("/tmp/org.talend.sdk.component.beam.it.clusterserialization.Main.input")
        String getOutputFile();

        void setOutputFile(String value);
    }
}
