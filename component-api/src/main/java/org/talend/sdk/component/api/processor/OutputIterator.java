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

import java.util.Iterator;

/**
 * Allows a processor to provide a lazy iterator for output records
 * instead of pushing them via {@link OutputEmitter#emit(Object)}.
 *
 * <p>
 * Used with {@code @Output} on an {@code OutputIterator} parameter to enable streaming:
 * records are produced on-demand during the consumer's drain loop,
 * rather than buffered entirely in memory.
 *
 * <p>
 * Example usage in a processor:
 * 
 * <pre>
 * {@code
 * 
 * &#64;ElementListener
 * public void process(@Input Record input,
 *         @Output OutputIterator<Record> output) {
 *     output.setIterator(myLazyIterator(input));
 * }
 * }
 * </pre>
 *
 * @param <T> the record type
 */
public interface OutputIterator<T> {

    /**
     * Sets the lazy iterator that will produce output records on demand.
     * Call this once per invocation; the consumer will pull records
     * by calling {@link Iterator#hasNext()} and {@link Iterator#next()}.
     *
     * @param iterator the lazy iterator providing output records
     */
    void setIterator(Iterator<T> iterator);
}
