// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.runtime.beam.impl;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.apache.beam.sdk.io.Source;
import org.talend.components.runtime.input.Input;
import org.talend.components.runtime.output.Processor;

import lombok.Data;

@Data
public class BeamInput implements Input {

    private final Source.Reader<?> reader;

    private final Processor processor;

    private final String plugin;

    private final String family;

    private final String name;

    private final ClassLoader loader;

    private final int chunkSize = 10; // arbitrary for now

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
                final boolean hasRecord;
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
                if (hasRecord) {
                    records = doTransform(reader.getCurrent());
                }
                if (processor != null && ++itemCounter > chunkSize) {
                    afterChunk();
                }
                return records.hasNext() ? records.next() : null;
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
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
