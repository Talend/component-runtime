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

import static org.junit.Assert.assertEquals;

import java.io.Serializable;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.TypeDescriptor;
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
        pipeline.apply(Create.of("a", "b"))
                .apply(MapElements.into(TypeDescriptor.of(JsonObject.class)).via((String input) -> factory.createObjectBuilder()
                        .add("test", factory.createArrayBuilder().add(factory.createObjectBuilder().add("foo", input))).build()))
                .setCoder(JsonpJsonObjectCoder.of(null)).apply(AutoKVWrapper.of(null));
        assertEquals(PipelineResult.State.DONE, pipeline.run().waitUntilFinish());
    }
}
