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
package org.talend.sdk.component.sample.feature.databasemapping.service;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.schema.DatabaseMapping;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.sample.feature.databasemapping.config.Config;
import org.talend.sdk.component.sample.feature.databasemapping.config.Dataset;

@Service
public class UIService {

    public static final String SECOND_FLOW_NAME = "second";

    public static final String THIRD_FLOW_NAME = "third";

    @Service
    private RecordBuilderFactory factory;

    @DatabaseMapping
    public String getDatabaseMapping(@Option("configuration") final Config configuration) {
        return configuration.getDse().getDso().getBackend().getKey();
    }

    @DiscoverSchema("dse")
    public Schema guessSchema(final Dataset dse) {
        return factory.newSchemaBuilder(Schema.Type.RECORD)
                .withEntry(factory.newEntryBuilder().withName("id").withType(Schema.Type.INT).build())
                .withEntry(factory.newEntryBuilder().withName("input").withType(Schema.Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName("another").withType(Schema.Type.STRING).build())
                .withEntry(factory.newEntryBuilder().withName("aBoolean").withType(Schema.Type.BOOLEAN).build())
                .build();
    }

}
