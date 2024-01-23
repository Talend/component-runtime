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
package org.talend.sdk.component.runtime.manager.service.record;

import java.io.Serializable;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FakeRecordBuilderFactory implements RecordBuilderFactory, Serializable {

    private final String plugin;

    @Override
    public Builder newRecordBuilder(final Schema schema, final Record record) {
        throw new UnsupportedOperationException("#newRecordBuilder()");
    }

    @Override
    public Builder newRecordBuilder(final Schema schema) {
        throw new UnsupportedOperationException("#newRecordBuilder()");
    }

    @Override
    public Builder newRecordBuilder() {
        throw new UnsupportedOperationException("#newRecordBuilder()");
    }

    @Override
    public Schema.Builder newSchemaBuilder(final Type type) {
        throw new UnsupportedOperationException("#newSchemaBuilder()");
    }

    @Override
    public Schema.Builder newSchemaBuilder(final Schema schema) {
        throw new UnsupportedOperationException("#newSchemaBuilder()");
    }

    @Override
    public Entry.Builder newEntryBuilder() {
        throw new UnsupportedOperationException("#newEntryBuilder()");
    }

    @Override
    public Entry.Builder newEntryBuilder(final Entry model) {
        return RecordBuilderFactory.super.newEntryBuilder(model);
    }
}
