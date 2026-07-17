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
package org.talend.sdk.component.runtime.output;

import org.talend.sdk.component.api.processor.MultiOutputIterator;
import org.talend.sdk.component.api.processor.OutputEmitter;

public interface OutputFactory {

    OutputEmitter create(String name);

    /**
     * Creates a {@link MultiOutputIterator} that routes records lazily to one or more
     * output connections without buffering.
     *
     * <p>
     * Supported only in the Studio DI runtime.
     *
     * @param <T> the record type
     * @return a MultiOutputIterator for lazy streaming
     * @throws UnsupportedOperationException if the runtime does not support multi-output iterator mode
     */
    default <T> MultiOutputIterator<T> createMultiOutputIterator() {
        throw new UnsupportedOperationException(
                "MultiOutputIterator is only supported in the Studio DI runtime");
    }
}
