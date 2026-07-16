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
 * Allows a processor to route records from a single lazy iterator to multiple
 * output connections, enabling true streaming split without buffering.
 *
 * <p>
 * Unlike using separate {@link OutputIterator}s per output (which are consumed
 * in lockstep by the drain loop), a {@code MultiOutputIterator} uses a single
 * source iterator where each element is tagged with its target output name via
 * {@link TaggedOutput}. The Studio DI runtime reads one tagged record at a time
 * and routes it to the appropriate connection — enabling correct split behavior
 * when different records go to different outputs.
 *
 * <p>
 * <b>Important:</b> This interface is supported only in the Studio DI runtime.
 *
 * <p>
 * Example usage (split in @ElementListener):
 *
 * <pre>
 * {@code
 * 
 * &#64;ElementListener
 * public void process(&#64;Input Record input,
 *         &#64;Output MultiOutputIterator<Record> splitter) {
 *     splitter.setIterator(myLazySplitIterator(input));
 * }
 * }
 * </pre>
 *
 * <p>
 * Example usage (bulk split in @AfterGroup):
 *
 * <pre>
 * {@code
 * 
 * &#64;AfterGroup
 * public void afterGroup(&#64;Output MultiOutputIterator<Record> splitter) {
 *     splitter.setIterator(
 *             records.stream()
 *                     .map(r -> isValid(r)
 *                             ? TaggedOutput.of("MAIN", transform(r))
 *                             : TaggedOutput.of("REJECT", r))
 *                     .iterator());
 * }
 * }
 * </pre>
 *
 * <p>
 * The drain loop on the Studio side should use per-connection
 * {@code hasDataFor(name)} checks to efficiently consume only the records
 * belonging to each output:
 *
 * <pre>
 * {@code
 * while (outputsHandler.hasMoreData()) {
 *     if (outputsHandler.hasDataFor("MAIN"))   mainRow   = outputsHandler.getValue("MAIN");
 *     if (outputsHandler.hasDataFor("REJECT"))  rejectRow = outputsHandler.getValue("REJECT");
 * }
 * }
 * </pre>
 *
 * @param <T> the record type
 * @see TaggedOutput
 * @see OutputIterator
 */
public interface MultiOutputIterator<T> {

    /**
     * Sets the lazy iterator that produces tagged output records on demand.
     * Each {@link TaggedOutput} specifies which output connection receives the record.
     *
     * @param iterator the iterator producing tagged records
     */
    void setIterator(Iterator<TaggedOutput<T>> iterator);
}
