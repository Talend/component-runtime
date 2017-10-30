/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.chain;

import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.output.Branches;
import org.talend.sdk.component.runtime.output.InputFactory;
import org.talend.sdk.component.runtime.output.OutputFactory;
import org.talend.sdk.component.runtime.output.Processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// note: not optimized
@Slf4j
@RequiredArgsConstructor
public class ExecutionChain {

    private final ExecutionChainBuilder.Head chainHead;

    private final ErrorHandler errorHandler;

    private final SuccessListener listener;

    // simple execution where a "group" (bundle) = an element
    public void execute() {
        final Mapper root = chainHead.getInputConfigurer().getMapper();
        root.start();

        final long totalSize = root.assess();
        final ChainedMapper mapper = new ChainedMapper(root, root.split(totalSize).iterator());

        final Input input = mapper.create();
        input.start();

        final long processorCount = onProcessors(chainHead.getInputConfigurer().getChildren(), p -> {
        }).count();
        final List<Processor> processorValues = onProcessors(chainHead.getInputConfigurer().getChildren(), Processor::start)
                .collect(toList());
        if (processorValues.size() != processorCount) {
            throw new IllegalStateException("Some processor didn't start, stopping");
        }

        if (onProcessors(chainHead.getInputConfigurer().getChildren(), Processor::beforeGroup).count() != processorCount) {
            throw new IllegalStateException("Some processor didn't start properly, stopping");
        }
        try {
            Object data;
            while ((data = input.next()) != null) {
                // todo: support outputfactory for input
                onElement(chainHead.getInputConfigurer().getChildren(), data, Branches.DEFAULT_BRANCH);
            }
        } finally {
            try {
                input.stop();
            } catch (final RuntimeException re) {
                log.warn(re.getMessage(), re);
            }
            try {
                root.stop();
            } catch (final RuntimeException re) {
                log.warn(re.getMessage(), re);
            }

            final List<Processor> reversedProcessors = new ArrayList<>(processorValues);
            Collections.reverse(reversedProcessors);
            reversedProcessors.forEach(processor -> {
                try {
                    processor.afterGroup(name -> value -> {
                        // we don't expect this to be used yet so the impl is not optimized
                        final Collection<ExecutionChainBuilder.ProcessorConfigurer<?>> children = chainHead.getInputConfigurer()
                                .getChildren().stream().filter(p -> p.getProcessor() == processor).findFirst()
                                .map(ExecutionChainBuilder.ProcessorConfigurer::getChildren).orElse(null);
                        send(name, children, value);
                        if (listener != null) {
                            try {
                                listener.onData(value);
                            } catch (final RuntimeException re) {
                                // no-op: if we call errorHandler we would corrupt it
                            }
                        }
                    });
                } catch (final RuntimeException re) {
                    log.warn(re.getMessage(), re); // don't prevent others to be stopped!
                }
                try {
                    processor.stop();
                } catch (final RuntimeException re) {
                    log.warn(re.getMessage(), re); // don't prevent others to be stopped!
                }
            });
        }
    }

    private void onElement(final Collection<ExecutionChainBuilder.ProcessorConfigurer<?>> procs, final Object data,
            final String branch) {
        try {
            send(branch, procs, data);
        } catch (final RuntimeException re) {
            if (errorHandler == null) { // no errorHandler means fail fast
                throw re;
            }
            errorHandler.onError(data, re);
        }
    }

    private void send(final String marker, final Collection<ExecutionChainBuilder.ProcessorConfigurer<?>> processors,
            final Object data) {
        processors.stream().filter(p -> p.getMarker().equals(marker)).forEach(p -> {
            final AtomicBoolean noOutput = new AtomicBoolean(true);
            final InputFactory inputFactory = branch -> data; // if we linked something to a processor we want that data ATM
            final OutputFactory outputFactory = name -> value -> {
                send(name, p.getChildren(), value);
                noOutput.compareAndSet(true, false);
            };
            p.getProcessor().onNext(inputFactory, outputFactory);
            if (noOutput.get()) {
                if (listener != null) {
                    try {
                        listener.onData(data);
                    } catch (final RuntimeException re) {
                        // no-op: if we call errorHandler we would corrupt it
                    }
                }
            }
        });
    }

    private Stream<Processor> onProcessors(final Collection<ExecutionChainBuilder.ProcessorConfigurer<?>> children,
            final Consumer<Processor> action) {
        return children.stream().flatMap(p -> {
            try {
                action.accept(p.getProcessor());
                return Stream.concat(Stream.of(p.getProcessor()), onProcessors(p.getChildren(), action));
            } catch (final RuntimeException re) {
                log.warn(re.getMessage(), re);
                return Stream.empty();
            }
        });
    }

    public interface SuccessListener {

        /**
         * @param data the successfully processed data.
         */
        void onData(final Object data);
    }

    public interface ErrorHandler {

        /**
         * @param data the data related to the failure.
         * @param exception the error which made the data processing failed.
         * @return the new chain data, if you want to skip the item, return a {@link Skip}.
         */
        Object onError(final Object data, final RuntimeException exception);
    }

    public interface Skip extends Serializable {

        Skip INSTANCE = new Skip() {
        };
    }

    @RequiredArgsConstructor
    private static final class ChainedInput implements Input {

        private final ChainedMapper parent;

        private Input delegate = null;

        @Override
        public Object next() {
            while (true) {
                if (delegate == null) {
                    delegate = parent.iterator.hasNext() ? parent.iterator.next().create() : null;
                    if (delegate == null) {
                        return null;
                    }
                    delegate.start();
                }
                final Object next = delegate.next();
                if (next != null) {
                    return next;
                }
                delegate.stop();
                delegate = null;
            }
        }

        @Override
        public String plugin() {
            return parent.plugin();
        }

        @Override
        public String rootName() {
            return parent.rootName();
        }

        @Override
        public String name() {
            return parent.name();
        }

        @Override
        public void start() {
            // no-op
        }

        @Override
        public void stop() {
            if (delegate != null) {
                delegate.stop();
            }
        }
    }

    @RequiredArgsConstructor
    private static final class ChainedMapper implements Mapper {

        private final Mapper root;

        private final Iterator<Mapper> iterator;

        @Override
        public long assess() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Mapper> split(final long desiredSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Input create() {
            return new ChainedInput(this);
        }

        @Override
        public boolean isStream() {
            return false;
        }

        @Override
        public String plugin() {
            return root.plugin();
        }

        @Override
        public String rootName() {
            return root.rootName();
        }

        @Override
        public String name() {
            return root.name();
        }

        @Override
        public void start() {
            root.start();
        }

        @Override
        public void stop() {
            root.stop();
        }
    }
}
