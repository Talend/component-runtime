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
package org.talend.sdk.component.runtime.di;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.record.RecordConverters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseIOHandler {

    protected final Jsonb jsonb;

    protected final RecordBuilderFactory recordBuilderMapper;

    protected final RecordConverters converters;

    protected final Map<String, IO> connections = new TreeMap<>();

    /**
     * Functional interface used internally to advance the tagged source one step.
     * Implementations call {@link #setPending(String, Object)} to store the next
     * record's output name and converted value, then return {@code true}.
     * Return {@code false} when the source is exhausted.
     */
    @FunctionalInterface
    protected interface TaggedAdvancer {

        boolean advance();
    }

    /**
     * Flat array of all registered {@link IO} values.
     * Cached after the first call to {@link #ioArray()} and invalidated whenever
     * {@link #addConnection} or {@link #init} modifies {@link #connections}.
     * Using an array in the hot drain loop avoids creating a new {@link java.util.Iterator}
     * on each {@link #hasMoreData()} invocation.
     */
    private IO[] ioCache;

    /**
     * Tagged-source advancer for split streaming.
     * Non-null only when the component used a
     * {@link org.talend.sdk.component.api.processor.MultiOutputIterator}.
     */
    private TaggedAdvancer taggedSource;

    /** Output-connection name of the look-ahead record; {@code null} when no record is pending. */
    private String pendingOutputName;

    /** Converted record value paired with {@link #pendingOutputName}. */
    private Object pendingRecord;

    public BaseIOHandler(final Jsonb jsonb, final Map<Class<?>, Object> servicesMapper) {
        this.jsonb = jsonb;
        this.recordBuilderMapper = (RecordBuilderFactory) servicesMapper.get(RecordBuilderFactory.class);
        this.converters = new RecordConverters();
    }

    public void init(final Collection<String> branchesOrder) {
        if (branchesOrder == null) {
            return;
        }

        final Map<String, String> mapping = new HashMap<>(); // temp structure to avoid concurrent modification
        final Iterator<String> branches = branchesOrder.iterator();
        for (final String rowStruct : connections.keySet()) {
            if (!branches.hasNext()) {
                break;
            }
            mapping.put(rowStruct, branches.next());
        }
        if (!mapping.isEmpty()) {
            mapping.forEach((row, branch) -> connections.putIfAbsent(branch, connections.get(row)));
            ioCache = null;
        }
    }

    public void addConnection(final String connectorName, final Class<?> type) {
        connections.put(connectorName, new IO<>(type));
        ioCache = null;
    }

    /** Returns a cached flat array of all registered IO objects for allocation-free iteration. */
    @SuppressWarnings("unchecked")
    private IO[] ioArray() {
        if (ioCache == null) {
            ioCache = connections.values().toArray(IO[]::new);
        }
        return ioCache;
    }

    public void reset() {
        for (final IO io : ioArray()) {
            io.reset();
        }
        taggedSource = null;
        pendingOutputName = null;
        pendingRecord = null;
    }

    public <T> T getValue(final String connectorName) {
        if (taggedSource != null) {
            if (connectorName.equals(pendingOutputName)) {
                final T value = (T) pendingRecord;
                pendingOutputName = null;
                pendingRecord = null;
                return value;
            }
            return null;
        }
        return (T) connections.get(connectorName).next();
    }

    public boolean hasMoreData() {
        if (taggedSource != null) {
            return pendingOutputName != null || taggedSource.advance();
        }
        for (final IO io : ioArray()) {
            if (io.hasNext()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the named connection has a record ready to be consumed.
     *
     * <p>
     * In tagged-source mode (when the component used a
     * {@link org.talend.sdk.component.api.processor.MultiOutputIterator}),
     * this peeks one record ahead from the shared tagged source and returns whether it
     * belongs to {@code connectionName}. The peeked record is buffered and consumed by
     * the next {@link #getValue(String)} call for the same connection.
     *
     * <p>
     * In independent-iterator mode, this directly checks the per-connection iterator/queue.
     *
     * <p>
     * Use this method in the Studio drain loop to avoid calling {@link #getValue(String)}
     * on connections that have no data:
     *
     * <pre>{@code
     * while (outputsHandler.hasMoreData()) {
     *     if (outputsHandler.hasDataFor("MAIN"))
     *         mainRow = outputsHandler.getValue("MAIN");
     *     if (outputsHandler.hasDataFor("REJECT"))
     *         rejectRow = outputsHandler.getValue("REJECT");
     * }
     * }</pre>
     *
     * @param connectionName the output connection name to check
     * @return {@code true} if a record for this connection is immediately available
     */
    public boolean hasDataFor(final String connectionName) {
        if (taggedSource != null) {
            if (pendingOutputName != null) {
                return pendingOutputName.equals(connectionName);
            }
            return taggedSource.advance() && pendingOutputName.equals(connectionName);
        }
        final IO io = connections.get(connectionName);
        return io != null && io.hasNext();
    }

    /**
     * Sets a tagged-source advancer for split streaming across multiple outputs.
     * Switching to tagged mode — subsequent {@link #hasMoreData()}, {@link #hasDataFor(String)},
     * and {@link #getValue(String)} operate via the advancer instead of per-connection queues.
     *
     * <p>
     * The {@code advancer} implementation must call {@link #setPending(String, Object)} with
     * the next output-connection name and converted record value, then return {@code true}.
     * Return {@code false} when the source is exhausted.
     *
     * @param advancer the tagged-source advancer produced by a MultiOutputIterator setup
     */
    protected void setTaggedSource(final TaggedAdvancer advancer) {
        this.taggedSource = advancer;
        this.pendingOutputName = null;
        this.pendingRecord = null;
    }

    /**
     * Called by a {@link TaggedAdvancer} to store the next pending record.
     *
     * @param outputName the output connection name for the record
     * @param record the converted record value
     */
    protected void setPending(final String outputName, final Object record) {
        this.pendingOutputName = outputName;
        this.pendingRecord = record;
    }

    protected String getActualName(final String name) {
        return "__default__".equals(name) ? "FLOW" : name;
    }

    /**
     * Represents a single output connection's data holder.
     * Supports two modes (mutually exclusive per invocation):
     * <ul>
     * <li><b>Push mode</b> (default): records are added via {@link #add(Object)} into the internal queue.
     * Used by {@code OutputEmitter.emit()}.</li>
     * <li><b>Pull mode</b> (iterator): a lazy {@link Iterator} source is set via {@link #setSource(Iterator)}.
     * Records are produced on-demand during the drain loop ({@link #hasNext()}/{@link #next()}).
     * Used by {@code OutputIterator.setIterator()} in the Studio DI runtime.</li>
     * </ul>
     * In pull mode, one record is eagerly buffered by {@link #hasNext()} so that
     * subsequent calls to {@link #hasNext()} and {@link #next()} make no further
     * calls to the source iterator — eliminating repeated {@code source.hasNext()}
     * invocations across {@code hasMoreData()}, {@code hasDataFor()}, and {@code getValue()}.
     * <p>
     * Note: The {@code source} field is only used by {@code OutputsHandler}; {@code InputsHandler}
     * uses only the queue-based push mode.
     */
    @RequiredArgsConstructor
    static class IO<T> {

        private final Queue<T> values = new LinkedList<>();

        private final Class<T> type;

        private Iterator<T> source;

        /** One-record look-ahead buffer for pull mode. Non-null iff {@link #bufferFull} is true. */
        private T buffered;

        /** True when {@link #buffered} holds a record ready to be returned by {@link #next()}. */
        private boolean bufferFull;

        void setSource(final Iterator<T> source) {
            closeSource();
            this.source = source;
            this.buffered = null;
            this.bufferFull = false;
        }

        private void reset() {
            values.clear();
            closeSource();
            this.source = null;
            this.buffered = null;
            this.bufferFull = false;
        }

        boolean hasNext() {
            if (!values.isEmpty()) {
                return true;
            }
            if (bufferFull) {
                return true;
            }
            if (source != null && source.hasNext()) {
                buffered = type.cast(source.next());
                bufferFull = true;
                return true;
            }
            return false;
        }

        T next() {
            if (!values.isEmpty()) {
                return type.cast(values.poll());
            }
            if (bufferFull) {
                final T val = buffered;
                buffered = null;
                bufferFull = false;
                return val;
            }
            return null;
        }

        void add(final T e) {
            values.add(e);
        }

        Class<T> getType() {
            return type;
        }

        private void closeSource() {
            if (source instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) source).close();
                } catch (final Exception e) {
                    log.debug("Failed to close iterator source", e);
                }
            }
        }
    }

}
