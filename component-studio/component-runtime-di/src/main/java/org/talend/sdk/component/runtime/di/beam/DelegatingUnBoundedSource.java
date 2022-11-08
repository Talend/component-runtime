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
package org.talend.sdk.component.runtime.di.beam;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.UnboundedSource;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.joda.time.Instant;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class DelegatingUnBoundedSource<A, B extends UnboundedSource.CheckpointMark> extends UnboundedSource<A, B>
        implements SettableSourceListener {

    private UnboundedSource<A, B> delegate;

    private SourceListener listener;

    @Override
    public List<? extends UnboundedSource<A, B>> split(final int desiredNumSplits, final PipelineOptions options)
            throws Exception {
        final List<? extends UnboundedSource<A, B>> sources = delegate.split(desiredNumSplits, options);
        listener.onSplit(sources.size());
        return sources.stream().map(s -> new DelegatingUnBoundedSource<>(s, listener)).collect(toList());
    }

    @Override
    public UnboundedReader<A> createReader(final PipelineOptions options, final B checkpointMark) throws IOException {
        return new DelegatingUnboundedReader<>(delegate.createReader(options, checkpointMark), listener);
    }

    @Override
    public Coder<B> getCheckpointMarkCoder() {
        return delegate.getCheckpointMarkCoder();
    }

    @Override
    public boolean requiresDeduping() {
        return delegate.requiresDeduping();
    }

    @Override
    public void validate() {
        delegate.validate();
    }

    @Override
    @Deprecated
    public Coder<A> getDefaultOutputCoder() {
        return delegate.getDefaultOutputCoder();
    }

    @Override
    public Coder<A> getOutputCoder() {
        return delegate.getOutputCoder();
    }

    @Override
    public void populateDisplayData(final DisplayData.Builder builder) {
        delegate.populateDisplayData(builder);
    }

    @Override
    public void setSourceListener(final SourceListener listener) {
        this.listener = listener;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class DelegatingUnboundedReader<A> extends UnboundedReader<A> {

        private UnboundedReader<A> reader;

        private SourceListener listener;

        @Override
        public boolean start() throws IOException {
            return reader.start();
        }

        @Override
        public boolean advance() throws IOException {
            return reader.advance();
        }

        @Override
        public byte[] getCurrentRecordId() throws NoSuchElementException {
            return reader.getCurrentRecordId();
        }

        @Override
        public Instant getWatermark() {
            return reader.getWatermark();
        }

        @Override
        public CheckpointMark getCheckpointMark() {
            return reader.getCheckpointMark();
        }

        @Override
        public long getSplitBacklogBytes() {
            return reader.getSplitBacklogBytes();
        }

        @Override
        public long getTotalBacklogBytes() {
            return reader.getTotalBacklogBytes();
        }

        @Override
        public UnboundedSource<A, ?> getCurrentSource() {
            return reader.getCurrentSource();
        }

        @Override
        public A getCurrent() throws NoSuchElementException {
            listener.onElement();
            return reader.getCurrent();
        }

        @Override
        public Instant getCurrentTimestamp() throws NoSuchElementException {
            listener.onElement();
            return reader.getCurrentTimestamp();
        }

        @Override
        public void close() throws IOException {
            try {
                reader.close();
            } finally {
                listener.onReaderClose();
            }
        }
    }
}
