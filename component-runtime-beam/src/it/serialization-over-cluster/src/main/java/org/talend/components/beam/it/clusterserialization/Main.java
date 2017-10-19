// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.beam.it.clusterserialization;

import static java.util.Collections.emptyMap;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

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
import org.talend.components.runtime.beam.TalendFn;
import org.talend.components.runtime.beam.TalendIO;
import org.talend.components.runtime.manager.ComponentManager;

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
                .apply(TalendFn.asFn(manager.findProcessor("sample", "mapper", 1, emptyMap())
                        .orElseThrow(() -> new IllegalStateException("didn't find the processor")), emptyMap()))
                .apply(ParDo.of(new ToStringFn()))
                .apply(TextIO.write().to(ValueProvider.StaticValueProvider.of(options.getOutputFile())));
        final PipelineResult.State state = pipeline.run().waitUntilFinish();
        System.out.println(state);
    }

    static class ToStringFn extends DoFn<Object, String> {

        @ProcessElement
        public void processElement(final ProcessContext context) throws Exception {
            context.output(context.element().toString());
        }
    }

    public interface Config extends PipelineOptions {

        @Description("the read file")
        @Default.String("/tmp/org.talend.components.beam.it.clusterserialization.Main.input")
        String getInputFile();

        void setInputFile(String value);

        @Description("the write file")
        @Default.String("/tmp/org.talend.components.beam.it.clusterserialization.Main.input")
        String getOutputFile();

        void setOutputFile(String value);
    }
}
