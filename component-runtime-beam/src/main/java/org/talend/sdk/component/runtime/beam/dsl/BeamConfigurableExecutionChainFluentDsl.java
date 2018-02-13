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
package org.talend.sdk.component.runtime.beam.dsl;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.runtime.beam.TalendFn;
import org.talend.sdk.component.runtime.beam.TalendIO;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.chain.ExecutionChainDsl;
import org.talend.sdk.component.runtime.manager.chain.internal.DSLParser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeamConfigurableExecutionChainFluentDsl
        implements ExecutionChainDsl, ExecutionChainDsl.ConfigurableExecutionChainFluentDsl {

    private final List<DSLParser.Step> chain = new ArrayList<>();

    private ChainConfiguration configuration;

    @Override
    public ConfigurableExecutionChainFluentDsl from(final String uri) {
        final DSLParser.Step step = DSLParser.parse(uri);
        chain.add(step);
        return this;
    }

    @Override
    public ExecutionChainDsl.ExecutionChainFluentDsl
            configure(final ExecutionChainDsl.ChainConfiguration configuration) {
        this.configuration = configuration;
        if (configuration.getErrorHandler() != null || configuration.getSuccessListener() != null) {
            log.warn("error handler and success listeners are not supported by beam runtime yet");
        }
        return this;
    }

    @Override
    public ExecutionChainDsl.ExecutionChainFluentDsl to(final String uri) {
        if (chain.isEmpty()) {
            throw new IllegalStateException("No from(uri) called");
        }
        chain.add(DSLParser.parse(uri));
        return this;
    }

    @Override
    public ExecutionChainDsl.Execution create() {
        if (chain.size() < 2) {
            throw new IllegalStateException("No from(uri) called or no to(uri) call");
        }
        final ComponentManager manager = ComponentManager.instance();
        final Pipeline pipeline = Pipeline.create(PipelineOptionsFactory.create());

        final boolean override = configuration != null && configuration.isSupportsOverride();
        final DSLParser.Step from = chain.get(0).withOverride(override);
        final Mapper mapper = manager
                .findMapper(from.getFamily(), from.getComponent(), from.getVersion(), from.getConfiguration())
                .orElseThrow(() -> new IllegalArgumentException("Didn't find: " + from));
        PCollection<JsonObject> current = pipeline
                .apply(TalendIO.read(mapper))
                .apply(ParDo.of(new RecordNormalizer(JsonBuilderFactory.class.cast(manager
                        .findPlugin(mapper.plugin())
                        .get()
                        .get(ComponentManager.AllServices.class)
                        .getServices()
                        .get(JsonBuilderFactory.class)))))
                .setCoder(JsonpJsonObjectCoder.of(mapper.plugin()));

        for (int i = 1; i < chain.size() - 1; i++) {
            final DSLParser.Step step = chain.get(i).withOverride(override);
            current = current.apply(TalendFn.asFn(manager
                    .findProcessor(step.getFamily(), step.getComponent(), step.getVersion(), step.getConfiguration())
                    .orElseThrow(() -> new IllegalArgumentException("Didn't find: " + step))));
        }

        final DSLParser.Step last = chain.get(chain.size() - 1).withOverride(override);
        current.apply(TalendIO.write(manager
                .findProcessor(last.getFamily(), last.getComponent(), last.getVersion(), last.getConfiguration())
                .orElseThrow(() -> new IllegalArgumentException("Didn't find: " + last))));

        return () -> pipeline.run().waitUntilFinish();
    }

    private static class RecordNormalizer extends DoFn<JsonObject, JsonObject> {

        private JsonBuilderFactory factory;

        protected RecordNormalizer() {
            // no-op
        }

        private RecordNormalizer(final JsonBuilderFactory factory) {
            this.factory = factory;
        }

        @ProcessElement
        public void onElement(final ProcessContext context) {
            context.output(toMap(context.element()));
        }

        private JsonObject toMap(final JsonObject element) {
            return factory.createObjectBuilder().add("__default__", factory.createArrayBuilder().add(element)).build();
        }
    }
}
