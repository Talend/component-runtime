/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.sample.feature.checkpoint;

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.MigrationHandler;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.configuration.ui.DefaultValue;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.checkpoint.Checkpoint;
import org.talend.sdk.component.api.input.checkpoint.CheckpointAvailable;
import org.talend.sdk.component.api.input.checkpoint.CheckpointData;
import org.talend.sdk.component.api.meta.Documentation;

import lombok.Data;

@Version
@Icon(value = Icon.IconType.CUSTOM, custom = "icon")
@Documentation("Checkpoint Input sample processor connector.")
@Emitter(family = "checkpoint", name = "incrementalSequenceInput")
public class IncrementalSequenceInput implements Serializable {

    private final transient JsonBuilderFactory factory;

    private List<Integer> data;

    private transient ListIterator<Integer> iterator;

    private Integer bookmark;

    private boolean newBookmark;

    private final transient InputConfig configuration;

    public IncrementalSequenceInput(final JsonBuilderFactory factory,
            @Option("configuration") final InputConfig config) {
        this.factory = factory;
        this.configuration = config;
    }

    @PostConstruct
    public void init() {
        data = IntStream.range(0, configuration.dataset.maxRecords).boxed().collect(toList());
        if (configuration.checkpoint != null) {
            bookmark = configuration.checkpoint.sinceId;
        }

        int start = bookmark + 1; // +1 since we want to start after the bookmark.
        if (start > configuration.dataset.maxRecords) {
            start = configuration.dataset.maxRecords;
        }

        if (bookmark == null) {
            iterator = new ArrayList<Integer>().listIterator();
        } else {
            iterator = data.listIterator(start);
        }
    }

    @Producer
    public JsonObject data() {
        if (iterator == null) {
            iterator = data.listIterator();
        }
        final Integer produced = iterator.hasNext() ? iterator.next() : null;

        if (produced == null) {
            return null;
        }

        configuration.checkpoint.sinceId = produced;
        bookmark = produced;

        if (bookmark % 2 == 0) {
            newBookmark = true;
        }

        return factory.createObjectBuilder().add("data", configuration.recordPrefix + produced).build();
    }

    @CheckpointData
    public CheckPointConfig getCheckpoint() {
        newBookmark = false;
        return configuration.checkpoint;
    }

    @CheckpointAvailable
    public Boolean isCheckpointReady() {
        return newBookmark;
    }

    @Data
    @GridLayout(value = {
            @GridLayout.Row("recordPrefix"),
            @GridLayout.Row("dataset"),
    })
    @GridLayout(names = GridLayout.FormType.CHECKPOINT, value = { @GridLayout.Row("checkpoint") })
    @Version
    public static class InputConfig {

        @Option
        @Documentation("Record prefix message.")
        private String recordPrefix = "";

        @Option
        @Documentation("Dataset.")
        private Dataset dataset = new Dataset();

        @Option
        @Documentation("Checkpointing configuration.")
        private CheckPointConfig checkpoint = new CheckPointConfig();
    }

    @DataSet
    @Data
    @GridLayout(value = { @GridLayout.Row("maxRecords"), @GridLayout.Row("datastore") })
    public static class Dataset implements Serializable {

        @Option
        @Documentation("Datastore.")
        private Datastore datastore = new Datastore();

        @Option
        @DefaultValue("20")
        @Documentation("Max records in dataset.")
        private int maxRecords = 20;
    }

    @DataStore
    @Data
    @GridLayout(value = { @GridLayout.Row("systemId") })
    public static class Datastore implements Serializable {

        @Option
        @Documentation("Useless datastore prop.")
        private String systemId = "unknown";
    }

    @Data
    @Checkpoint
    @Version(value = 2, migrationHandler = CheckpointMigrationHandler.class)
    public static class CheckPointConfig implements Serializable {

        @Option
        @Documentation("Checkpointing state : since id.")
        private int sinceId = -1;
    }

    public static class CheckpointMigrationHandler implements MigrationHandler {

        public static final String OLD_KEY = "lastId";

        /**
         * @param incomingVersion the version of associatedData values.
         * @param incomingData the data sent from the caller. Keys are using the path of the property as in component
         * metadata.
         * @return
         */
        @Override
        public Map<String, String> migrate(final int incomingVersion, final Map<String, String> incomingData) {
            if (incomingVersion < 2 && incomingData.containsKey(OLD_KEY)) {
                incomingData.put("sinceId", incomingData.get(OLD_KEY));
                incomingData.remove(OLD_KEY);
            }
            return incomingData;
        }
    }
}
