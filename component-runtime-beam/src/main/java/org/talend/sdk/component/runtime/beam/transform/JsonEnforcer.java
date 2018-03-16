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

import static lombok.AccessLevel.PROTECTED;

import javax.json.JsonObject;
import javax.json.bind.Jsonb;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.beam.transform.service.ServiceLookup;
import org.talend.sdk.component.runtime.manager.ComponentManager;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
public class JsonEnforcer<T> extends DoFn<T, JsonObject> {

    private String plugin;

    private transient Jsonb jsonb;

    @ProcessElement
    public void onElement(final ProcessContext context) {
        context.output(toJson(context.element()));
    }

    private JsonObject toJson(final Object element) {
        if (JsonObject.class.isInstance(element)) {
            return JsonObject.class.cast(element);
        }
        if (jsonb == null) {
            jsonb = ServiceLookup.lookup(ComponentManager.instance(), plugin, Jsonb.class);
        }
        return jsonb.fromJson(jsonb.toJson(element), JsonObject.class);
    }

    public static <T> PTransform<PCollection<T>, PCollection<JsonObject>> of(final String plugin) {
        return new Transform<>(JsonpJsonObjectCoder.of(plugin), new JsonEnforcer<>(plugin, null));
    }

    @AllArgsConstructor
    @NoArgsConstructor(access = PROTECTED)
    static class Transform<T> extends PTransform<PCollection<T>, PCollection<JsonObject>> {

        private Coder<JsonObject> coder;

        private DoFn<T, JsonObject> fn;

        @Override
        public PCollection<JsonObject> expand(final PCollection<T> input) {
            return input.apply(ParDo.of(fn)).setCoder(coder);
        }
    }
}
