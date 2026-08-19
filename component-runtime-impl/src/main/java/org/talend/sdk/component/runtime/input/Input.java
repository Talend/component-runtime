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
package org.talend.sdk.component.runtime.input;

import java.util.function.Consumer;

import org.talend.sdk.component.runtime.base.Lifecycle;

public interface Input extends Lifecycle {

    Object next();

    /**
     * Start the input with a checkpoint callback feature and resuming state.
     */
    default void start(final Consumer<CheckpointState> checkpointCallback) {
        throw new UnsupportedOperationException("#start()");
    }

    /**
     * Retrieve the current checkpoint.
     */
    default CheckpointState getCheckpoint() {
        throw new UnsupportedOperationException("#getCheckpoint()");
    }

    /**
     * Is a new checkpoint available.
     */
    default boolean isCheckpointReady() {
        throw new UnsupportedOperationException("#isCheckpointReady()");
    }

}
