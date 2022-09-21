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
package org.talend.sdk.component.runtime.tdp;

import java.util.HashMap;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

public class TdpRecordBuilderFactory implements RecordBuilderFactory {

    @Override
    public Record.Builder newRecordBuilder(final Schema schema, final Record record) {
        return TdpRecord.builder(schema, record);
    }

    @Override
    public Record.Builder newRecordBuilder(final Schema schema) {
        return TdpRecord.builder(schema);
    }

    @Override
    public Record.Builder newRecordBuilder() {
        return new TdpRecord.Builder();
    }

    @Override
    public Schema.Builder newSchemaBuilder(final Schema.Type type) {
        return TdpSchema.builder()
                .withType(type);
    }

    @Override
    public Schema.Builder newSchemaBuilder(final Schema schema) {
        Schema.Builder builder = TdpSchema.builder()
                .withType(schema.getType())
                .withProps(new HashMap<>(schema.getProps()));

        schema.getEntries().forEach(builder::withEntry);

        return builder;
    }

    @Override
    public Schema.Entry.Builder newEntryBuilder() {
        return TdpEntry.builder();
    }

    @Override
    public Schema.Entry.Builder newEntryBuilder(final Schema.Entry entry) {
        return entry.toBuilder();
    }
}
