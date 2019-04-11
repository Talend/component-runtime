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
package org.talend.sdk.component.api.service.record;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

/**
 * Entry point to create records (through builders).
 */
public interface RecordBuilderFactory {

    /**
     * @param schema the schema of the record to be built
     * @return a builder to create a new record and enforce the built record to respect
     * a static schema. If the entries don't match the schema the build call will fail.
     */
    Record.Builder newRecordBuilder(Schema schema);

    /**
     * @return a builder to create a new record.
     */
    Record.Builder newRecordBuilder();

    /**
     * @param type the schema type.
     * @return a builder to create a schema.
     */
    Schema.Builder newSchemaBuilder(Schema.Type type);

    /**
     * @return a builder to create a schema entry.
     */
    Schema.Entry.Builder newEntryBuilder();
}
