/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.record.Record;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
class RecordParDoTransformCoderProvider<T> extends PTransform<PCollection<Record>, PCollection<T>> {

    private Coder<T> coder;

    private DoFn<Record, T> fn;

    @Override
    public PCollection<T> expand(final PCollection<Record> input) {
        return input.apply(ParDo.of(fn)).setCoder(coder);
    }
}
