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

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PROTECTED;

import java.util.Map;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.beam.sdk.coders.CannotProvideCoderException;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Adapter to convert a ProcessContext to a
 * {@code Map<String, List<?>>} encapsulated in a {@code JsonObject}.
 */
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
public class ViewsMappingTransform extends PTransform<PCollection<JsonObject>, PCollection<JsonObject>> {

    private Map<String, PCollectionView<?>> views;

    private String plugin;

    @Override
    public PCollection<JsonObject> expand(final PCollection<JsonObject> input) {
        return input.apply(ParDo.of(new MappingViewsFn(views, plugin)));
    }

    @Override
    protected Coder<?> getDefaultOutputCoder() throws CannotProvideCoderException {
        return JsonpJsonObjectCoder.of(plugin);
    }

    @NoArgsConstructor(access = PROTECTED)
    public static class MappingViewsFn extends DoFn<JsonObject, JsonObject> {

        private volatile JsonBuilderFactory builderFactory;

        private String plugin;

        private Map<String, PCollectionView<?>> views;

        private MappingViewsFn(final String plugin) {
            this(emptyMap(), plugin);
        }

        private MappingViewsFn(final Map<String, PCollectionView<?>> views, final String plugin) {
            this.views = views.entrySet().stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            this.plugin = plugin;
        }

        @ProcessElement
        public void onElement(final ProcessContext context) {
            context.output(createMap(context));
        }

        private JsonObject createMap(final ProcessContext context) {
            final JsonObjectBuilder builder = builderFactory().createObjectBuilder();
            builder.add("__default__", builderFactory.createArrayBuilder().add(context.element()));
            views.forEach((n, v) -> {
                final JsonObject sideInput = JsonObject.class.cast(context.sideInput(v));
                builder.add(n, builderFactory.createArrayBuilder().add(sideInput));
            });
            return builder.build();
        }

        private JsonBuilderFactory builderFactory() {
            if (builderFactory == null) {
                synchronized (this) {
                    if (builderFactory == null) {
                        builderFactory =
                                ContainerFinder.Instance.get().find(plugin).findService(JsonBuilderFactory.class);
                    }
                }
            }
            return builderFactory;
        }
    }
}
