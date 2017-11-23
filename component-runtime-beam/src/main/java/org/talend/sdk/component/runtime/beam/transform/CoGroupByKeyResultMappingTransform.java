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

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.beam.sdk.coders.CannotProvideCoderException;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.runtime.beam.TalendCoder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adapter to convert a ProcessContext coming from a CoGBK to a <code><pre>Map<String, List<?>></pre></code>.
 */
public class CoGroupByKeyResultMappingTransform<K>
        extends PTransform<PCollection<KV<K, CoGbkResult>>, PCollection<Map<String, List<Serializable>>>> {

    @Override
    public PCollection<Map<String, List<Serializable>>> expand(final PCollection<KV<K, CoGbkResult>> input) {
        return input.apply(ParDo.of(new CoGBKMappingFn<>()));
    }

    @Override
    protected Coder<?> getDefaultOutputCoder() throws CannotProvideCoderException {
        return TalendCoder.of();
    }

    public static class CoGBKMappingFn<K> extends DoFn<KV<K, CoGbkResult>, Map<String, List<Serializable>>> {

        @ProcessElement
        public void onElement(final ProcessContext context) {
            context.output(createMap(context));
        }

        private Map<String, List<Serializable>> createMap(final ProcessContext context) {
            final CoGbkResult result = context.element().getValue();
            return result.getSchema().getTupleTagList().getAll().stream()
                    .map(key -> new Pair<>(key, Serializable.class.cast(result.getOnly(key))))
                    .collect(toMap(pair -> pair.getFirst().getId(), pair -> singletonList(pair.getSecond())));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class Pair<A extends Serializable, B extends Serializable> {

        private A first;

        private B second;
    }
}
