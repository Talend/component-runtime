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
package org.talend.sdk.component.runtime.di.beam;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.io.BoundedSource;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.joda.time.Instant;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class DelegatingBoundedSource<T> extends BoundedSource<T> implements SettableSourceListener {

    private BoundedSource<T> delegate;

    private SourceListener listener;

    @Override
    public List<? extends BoundedSource<T>> split(final long desiredBundleSizeBytes, final PipelineOptions options)
            throws Exception {
        final List<? extends BoundedSource<T>> sources = delegate.split(desiredBundleSizeBytes, options);
        listener.onSplit(sources.size());
        log.debug("Split {} in {} sources ({})", delegate, sources.size(), sources);
        return sources.stream().map(s -> new DelegatingBoundedSource<>(s, listener)).collect(toList());
    }

    @Override
    public long getEstimatedSizeBytes(final PipelineOptions options) throws Exception {
        return delegate.getEstimatedSizeBytes(options);
    }

    @Override
    public BoundedReader<T> createReader(final PipelineOptions options) throws IOException {
        final BoundedReader<T> boundedReader = delegate.createReader(options);
        final DelegatingBoundedReader<T> reader = new DelegatingBoundedReader<>(this, boundedReader, listener);
        log.debug("Creating reader {} from source {}", boundedReader, delegate);
        return reader;
    }

    @Override
    public void validate() {
        delegate.validate();
    }

    @Override
    @Deprecated
    public Coder<T> getDefaultOutputCoder() {
        return delegate.getDefaultOutputCoder();
    }

    @Override
    public Coder<T> getOutputCoder() {
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DelegatingBoundedSource<?> that = (DelegatingBoundedSource<?>) o;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class DelegatingBoundedReader<T> extends BoundedReader<T> {

        private BoundedSource<T> source;

        private BoundedReader<T> delegate;

        private SourceListener listener;

        @Override
        public Double getFractionConsumed() {
            return delegate.getFractionConsumed();
        }

        @Override
        public long getSplitPointsConsumed() {
            return delegate.getSplitPointsConsumed();
        }

        @Override
        public long getSplitPointsRemaining() {
            return delegate.getSplitPointsRemaining();
        }

        @Override
        public BoundedSource<T> getCurrentSource() {
            return source;
        }

        @Override
        public BoundedSource<T> splitAtFraction(final double fraction) {
            final BoundedSource<T> newSource = delegate.splitAtFraction(fraction);
            if (newSource != null) {
                listener.onSplit(1);
                log.debug("Split at fraction {} reader {} and got source {}", fraction, delegate, newSource);
                return new DelegatingBoundedSource<T>(newSource, listener) {
                };
            }
            return null;
        }

        @Override
        public Instant getCurrentTimestamp() throws NoSuchElementException {
            return delegate.getCurrentTimestamp();
        }

        @Override
        public boolean start() throws IOException {
            return delegate.start();
        }

        @Override
        public boolean advance() throws IOException {
            return delegate.advance();
        }

        @Override
        public T getCurrent() throws NoSuchElementException {
            listener.onElement();
            return delegate.getCurrent();
        }

        @Override
        public void close() throws IOException {
            try {
                delegate.close();
            } finally {
                log.debug("Closing reader {} of source {}", delegate, delegate.getCurrentSource());
                listener.onReaderClose();
            }
        }
    }
}
