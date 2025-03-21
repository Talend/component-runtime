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
package org.talend.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.checkpoint.Checkpoint;
import org.talend.sdk.component.api.input.checkpoint.CheckpointAvailable;
import org.talend.sdk.component.api.input.checkpoint.CheckpointData;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ResumeableInput implements Serializable {

    private final Jsonb jsonb;

    private final RecordBuilderFactory factory;

    private transient ListIterator<Record> iterator;

    private final ResumeableConfiguration configuration;

    public ResumeableInput(final Jsonb jsonb, final RecordBuilderFactory factory,
            @Option("configuration") final ResumeableConfiguration configuration) {
        this.jsonb = jsonb;
        this.factory = factory;
        this.configuration = configuration;
    }

    @PostConstruct
    public void init() throws IOException {
        log.info("[init] configuration = {}", configuration);
        final List<Record> records = new ArrayList<>();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        //
        try (BufferedReader br = new BufferedReader(new FileReader(configuration.getResourcePath()))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip the header line
                }
                String[] values = line.split(",");
                final int id = Integer.parseInt(values[0]);
                final String name = values[1];
                final Date updatedAt = dateFormat.parse(values[2]);
                if (checkResume(id, updatedAt)) {
                    records.add(factory.newRecordBuilder()
                            .withInt("id", id)
                            .withString("name", name)
                            .withDateTime("updatedAt", updatedAt)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("[init]", e);
        }
        iterator = records.listIterator();
    }

    private boolean checkResume(int id, Date date) {
        if (configuration.checkpoint.strategy == ResumeableCheckPointConfiguration.Strategy.BY_ID) {
            return id > configuration.checkpoint.lastId;
        } else {
            return date.after(Date.from(new Date(configuration.checkpoint.lastUpdate).toInstant()));
        }
    }

    @Producer
    public Record next() {
        final Record record = iterator.hasNext() ? iterator.next() : null;
        if (record == null) {
            configuration.checkpoint.status = "finished";
            return null;
        }
        configuration.checkpoint.status = "running";
        configuration.checkpoint.lastId = record.getInt("id");
        configuration.checkpoint.lastUpdate = record.getDateTime("updatedAt")
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return record;
    }

    @CheckpointAvailable
    public boolean available() {
        // checkpoint is made on each iteration.
        return true;
    }

    @CheckpointData
    public Object checkpoint() {
        return configuration.checkpoint;
    }

    @Data
    @GridLayout(value = {
            @GridLayout.Row("dataset"),
            @GridLayout.Row("resourcePath"),
    })
    @GridLayout(names = GridLayout.FormType.CHECKPOINT, value = {
            @GridLayout.Row("checkpoint"),
    })
    public static class ResumeableConfiguration implements Serializable {

        @Option
        private ResumeableDataset dataset;

        @Option
        private String resourcePath;

        @Option
        private ResumeableCheckPointConfiguration checkpoint = new ResumeableCheckPointConfiguration();
    }

    @Data
    @DataSet("dataset")
    @GridLayout(names = GridLayout.FormType.CHECKPOINT, value = {
            @GridLayout.Row("strategy"),
            @GridLayout.Row("lastId") })
    public static class ResumeableDataset implements Serializable {

        @Option
        private String systemUrl;

        @Option
        private String configUrl;
    }

    static final int CHECKPOINT_VERSION = 2;

    @Data
    @GridLayout(names = GridLayout.FormType.CHECKPOINT, value = {
            @GridLayout.Row("strategy"),
            @GridLayout.Row("lastId"),
            @GridLayout.Row("lastUpdate"),
            @GridLayout.Row("status"),
    })
    @Version(value = CHECKPOINT_VERSION, migrationHandler = CheckpointMigrationHandler.class)
    @Checkpoint
    public static class ResumeableCheckPointConfiguration implements Serializable {

        public enum Strategy {
            BY_ID,
            BY_DATE
        }

        @Option
        @DefaultValue("BY_ID")
        private Strategy strategy = Strategy.BY_ID;

        @Option
        private int lastId;

        @Option
        private String lastUpdate;

        @Option
        private String status;

    }

    public static class CheckpointMigrationHandler implements MigrationHandler {

        @Override
        public Map<String, String> migrate(int incomingVersion, Map<String, String> incomingData) {
            log.info("[checkpoint#migrate] currrentVersion = {}, incomingVersion = {}, incomingData = {}",
                    CHECKPOINT_VERSION, incomingVersion, incomingData);
            if (incomingVersion < 2) {
                if (incomingData.containsKey("sinceId")) {
                    incomingData.put("lastId", incomingData.get("sinceId"));
                    incomingData.remove("sinceId");
                }
            }
            incomingData.entrySet().forEach(e -> log.info("[migrate] {}={}.", e.getKey(), e.getValue()));
            return incomingData;
        }
    }

}
