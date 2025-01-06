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

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.RecordImpl;

// simple impl converting at the last moment the record to an avro one
// can be enhanced later to do it on the fly
public class AvroRecordBuilder extends RecordImpl.BuilderImpl {

    public AvroRecordBuilder() {
        // no-op
    }

    public AvroRecordBuilder(final Schema providedSchema) {
        super(providedSchema);
    }

    @Override
    public Record build() {
        return new AvroRecord(super.build());
    }
}
