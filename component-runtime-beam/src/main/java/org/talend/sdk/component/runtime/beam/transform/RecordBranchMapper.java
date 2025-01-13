/**
 * Copyright (C) 2006-2025 Talend Inc. - www.talend.com
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
import org.talend.sdk.component.runtime.beam.spi.record.RecordCollectors;
import org.talend.sdk.component.runtime.beam.transform.service.ServiceLookup;
import org.talend.sdk.component.runtime.manager.ComponentManager;

/**
 * Redirects a branch on another branch without modifying the other ones.
 */
public class RecordBranchMapper extends DoFn<Record, Record> {

    private RecordBuilderFactory factory;

    private String sourceBranch;

    private String targetBranch;

    public RecordBranchMapper(final RecordBuilderFactory factory, final String sourceBranch,
            final String targetBranch) {
        this.factory = factory;
        this.sourceBranch = sanitizeConnectionName(sourceBranch);
        this.targetBranch = sanitizeConnectionName(targetBranch);
    }

    protected RecordBranchMapper() {
        // no-op
    }

    @ProcessElement
    public void onElement(final ProcessContext context) {
        final Record aggregate = context.element();
        final Collection<Record> branch = aggregate.getArray(Record.class, sourceBranch);
        if (branch != null) {
            final Record output = aggregate.getSchema().getAllEntries().collect(factory::newRecordBuilder, (a, e) -> {
                final boolean remappedBranch = e.getName().equals(sourceBranch);
                final String branchName = remappedBranch ? targetBranch : e.getName();
                a
                        .withArray(
                                factory
                                        .newEntryBuilder()
                                        .withName(branchName)
                                        .withType(Schema.Type.ARRAY)
                                        .withElementSchema(e.getElementSchema())
                                        .build(),
                                remappedBranch ? branch : aggregate.getArray(Record.class, e.getName()));
            }, RecordCollectors::merge).build();
            context.output(output);
        } else {
            context.output(aggregate);
        }
    }

    public static PTransform<PCollection<Record>, PCollection<Record>> of(final String plugin, final String fromBranch,
            final String toBranch) {
        final RecordBuilderFactory lookup =
                ServiceLookup.lookup(ComponentManager.instance(), plugin, RecordBuilderFactory.class);
        return new RecordParDoTransformCoderProvider<>(SchemaRegistryCoder.of(),
                new RecordBranchMapper(lookup, fromBranch, toBranch));
    }
}
