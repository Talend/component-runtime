/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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

import static org.talend.sdk.component.api.record.Schema.sanitizeConnectionName;

import java.util.Collection;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.PCollection;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.beam.coder.registry.SchemaRegistryCoder;
import org.talend.sdk.component.runtime.beam.transform.service.ServiceLookup;
import org.talend.sdk.component.runtime.manager.ComponentManager;

/**
 * Filters a record by branch, output is a record containing only the selected branch
 * or no record is emitted if the branch is missing.
 */
public class RecordBranchFilter extends DoFn<Record, Record> {

    private RecordBuilderFactory factory;

    private String branch;

    public RecordBranchFilter(final RecordBuilderFactory factory, final String branch) {
        this.factory = factory;
        this.branch = sanitizeConnectionName(branch);
    }

    protected RecordBranchFilter() {
        // no-op
    }

    @ProcessElement
    public void onElement(final ProcessContext context) {
        final Record aggregate = context.element();
        final Collection<Record> branchValue = aggregate.getArray(Record.class, branch);
        if (branchValue != null) {
            final Schema.Entry entry = aggregate.getSchema().getEntry(branch);
            context.output(factory.newRecordBuilder().withArray(entry, branchValue).build());
        }
    }

    public static PTransform<PCollection<Record>, PCollection<Record>> of(final String plugin,
            final String branchSelector) {
        final RecordBuilderFactory lookup =
                ServiceLookup.lookup(ComponentManager.instance(), plugin, RecordBuilderFactory.class);
        return new RecordParDoTransformCoderProvider<>(SchemaRegistryCoder.of(),
                new RecordBranchFilter(lookup, branchSelector));
    }
}
