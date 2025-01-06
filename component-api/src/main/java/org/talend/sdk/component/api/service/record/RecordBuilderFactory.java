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
package org.talend.sdk.component.api.service.record;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;

/**
 * Entry point to create records (through builders).
 */
public interface RecordBuilderFactory extends Serializable {

    /**
     * Enables to build a record from another one. It is typically useful to add a column and passthrough others.
     *
     * @param schema the schema if the output record.
     * @param record the record to take as input.
     * @return a new builder initialized with the input record for all matching entries (by name).
     */
    Record.Builder newRecordBuilder(Schema schema, Record record);

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
     * Build a schema from another one. Typically useful to add a column and letting others passthrough.
     * 
     * @param schema the input schema.
     * @return a new schema builder intialized with the input schema.
     */
    Schema.Builder newSchemaBuilder(Schema schema);

    /**
     * @return a builder to create a schema entry.
     */
    Schema.Entry.Builder newEntryBuilder();

    /**
     * Build a schema.entry from another one. Useful to duplicate a column with some changes.
     * 
     * @param model : model of entry to copy.
     * @return entry builder with model parameters.
     */
    default Schema.Entry.Builder newEntryBuilder(Schema.Entry model) {
        final Map<String, String> props = new HashMap<>();
        final Map<String, String> modelProps = model.getProps();
        if (modelProps != null) {
            props.putAll(modelProps);
        }
        return this
                .newEntryBuilder()
                .withType(model.getType())
                .withNullable(model.isNullable())
                .withName(model.getName())
                .withElementSchema(model.getElementSchema())
                .withDefaultValue(model.getDefaultValue())
                .withComment(model.getComment())
                .withProps(props);
    }
}
