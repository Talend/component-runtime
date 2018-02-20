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
package org.talend.sdk.component.runtime.beam.transform;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.talend.sdk.component.runtime.beam.transform.Pipelines.buildBaseJsonPipeline;

import java.io.Serializable;
import java.util.List;
import java.util.stream.StreamSupport;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.runtime.manager.ComponentManager;

public class RecordBranchMapperTest implements Serializable {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test() {
        final ComponentManager instance = ComponentManager.instance();
        final JsonBuilderFactory factory = instance.getJsonpBuilderFactory();
        PAssert
                .that(buildBaseJsonPipeline(pipeline, factory).apply(RecordBranchMapper.of(null, "b1", "other")))
                .satisfies(values -> {
                    final List<JsonObject> items = StreamSupport.stream(values.spliterator(), false).collect(toList());
                    assertEquals(2, items.size());
                    items.forEach(item -> {
                        assertTrue(item.containsKey("other"));
                        assertNotNull(item.getJsonArray("other").getJsonObject(0).getString("foo"));
                        assertFalse(item.containsKey("b1"));
                        assertTrue(item.containsKey("b2"));
                        assertNotNull(item.getJsonArray("b2").getJsonObject(0).getString("bar"));
                    });
                    return null;
                });
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }
}
