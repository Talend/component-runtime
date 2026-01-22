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
package org.talend.test.valid.checkpoint;

import static org.talend.sdk.component.api.component.Icon.IconType.FILE_JOB_O;

import java.io.Serializable;

import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.Producer;
import org.talend.sdk.component.api.input.checkpoint.Checkpoint;
import org.talend.sdk.component.api.input.checkpoint.CheckpointAvailable;
import org.talend.sdk.component.api.input.checkpoint.CheckpointData;

import lombok.RequiredArgsConstructor;

@Emitter(family = "checkpoint", name = "input")
@Version
@Icon(FILE_JOB_O)
@RequiredArgsConstructor
public class InputCheckpoint implements Serializable {

    private final MyConfiguration configuration;

    @Producer
    public Object data() {
        return null;
    }

    @CheckpointData
    public CheckpointConfig getCheckpoint() {
        return null;
    }

    @CheckpointAvailable
    public boolean isCheckpointAvailable() {
        return false;
    }

    public static class MyConfiguration implements Serializable {

        @Option
        private String data = "data";

        @Option
        private CheckpointConfig checkpoint;

    }

    @Checkpoint
    public static class CheckpointConfig implements Serializable {

        private String position;
    }
}
