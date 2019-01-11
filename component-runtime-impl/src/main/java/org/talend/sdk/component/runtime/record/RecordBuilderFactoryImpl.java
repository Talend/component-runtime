/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.record;

import java.io.ObjectStreamException;
import java.io.Serializable;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.Data;

@Data
public class RecordBuilderFactoryImpl implements RecordBuilderFactory, Serializable {

    private final String plugin;

    @Override
    public Schema.Builder newSchemaBuilder(final Schema.Type type) {
        switch (type) {
        case RECORD:
        case ARRAY:
            return new SchemaImpl.BuilderImpl().withType(type);
        default:
            return Schemas.valueOf(type.name());
        }
    }

    @Override
    public Record.Builder newRecordBuilder(final Schema schema) {
        return new RecordImpl.BuilderImpl(schema);
    }

    @Override
    public Record.Builder newRecordBuilder() {
        return new RecordImpl.BuilderImpl();
    }

    @Override
    public Schema.Entry.Builder newEntryBuilder() {
        return new SchemaImpl.EntryImpl.BuilderImpl();
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, RecordBuilderFactory.class.getName());
    }
}
