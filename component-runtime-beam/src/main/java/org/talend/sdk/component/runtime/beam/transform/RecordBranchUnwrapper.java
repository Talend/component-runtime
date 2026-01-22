/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import static org.talend.sdk.component.api.record.SchemaCompanionUtil.sanitizeName;

import java.util.Collection;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;

/**
 * Extract the value of a branch if exists (unwrap).
 */
public class RecordBranchUnwrapper extends DoFn<Record, Record> {

    private String branch;

    public RecordBranchUnwrapper(final String branch) {
        this.branch = sanitizeName(branch);
    }

    protected RecordBranchUnwrapper() {
        // no-op
    }

    @ProcessElement
    public void onElement(final ProcessContext context) {
        final Record aggregate = context.element();
        final Collection<Record> array = aggregate.getArray(Record.class, branch);
        if (array != null) {
            array.forEach(context::output);
        }
    }

    // keep plugin here, this is how we would lookup services if needed
    public static PTransform<PCollection<Record>, PCollection<Record>> of(final String plugin,
            final String branchSelector) {
        return new RecordParDoTransformCoderProvider<>(SchemaRegistryCoder.of(),
                new RecordBranchUnwrapper(branchSelector));
    }
}
