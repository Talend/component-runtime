/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.visitor;

import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.standalone.DriverRunner;

public interface ModelListener {

    default void onPartitionMapper(final Class<?> type, final PartitionMapper partitionMapper) {
        // no-op
    }

    default void onEmitter(final Class<?> type, final Emitter emitter) {
        // no-op
    }

    default void onProcessor(final Class<?> type, final Processor processor) {
        // no-op
    }

    default void onDriverRunner(final Class<?> type, final DriverRunner processor) {
        // no-op
    }
}
