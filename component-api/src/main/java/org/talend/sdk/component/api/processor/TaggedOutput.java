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
package org.talend.sdk.component.api.processor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A record tagged with its target output connection name.
 * Used with {@link MultiOutputIterator} to route individual records to
 * specific output connections from a single streaming iterator.
 *
 * @param <T> the record type
 */
@Getter
@RequiredArgsConstructor
public class TaggedOutput<T> {

    /**
     * The name of the output connection this record should be routed to.
     * Use {@code "__default__"} or {@code "FLOW"} for the default output.
     */
    private final String outputName;

    /** The record to emit to the named output. */
    private final T record;

    /**
     * Convenience factory method.
     *
     * @param outputName the target output connection name
     * @param record the record to emit
     * @param <T> the record type
     * @return a new TaggedOutput
     */
    public static <T> TaggedOutput<T> of(final String outputName, final T record) {
        return new TaggedOutput<>(outputName, record);
    }
}
