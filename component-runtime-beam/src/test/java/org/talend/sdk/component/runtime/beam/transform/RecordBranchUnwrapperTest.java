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

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.talend.sdk.component.runtime.beam.transform.Pipelines.buildBasePipeline;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.api.record.Record;

public class RecordBranchUnwrapperTest implements Serializable {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test() {
        PAssert
                .that(buildBasePipeline(pipeline).apply(RecordBranchMapper.of(null, "b1", "other")))
                .satisfies(values -> {
                    final List<Record> items = StreamSupport.stream(values.spliterator(), false).collect(toList());
                    assertEquals(2, items.size());
                    items.forEach(item -> {
                        final Collection<Record> other = item.getArray(Record.class, "other");
                        assertNotNull(other);
                        assertNotNull(other.iterator().next().getString("foo"));
                        assertNull(item.get(Object.class, "b1"));
                        assertNotNull(item.get(Object.class, "b2"));
                    });
                    return null;
                });
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }
}
