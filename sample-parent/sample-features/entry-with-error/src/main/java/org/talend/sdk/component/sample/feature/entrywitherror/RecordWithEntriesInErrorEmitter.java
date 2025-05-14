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
package org.talend.sdk.component.sample.feature.entrywitherror;

import java.io.Serializable;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.record.Record.Builder;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.record.Schema.Entry;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.Data;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "mapper")
@Emitter(name = "RecordWithEntriesInErrorEmitter")
@Documentation("Generated record with entries in error.")
public class RecordWithEntriesInErrorEmitter implements Serializable {

    private final RecordBuilderFactory recordBuilderFactory;

    private final Config config;

    private transient Schema recordSchema;

    private transient Function<Integer, Record> createRecordFunction;

    private transient int index;

    public RecordWithEntriesInErrorEmitter(
            final RecordBuilderFactory recordBuilderFactory,
            final @Option("configuration") Config config) {
        this.recordBuilderFactory = recordBuilderFactory;
        this.config = config;
    }

    @PostConstruct
    public void init() {
        recordSchema = recordBuilderFactory.newSchemaBuilder(Schema.Type.RECORD)
                .withEntry(recordBuilderFactory.newEntryBuilder()
                        .withName("name")
                        .withNullable(false)
                        .withErrorCapable(true)
                        .withType(Schema.Type.STRING)
                        .build())
                .withEntry(recordBuilderFactory.newEntryBuilder()
                        .withName("date")
                        .withNullable(false)
                        .withErrorCapable(true)
                        .withType(Schema.Type.DATETIME)
                        .build())
                .withEntry(recordBuilderFactory.newEntryBuilder()
                        .withName("age")
                        .withNullable(false)
                        .withErrorCapable(true)
                        .withType(Schema.Type.INT)
                        .build())
                .build();

        createRecordFunction = i -> {
            Builder builder = recordBuilderFactory.newRecordBuilder(recordSchema).withString("name", "name " + i);

            boolean generateErrors = config.getHowManyErrors() >= i;

            if (generateErrors) {
                Entry dateEntry = recordSchema.getEntry("date");
                builder.with(dateEntry, "789-555");
            } else {
                ZonedDateTime dateTime = ZonedDateTime.of(
                        2025, // Year
                        Month.APRIL.getValue(), // Month
                        1 + i, // Day
                        15, // Hours
                        30, // Minutes
                        0, // seconds
                        0, // nanoseconds
                        ZoneId.of("UTC") // Timezone
                );
                builder.withDateTime("date", dateTime);
            }

            if (generateErrors) {
                Entry ageEntry = recordSchema.getEntry("age");
                builder.with(ageEntry, "-78");
            } else {
                builder.withInt("age", 50 + i);
            }

            return builder.build();
        };
    }

    @Producer
    public Record data() {
        index++;
        if (index <= config.getNbRecords()) {
            return createRecordFunction.apply(index);
        }

        return null;
    }

    @Data
    @GridLayout(value = {
            @GridLayout.Row("howManyErrors"),
            @GridLayout.Row("nbRecords"),
    })
    public static class Config implements Serializable {

        @Option
        @Documentation("The number of errors to generate..")
        private int howManyErrors;

        @Option
        @Documentation("Number of generated records.")
        private int nbRecords = 5;

    }

}
