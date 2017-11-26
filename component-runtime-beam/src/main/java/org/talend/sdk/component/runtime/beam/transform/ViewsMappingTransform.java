/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.beam.sdk.coders.CannotProvideCoderException;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.talend.sdk.component.runtime.beam.TalendCoder;

/**
 * Adapter to convert a ProcessContext to a
 * <code><pre>Map<String, List<?>></pre></code>.
 */
public class ViewsMappingTransform<I extends Serializable>
        extends PTransform<PCollection<I>, PCollection<Map<String, List<Serializable>>>> {

    private final Map<String, PCollectionView<?>> views;

    public ViewsMappingTransform(final Map<String, PCollectionView<?>> sides) {
        this.views = sides;
    }

    @Override
    public PCollection<Map<String, List<Serializable>>> expand(final PCollection<I> input) {
        return input.apply(ParDo.of(new MappingViewsFn<>(views)));
    }

    @Override
    protected Coder<?> getDefaultOutputCoder() throws CannotProvideCoderException {
        return TalendCoder.of();
    }

    public static class MappingViewsFn<I extends Serializable> extends DoFn<I, Map<String, List<Serializable>>> {

        private final Map<String, PCollectionView<?>> views;

        protected MappingViewsFn() {
            this.views = emptyMap();
        }

        private MappingViewsFn(final Map<String, PCollectionView<?>> views) {
            this.views = views.entrySet().stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        @ProcessElement
        public void onElement(final ProcessContext context) {
            context.output(createMap(context));
        }

        private Map<String, List<Serializable>> createMap(final ProcessContext context) {
            final Map<String, List<Serializable>> map = new HashMap<>();
            map.put("__default__", singletonList(context.element()));
            views.forEach((n, v) -> {
                final Serializable sideInput = Serializable.class.cast(context.sideInput(v));
                map.put(n, singletonList(sideInput));
            });
            return map;
        }
    }
}
