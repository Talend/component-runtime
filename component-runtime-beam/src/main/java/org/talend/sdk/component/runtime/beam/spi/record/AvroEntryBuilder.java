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
package org.talend.sdk.component.runtime.beam.spi.record;

import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.SchemaImpl;

public class AvroEntryBuilder extends SchemaImpl.EntryImpl.BuilderImpl {

    @Override
    public Schema.Entry.Builder withElementSchema(final Schema schema) {
        if (schema instanceof AvroSchema) {
            final AvroSchema innerSchema = (AvroSchema) schema;
            AvroSchema avroSchema = this.authorizeNull(innerSchema);
            return super.withElementSchema(avroSchema);
        }
        return super.withElementSchema(schema);
    }

    private AvroSchema authorizeNull(final AvroSchema innerSchema) {
        org.apache.avro.Schema delegate = innerSchema.getDelegate();
        if (delegate.getType() != org.apache.avro.Schema.Type.UNION) {
            org.apache.avro.Schema union = org.apache.avro.Schema.createUnion(delegate,
                    org.apache.avro.Schema.create(org.apache.avro.Schema.Type.NULL));
            return new AvroSchema(union);
        }
        return innerSchema;
    }
}
