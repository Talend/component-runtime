/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.transform;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adapter to convert a ProcessContext coming from a CoGBK to a
 * {@code Map<String, List<?>>}
 */
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class CoGroupByKeyResultMappingTransform<K>
        extends PTransform<PCollection<KV<K, CoGbkResult>>, PCollection<JsonObject>> {

    private String plugin;

    private boolean propagateKey;

    @Override
    public PCollection<JsonObject> expand(final PCollection<KV<K, CoGbkResult>> input) {
        return input.apply(ParDo.of(new CoGBKMappingFn<>(plugin, propagateKey, null, null)));
    }

    @Override
    protected Coder<?> getDefaultOutputCoder() {
        return JsonpJsonObjectCoder.of(plugin);
    }

    @AllArgsConstructor(access = PRIVATE)
    @NoArgsConstructor(access = PROTECTED)
    public static class CoGBKMappingFn<K> extends DoFn<KV<K, CoGbkResult>, JsonObject> {

        private String plugin;

        private boolean propagateKey;

        private volatile JsonBuilderFactory builderFactory;

        private volatile Jsonb jsonb;

        @ProcessElement
        public void onElement(final ProcessContext context) {
            context.output(createMap(context));
        }

        private JsonObject createMap(final ProcessContext context) {
            final KV<K, CoGbkResult> element = context.element();
            final CoGbkResult result = element.getValue();
            final JsonObjectBuilder builder = result
                    .getSchema()
                    .getTupleTagList()
                    .getAll()
                    .stream()
                    .map(key -> new Pair<>(key.getId(), JsonObject.class.cast(result.getOnly(key, null))))
                    .filter(p -> p.getSecond() != null)
                    .collect(builderFactory()::createObjectBuilder,
                            (b, p) -> b.add(p.getFirst(), builderFactory.createArrayBuilder().add(p.getSecond())),
                            JsonObjectBuilder::addAll);
            if (propagateKey) {
                builder.add("$$internal",
                        builderFactory.createObjectBuilder().add("key", String.valueOf(element.getKey())));
            }
            return builder.build();
        }

        private JsonBuilderFactory builderFactory() {
            if (builderFactory == null) {
                synchronized (this) {
                    if (builderFactory == null) {
                        final LightContainer container = ContainerFinder.Instance.get().find(plugin);
                        builderFactory = container.findService(JsonBuilderFactory.class);
                        jsonb = container.findService(Jsonb.class);
                    }
                }
            }
            return builderFactory;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Pair<A, B> {

        private A first;

        private B second;
    }
}
