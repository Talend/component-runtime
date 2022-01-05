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
package org.talend.sdk.component.junit.beam;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import javax.json.Json;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.junit.JoinInputFactory;
import org.talend.sdk.component.junit.SimpleComponentRule;
import org.talend.sdk.component.junit.beam.test.SampleProcessor;
import org.talend.sdk.component.runtime.beam.TalendFn;
import org.talend.sdk.component.runtime.output.Processor;

public class ProcessorTest {

    @ClassRule
    public static final SimpleComponentRule COMPONENT_FACTORY =
            new SimpleComponentRule(SampleProcessor.class.getPackage().getName());

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @Test
    public void processor() {
        final Processor processor = COMPONENT_FACTORY.createProcessor(SampleProcessor.class, new Object());

        final JoinInputFactory joinInputFactory = new JoinInputFactory()
                .withInput("__default__",
                        asList(new SampleProcessor.Sample(1), Json.createObjectBuilder().add("data", 2).build()));

        final PCollection<Record> inputs =
                pipeline.apply(Data.of(processor.plugin(), joinInputFactory.asInputRecords()));

        final PCollection<Map<String, Record>> outputs =
                inputs.apply(TalendFn.asFn(processor)).apply(Data.map(processor.plugin(), Record.class));

        PAssert.that(outputs).satisfies((SerializableFunction<Iterable<Map<String, Record>>, Void>) input -> {
            final List<Map<String, Record>> result = StreamSupport.stream(input.spliterator(), false).collect(toList());

            assertEquals(2, result.size());
            result.forEach(e -> assertTrue(e.containsKey("__default__") && e.containsKey("reject")));
            assertEquals(new HashSet<>(asList(1, 2)),
                    result.stream().map(e -> e.get("__default__").getInt("data")).collect(toSet()));
            return null;
        });

        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }
}
