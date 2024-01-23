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
package org.talend.sdk.component.runtime.beam.transform;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.talend.sdk.component.runtime.beam.transform.Pipelines.buildBasePipeline;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.values.KV;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.manager.chain.internal.JobImpl;

public class AutoKVWrapperTest implements Serializable {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test() {
        PAssert
                .that(buildBasePipeline(pipeline)
                        .apply(AutoKVWrapper
                                .of(null, JobImpl.LocalSequenceHolder.cleanAndGet(getClass().getName() + ".test"), "",
                                        "")))
                .satisfies(values -> {
                    final List<KV<String, Record>> items = StreamSupport
                            .stream(values.spliterator(), false)
                            .sorted(comparing(
                                    k -> k.getValue().getArray(Record.class, "b1").iterator().next().getString("foo")))
                            .collect(toList());
                    assertEquals(2, items.size());
                    assertEquals(2, new HashSet<>(items).size()); // ensure we got 2 ids
                    assertEquals(asList("a", "b"), items
                            .stream()
                            .map(k -> k.getValue().getArray(Record.class, "b1").iterator().next().getString("foo"))
                            .collect(toList()));
                    return null;
                });
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }
}
