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
package org.talend.sdk.component.sample.feature.supporterror;

import java.io.Serializable;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.runtime.record.RecordImpl;
import org.talend.sdk.component.runtime.record.SchemaImpl;

import lombok.Data;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Documentation("Support Error Input sample processor connector.")
@Emitter(family = "supporterror", name = "supportErrorInput")
public class SupportErrorInput implements Serializable {

//    private final transient JsonBuilderFactory factory;

    private transient Schema recordSchema;

    private final transient InputConfig configuration;

    public SupportErrorInput(@Option("configuration") final InputConfig config) {
        this.configuration = config;
    }

    @PostConstruct
    public void init() {
        recordSchema = new SchemaImpl.BuilderImpl()
                .withType(Schema.Type.RECORD)
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("name")
                        .withNullable(false)
                        .withType(Schema.Type.STRING)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("date")
                        .withNullable(false)
                        .withType(Schema.Type.DATETIME)
                        .build())
                .withEntry(new SchemaImpl.EntryImpl.BuilderImpl()
                        .withName("age")
                        .withNullable(false)
                        .withType(Schema.Type.INT)
                        .build())
                .build();
    }

    @Producer
    public Record data() {
        final RecordImpl.BuilderImpl builder = new RecordImpl.BuilderImpl(recordSchema);
        final Record record = builder.withString("name", "example connector")
                .withError("date", null, "date is null", null)
                .withError("age", "string", "wrong int value", null)
                .build();

        return record;
    }

    @Data
    @GridLayout(value = {
            @GridLayout.Row("dataset"),
    })
     @Version
    public static class InputConfig {

        @Option
        @Documentation("Dataset.")
        private Dataset dataset = new Dataset();
    }

    @DataSet
    @Data
    @GridLayout(value = { @GridLayout.Row("datastore") })
    public static class Dataset implements Serializable {

        @Option
        @Documentation("Datastore.")
        private Datastore datastore = new Datastore();

    }

    @DataStore
    @Data
    @GridLayout(value = { @GridLayout.Row("name"), @GridLayout.Row("age"),
            @GridLayout.Row("date")})
    public static class Datastore implements Serializable {

        @Option
        @Documentation("Name prop.")
        private String name = "test";

        @Option
        @Documentation("Age prop.")
        private int age = 0;

        @Option
        @Documentation("Date prop.")
        private Date date;
    }

}
