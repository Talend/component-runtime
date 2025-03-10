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

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.annotation.JsonbProperty;

import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.checkpoint.Checkpoint;
import org.talend.sdk.component.api.input.checkpoint.CheckpointAvailable;
import org.talend.sdk.component.api.input.checkpoint.CheckpointData;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Emitter(family = "checkpoint", name = "list-input")
public class CheckpointInput implements Serializable {

    private final JsonBuilderFactory factory;

    private final Jsonb jsonb;

    private List<Integer> data;

    private transient ListIterator<Integer> iterator;

    private Integer bookmark;

    private boolean newBookmark;

    private final InputConfig configuration;

    public CheckpointInput(final JsonBuilderFactory factory, final Jsonb jsonb,
            @Option("configuration") final InputConfig config) {
        this.factory = factory;
        this.jsonb = jsonb;
        this.configuration = config;
        log.warn("[CheckpointInput] config: {}.", config);
    }

    @PostConstruct
    public void init() {
        data = IntStream.range(0, 10).boxed().collect(toList());
        if (configuration.checkpoint == null) {
            log.info("[resume] No valid checkpoint configuration found, using start of dataset.");
            bookmark = 0;
        } else {
            if ("finished".equals(configuration.checkpoint.status)) {
                bookmark = 0;
            } else {
                bookmark = configuration.checkpoint.sinceId;
            }
        }
        log.warn("[resume] resuming at: {} data: {}.", bookmark, data);
        if (bookmark == null) {
            iterator = new ArrayList<Integer>().listIterator();
        } else {
            iterator = data.listIterator(bookmark);
        }
    }

    @Producer
    public JsonObject data() {
        if (iterator == null) {
            iterator = data.listIterator();
        }
        final Integer produced = iterator.hasNext() ? iterator.next() : null;
        if (produced == null) {
            configuration.checkpoint.status = "finished";
            configuration.checkpoint.sinceId = bookmark;
            return null;
        }
        bookmark = produced;
        configuration.checkpoint.sinceId = produced;
        configuration.checkpoint.status = "running";

        if (bookmark % 2 == 0) {
            newBookmark = true;
        }

        return factory.createObjectBuilder().add("data", produced).build();
    }

    @CheckpointData
    public JsonObject getCheckpoint() {
        newBookmark = false;
        return jsonb.fromJson(jsonb.toJson(configuration.checkpoint), JsonObject.class);
    }

    @CheckpointAvailable
    public Boolean isCheckpointReady() {
        return newBookmark;
    }

    @Data
    @GridLayout(names = GridLayout.FormType.CHECKPOINT, value = {
            @GridLayout.Row("bookmarks"),
    })
    @Checkpoint
    public static class CheckPointInputConfig implements Serializable {

        public enum Strategy {
            BY_ID,
            BY_DATE
        }

        @Option
        @Documentation("Check point mode.")
        private String stream;

        @Option
        @DefaultValue("BY_ID")
        private Strategy strategy = Strategy.BY_ID;

        @Option
        @JsonbProperty("start_date")
        private String startDate;

        @Option
        @JsonbProperty("since_id")
        private int sinceId;

        @Option
        private String status;
    }

    @Data
    @GridLayout(value = {
            @GridLayout.Row("user"),
            @GridLayout.Row("pass"),
    })
    @GridLayout(names = GridLayout.FormType.CHECKPOINT, value = {
            @GridLayout.Row("checkPointInputConfig"),
    })
    public static class InputConfig {

        @Option
        private String user;

        @Option
        private String pass;

        @Option
        private CheckPointInputConfig checkpoint = new CheckPointInputConfig();
    }
}
