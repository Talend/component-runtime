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

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.apache.beam.sdk.transforms.DoFn;

/**
 * Allows to convert an input to a normal output wrapping the value in a __default__ container.
 */
public class RecordNormalizer extends DoFn<JsonObject, JsonObject> {

    private JsonBuilderFactory factory;

    protected RecordNormalizer() {
        // no-op
    }

    public RecordNormalizer(final JsonBuilderFactory factory) {
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
