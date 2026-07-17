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
 * Allows a processor to stream records lazily to multiple output connections
 * without buffering, supporting two mutually exclusive modes per invocation:
 *
 * <ul>
 * <li><b>Split mode</b> ({@link #setIterator(Iterator)}): a single tagged iterator
 * routes each record to a specific output connection via {@link TaggedOutput}.
 * Use when records come from one shared source and must be split between outputs.</li>
 * <li><b>Independent mode</b> ({@link #setIterator(String, Iterator)}): each output
 * connection gets its own independent lazy iterator, consumed in parallel by the drain
 * loop. Use when each output has its own self-contained lazy source.</li>
 * </ul>
 *
 * <p>
 * Both modes can be selected at runtime from the same fixed method parameter,
 * so the component does not need separate parameters per output.
 *
 * <p>
 * <b>Important:</b> This interface is supported only in the Studio DI runtime.
 *
 * <p>
 * <b>Split mode example</b> (one source, per-record routing):
 *
 * <pre>
 * {@code
 * 
 * &#64;ElementListener
 * public void process(&#64;Input Record input,
 *         &#64;Output MultiOutputIterator<Record> out) {
 *     out.setIterator(
 *             mySource.stream()
 *                     .map(r -> isValid(r)
 *                             ? TaggedOutput.of("MAIN", transform(r))
 *                             : TaggedOutput.of("REJECT", r))
 *                     .iterator());
 * }
 * }
 * </pre>
 *
 * <p>
 * <b>Independent mode example</b> (each output has its own lazy source):
 *
 * <pre>
 * {@code
 * 
 * &#64;AfterGroup
 * public void afterGroup(&#64;Output MultiOutputIterator<Record> out) {
 *     out.setIterator("MAIN", mainDatabase.lazyQuery());
 *     out.setIterator("REJECT", errorLog.lazyRead());
 * }
 * }
 * </pre>
 *
 * <p>
 * The drain loop on the Studio side uses {@code hasDataFor(name)} for correct
 * per-connection checking in both modes:
 *
 * <pre>
 * {@code
 * while (outputsHandler.hasMoreData()) {
 *     if (outputsHandler.hasDataFor("MAIN"))
 *         mainRow = outputsHandler.getValue("MAIN");
 *     if (outputsHandler.hasDataFor("REJECT"))
 *         rejectRow = outputsHandler.getValue("REJECT");
 * }
 * }
 * </pre>
 *
 * @param <T> the record type
 * @see TaggedOutput
 */
public interface MultiOutputIterator<T> {

    /**
     * <b>Split mode</b>: sets a single lazy iterator whose elements are tagged with
     * the target output connection name via {@link TaggedOutput}.
     * The runtime reads one record at a time and routes it to the matching connection.
     *
     * <p>
     * Mutually exclusive with {@link #setIterator(String, Iterator)} within one invocation.
     *
     * @param iterator the tagged iterator routing records to their named outputs
     */
    void setIterator(Iterator<TaggedOutput<T>> iterator);

    /**
     * <b>Independent mode</b>: assigns a lazy iterator to a specific named output connection.
     * Call once per output that needs a dedicated lazy source; connections without an
     * assigned iterator fall back to the push-mode queue as usual.
     *
     * <p>
     * Mutually exclusive with {@link #setIterator(Iterator)} within one invocation.
     *
     * @param outputName the output connection name (e.g. {@code "MAIN"}, {@code "REJECT"})
     * @param iterator the lazy iterator producing records for that connection
     */
    void setIterator(String outputName, Iterator<T> iterator);
}
