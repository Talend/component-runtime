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
package org.talend.sdk.component.runtime.input;

import java.util.function.Function;

import org.talend.sdk.component.runtime.base.Lifecycle;

public interface Input extends Lifecycle {

    Object next();

    /**
     * Start the input with a checkpoint feature.
     *
     * @param resumeCheckpoint
     * @param checkpointFunction
     */
    default void start(final Object resumeCheckpoint, final Function<Object, Void> checkpointFunction) {
        throw new IllegalArgumentException("Checkpoint feature is not implemented.");
    }

    /**
     * For manual checkpointing, this method is called to get the current checkpoint.
     *
     * @return the current checkpoint.
     */
    default Object checkpoint() {
        throw new IllegalArgumentException("Checkpoint feature is not implemented.");
    }
}
