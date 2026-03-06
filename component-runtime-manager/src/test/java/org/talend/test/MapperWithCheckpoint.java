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
package org.talend.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.json.bind.annotation.JsonbProperty;

import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.configuration.ui.layout.GridLayout;
import org.talend.sdk.component.api.input.Assessor;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.input.PartitionSize;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.Split;
import org.talend.sdk.component.api.input.checkpoint.Checkpoint;
import org.talend.sdk.component.api.input.checkpoint.CheckpointData;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.record.Record;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import lombok.Data;
import lombok.NoArgsConstructor;

@Version(1)
@PartitionMapper(family = "checkpoint", name = "mapper-with-checkpoint")
@Documentation("A TCK Mapper that returns several emitters and that support checkpoint.")
public class MapperWithCheckpoint implements Serializable {

    private final MapperWithCheckpointConfig config;

    private final RecordBuilderFactory recordBuilderFactory;

    private int partition = -1;

    public MapperWithCheckpoint(final @Option("configuration") MapperWithCheckpointConfig config,
            final RecordBuilderFactory recordBuilderFactory) {
        this.config = config;
        this.recordBuilderFactory = recordBuilderFactory;
    }

    private void setPartition(int p) {
        this.partition = p;
    }

    @Assessor
    public long estimateSize() {
        return 1L;
    }

    @Split
    public List<MapperWithCheckpoint> split(@PartitionSize final long bundles) {
        List<MapperWithCheckpoint> workers = new ArrayList<>();
        for (int i = 1; i <= this.config.getNbRecordsByPartitions(); i++) {
            MapperWithCheckpoint mapperWithCheckpoint = new MapperWithCheckpoint(this.config, recordBuilderFactory);
            mapperWithCheckpoint.setPartition(i);
            workers.add(mapperWithCheckpoint);
        }
        return workers;
    }

    @Emitter
    public MapperWithCheckpointWorker createWorker() {
        return new MapperWithCheckpointWorker(recordBuilderFactory, this.partition, 10);
    }

    public static class MapperWithCheckpointWorker implements Serializable {

        private final RecordBuilderFactory recordBuilderFactory;

        private int partition;

        private int nbRecords;

        private int current = 0;

        private boolean newBookmark = false;

        public MapperWithCheckpointWorker(final RecordBuilderFactory recordBuilderFactory, int partition,
                int nbRecords) {
            this.recordBuilderFactory = recordBuilderFactory;
            this.partition = partition;
            this.nbRecords = nbRecords;
        }

        @Producer
        public Record next() {
            current++;
            if (current > nbRecords) {
                return null;
            }

            newBookmark = true;
            return recordBuilderFactory.newRecordBuilder()
                    .withInt("partition", partition)
                    .withInt("current", (partition * 100) + current)
                    .build();
        }

        @CheckpointData
        public MapperWithCheckpointCheckpoint getCheckpoint() {
            newBookmark = false;
            MapperWithCheckpointCheckpoint mapperWithCheckpointCheckpoint = new MapperWithCheckpointCheckpoint();
            mapperWithCheckpointCheckpoint.setId(current);
            return mapperWithCheckpointCheckpoint;
        }

    }

    @Data
    @NoArgsConstructor
    @GridLayout(names = GridLayout.FormType.CHECKPOINT,
            value = { @GridLayout.Row("id") })
    @Checkpoint
    public static class MapperWithCheckpointCheckpoint implements Serializable {

        @Option
        @JsonbProperty("id")
        private int id;

    }

    @Data
    @GridLayout(value = {
            @GridLayout.Row("partition")
    })
    @GridLayout(names = GridLayout.FormType.CHECKPOINT, value = {
            @GridLayout.Row("nbRecordsByPartition") })
    public static class MapperWithCheckpointConfig {

        @Option
        private int nbRecordsByPartitions;

        @Option
        private MapperWithCheckpointCheckpoint checkpoint = new MapperWithCheckpointCheckpoint();
    }

}
