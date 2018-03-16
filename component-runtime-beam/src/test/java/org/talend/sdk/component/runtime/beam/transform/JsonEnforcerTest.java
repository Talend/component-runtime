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
import static java.util.stream.Collectors.toList;
import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.List;
import java.util.stream.StreamSupport;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.runtime.beam.TalendIOTest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class JsonEnforcerTest implements Serializable {

    private static final String PLUGIN = jarLocation(TalendIOTest.class).getAbsolutePath();

    @Rule
    public final transient TestPipeline pipeline = TestPipeline.create();

    @Test
    public void test() {
        PAssert
                .that(pipeline
                        .apply(Create.of(new Pojo("1"), new Pojo("2")).withCoder(SerializableCoder.of(Pojo.class)))
                        .apply(JsonEnforcer.of(PLUGIN)))
                .satisfies(values -> {
                    final List<String> items = StreamSupport
                            .stream(values.spliterator(), false)
                            .map(k -> k.getString("id"))
                            .sorted()
                            .collect(toList());
                    assertEquals(asList("1", "2"), items);
                    return null;
                });
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pojo implements Serializable {

        private String id;
    }
}
