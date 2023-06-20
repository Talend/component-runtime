/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.test.connectors.service;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Builder;
import org.talend.sdk.component.api.record.Schema.Type;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended;
import org.talend.sdk.component.test.connectors.config.NestedConfig;
import org.talend.sdk.component.test.connectors.config.TheDataset;

@Service
public class SchemaServices {

    /**
     * In this service sample class we will implement every existing particular actions related to schema.
     *
     * Services actions are listed here: https://talend.github.io/component-runtime/main/latest/services-actions.html
     *
     * Implemented:
     * - DiscoverSchema https://talend.github.io/component-runtime/main/latest/ref-actions.html#_schema
     * - DiscoverSchemaExtended https://talend.github.io/component-runtime/main/latest/ref-actions.html#_schemaExtended
     *
     */

    public final static String DISCOVER_SCHEMA_STATIC = "action_DISCOVER_SCHEMA_static";

    public final static String DISCOVER_SCHEMA_DYNAMIC = "action_DISCOVER_SCHEMA_dynamic";

    public final static String DISCOVER_SCHEMA_EXTENDED_STATIC = "action_DISCOVER_SCHEMA_EXT_static";

    public final static String DISCOVER_SCHEMA_EXTENDED_MINIMAL = "action_DISCOVER_SCHEMA_EXT_minimal";

    public final static String DISCOVER_SCHEMA_EXTENDED_SCHEMA = "action_DISCOVER_SCHEMA_EXT_schema";

    public final static String DISCOVER_SCHEMA_EXTENDED_BRANCH = "action_DISCOVER_SCHEMA_EXT_branch";

    public final static String DISCOVER_SCHEMA_EXTENDED_FULL = "action_DISCOVER_SCHEMA_EXT_full";

    /**
     * Schema needed elements
     */
    @Service
    private RecordBuilderFactory recordFactory;

    /**
     * Schema action
     * Methode to test DiscoverSchema with a static fix response
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/ref-actions.html#_schema
     * Type: schema
     * API: @org.talend.sdk.component.api.service.schema.DiscoverSchema
     *
     * Returned type: org.talend.sdk.component.api.record.Schema
     *
     */
    @DiscoverSchema(DISCOVER_SCHEMA_STATIC)
    public Schema discoverSchemaStatic(@Option final TheDataset dataset) {
        final Builder schemaBuilder = recordFactory.newSchemaBuilder(Type.RECORD);

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_string")
                .withType(Type.STRING)
                .build());

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_bytes")
                .withType(Type.BYTES)
                .build());

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_int")
                .withType(Type.INT)
                .build());

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_long")
                .withType(Type.LONG)
                .build());

        return schemaBuilder.build();
    }

    /**
     * Schema action
     * Methode to test DiscoverSchema with a variable response from dataset element
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/ref-actions.html#_schema
     * Type: schema
     * API: @org.talend.sdk.component.api.service.schema.DiscoverSchema
     *
     * Returned type: org.talend.sdk.component.api.record.Schema
     *
     */
    @DiscoverSchema(DISCOVER_SCHEMA_DYNAMIC)
    public Schema discoverSchemaDynamic(@Option final TheDataset dataset) {
        final Builder schemaBuilder = recordFactory.newSchemaBuilder(Type.RECORD);

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("dynamic_entry_".concat(dataset.DATASET_INFO))
                .withType(Type.STRING)
                .build());

        return schemaBuilder.build();
    }

    /**
     * Schema Extended action
     * Test DiscoverSchemaExtended with a static fix response
     *
     * Documentation: https://talend.github.io/component-runtime/main/latest/ref-actions.html#_schema_extended
     * Type: schema_extended
     * API: @org.talend.sdk.component.api.service.schema.DiscoverSchemaExtended
     *
     * Returned type: org.talend.sdk.component.api.record.Schema
     *
     */
    @DiscoverSchemaExtended(DISCOVER_SCHEMA_EXTENDED_STATIC)
    public Schema discoverSchemaExtendedStatic(final @Option("configuration") NestedConfig unused) {
        final Builder schemaBuilder = recordFactory.newSchemaBuilder(Type.RECORD);

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_float")
                .withType(Type.FLOAT)
                .build());

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_double")
                .withType(Type.DOUBLE)
                .build());

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_boolean")
                .withType(Type.BOOLEAN)
                .build());

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_datetime")
                .withType(Type.DATETIME)
                .build());

        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName("entry_decimal")
                .withType(Type.DECIMAL)
                .build());

        return schemaBuilder.build();
    }

    /**
     * Schema Extended action
     * Methode to test DiscoverSchemaExtended with a variable response depending on parameters value.
     * Multiples methods using all parameters capabilities.
     *
     * discoverSchemaExtendedFull: all possibles parameters (option + schema + branch)
     * discoverSchemaExtendedSch: One option and Schema
     * discoverSchemaExtendedBranch: One option and a branch
     * discoverSchemaExtendedMinimal: only one @option
     *
     * Returned type: org.talend.sdk.component.api.record.Schema
     *
     */

    @DiscoverSchemaExtended(DISCOVER_SCHEMA_EXTENDED_MINIMAL)
    public Schema discoverSchemaExtendedMinimal(final @Option("configurationMinimal") NestedConfig incomingConfig) {
        final Builder schemaBuilder = recordFactory.newSchemaBuilder(Type.RECORD);
        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName(incomingConfig.getStringOption1())
                .withType(Type.STRING)
                .build());
        return schemaBuilder.build();
    }

    @DiscoverSchemaExtended(DISCOVER_SCHEMA_EXTENDED_SCHEMA)
    public Schema discoverSchemaExtendedSch(final @Option("configurationSchema") NestedConfig incomingConfig,
            final Schema incomingSchema) {

        final Builder schemaBuilder = recordFactory.newSchemaBuilder(incomingSchema);
        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName(incomingConfig.getStringOption1())
                .withType(Type.STRING)
                .build());
        return schemaBuilder.build();
    }

    @DiscoverSchemaExtended(DISCOVER_SCHEMA_EXTENDED_BRANCH)
    public Schema discoverSchemaExtendedBranch(final @Option("configurationBranch") NestedConfig incomingConfig,
            final String branch) {

        final Builder schemaBuilder = recordFactory.newSchemaBuilder(Type.RECORD);
        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName(incomingConfig.getStringOption1())
                .withType(Type.STRING)
                .build());
        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName(branch)
                .withType(Type.STRING)
                .build());
        return schemaBuilder.build();
    }

    @DiscoverSchemaExtended(DISCOVER_SCHEMA_EXTENDED_FULL)
    public Schema discoverSchemaExtendedFull(final @Option("configurationFull") NestedConfig incomingConfig,
            final Schema incomingSchema,
            final String branch) {

        final Builder schemaBuilder = recordFactory.newSchemaBuilder(incomingSchema);
        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName(incomingConfig.getStringOption1())
                .withType(Type.STRING)
                .build());
        schemaBuilder.withEntry(recordFactory.newEntryBuilder()
                .withName(branch)
                .withType(Type.STRING)
                .build());
        return schemaBuilder.build();
    }
}
