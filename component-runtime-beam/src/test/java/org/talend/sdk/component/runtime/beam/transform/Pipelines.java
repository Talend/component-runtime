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

import static lombok.AccessLevel.PRIVATE;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TypeDescriptor;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class Pipelines {

    static PCollection<JsonObject> buildBaseJsonPipeline(final TestPipeline pipeline,
            final JsonBuilderFactory factory) {
        return pipeline
                .apply(Create.of("a", "b"))
                .apply(MapElements
                        .into(TypeDescriptor.of(JsonObject.class))
                        .via((String input) -> factory
                                .createObjectBuilder()
                                .add("b1", factory.createArrayBuilder().add(factory
                                        .createObjectBuilder()

                                        .add("foo", input)))
                                .add("b2", factory.createArrayBuilder().add(factory
                                        .createObjectBuilder()

                                        .add("bar", input)))
                                .build()))
                .setCoder(JsonpJsonObjectCoder.of(null));
    }
}
