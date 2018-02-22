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

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.talend.sdk.component.runtime.beam.transform.Pipelines.buildBaseJsonPipeline;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.stream.StreamSupport;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.values.KV;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.manager.ComponentManager;

public class AutoKVWrapperTest implements Serializable {

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test() {
        final ComponentManager instance = ComponentManager.instance();
        final JsonBuilderFactory factory = instance.getJsonpBuilderFactory();
        PAssert
                .that(buildBaseJsonPipeline(pipeline, factory)
                        .setCoder(JsonpJsonObjectCoder.of(null))
                        .apply(AutoKVWrapper.of(null,
                                AutoKVWrapper.LocalSequenceHolder.cleanAndGet(getClass().getName() + ".test"), "", "")))
                .satisfies(values -> {
                    final List<KV<String, JsonObject>> items =
                            StreamSupport.stream(values.spliterator(), false).collect(toList());
                    items.sort(comparing(k -> k.getValue().getJsonArray("b1").getJsonObject(0).getString("foo")));
                    assertEquals(2, items.size());
                    assertEquals(2, new HashSet<>(items).size()); // ensure we got 2 ids
                    assertEquals(asList("a", "b"),
                            items
                                    .stream()
                                    .map(k -> k.getValue().getJsonArray("b1").getJsonObject(0).getString("foo"))
                                    .collect(toList()));
                    return null;
                });
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }
}
