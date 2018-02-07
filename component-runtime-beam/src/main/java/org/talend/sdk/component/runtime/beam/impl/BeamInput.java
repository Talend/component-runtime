/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.beam.sdk.io.Source;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class BeamInput implements Input {

    private final Source.Reader<?> reader;

    private final Processor processor;

    private final String plugin;

    private final String family;

    private final String name;

    private final ClassLoader loader;

    private final int chunkSize = 10; // arbitrary for now

    private final long retryOnNoRecordTimeoutSec;

    private boolean started;

    private int itemCounter = 0;

    private Iterator<Object> records;

    @Override
    public Object next() { // note we don't fully respect chunksize but it should be good enough to start
        if (records != null && records.hasNext()) {
            return records.next();
        }
        return execute(() -> {
            try {
                boolean hasRecord;
                if (!started) {
                    hasRecord = reader.start();
                    if (processor != null) {
                        processor.start();
                    }
                    started = true;
                } else {
                    hasRecord = reader.advance();
                }
                if (itemCounter == 0 && processor != null) {
                    processor.beforeGroup();
                }
                if (!hasRecord && retryOnNoRecordTimeoutSec > 0) {
                    final long init = System.currentTimeMillis();
                    final long maxRetryTimestamp = init + TimeUnit.SECONDS.toMillis(retryOnNoRecordTimeoutSec);
                    while (!hasRecord && System.currentTimeMillis() < maxRetryTimestamp) {
                        try {
                            Thread.sleep(500);
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return null;
                        }
                        hasRecord = reader.advance();
                    }
                    if (!hasRecord) {
                        log.warn("No record in {} seconds, quitting", retryOnNoRecordTimeoutSec);
                    }
                }
                if (hasRecord) {
                    records = doTransform(reader.getCurrent());
                }
                if (processor != null && ++itemCounter > chunkSize) {
                    afterChunk();
                }
                if (records != null && records.hasNext()) {
                    return records.next();
                }
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
    }

    @Override
    public String plugin() {
        return plugin;
    }

    @Override
    public String rootName() {
        return family;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void start() {
        // no-op: lazy since it reads a data
    }

    @Override
    public void stop() {
        execute(() -> {
            if (started) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                } finally {
                    if (processor != null) {
                        try {
                            if (itemCounter > 0) {
                                afterChunk();
                            }
                        } finally {
                            processor.stop();
                        }
                    }
                }
            }
            return null;
        });
    }

    private void afterChunk() {
        processor.afterGroup(name -> value -> {
            throw new IllegalArgumentException("chunk outputs are not yet supported");
        });
        itemCounter = 0;
    }

    private <T> T execute(final Supplier<T> task) {
        final Thread thread = Thread.currentThread();
        final ClassLoader tccl = thread.getContextClassLoader();
        thread.setContextClassLoader(this.loader);
        try {
            return task.get();
        } finally {
            thread.setContextClassLoader(tccl);
        }
    }

    private Iterator<Object> doTransform(final Object current) {
        if (current == null || processor == null) {
            return new SingleElementIterator<>(current);
        }
        final StoringOuputFactory output = new StoringOuputFactory();
        processor.onNext(new SingleInputFactory(current), output);
        if (output.getValues() != null) {
            return output.getValues().iterator();
        }
        return new SingleElementIterator<>(current); // do we want to return an empty iterator here?
    }

    @Data
    private static class SingleElementIterator<T> implements Iterator<T> {

        private final T element;

        private boolean done;

        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public T next() {
            if (done) {
                throw new NoSuchElementException();
            }
            try {
                return element;
            } finally {
                done = true;
            }
        }
    }
}
