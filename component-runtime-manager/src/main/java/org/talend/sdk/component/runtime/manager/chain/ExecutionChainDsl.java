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
package org.talend.sdk.component.runtime.manager.chain;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.chain.internal.DSLParser;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// using an interface since it is easier to compose and doesn't have to host any state
public interface ExecutionChainDsl {

    @NoArgsConstructor(access = PRIVATE)
    class Root {

        public static ConfigurableExecutionChainFluentDsl from(final String uri) {
            final Iterator<ExecutionChainDsl> iterator = ServiceLoader.load(ExecutionChainDsl.class).iterator();
            return iterator.hasNext() ? iterator.next().from(uri) : new ExecutionChainDsl() {
            }.from(uri);
        }
    }

    default ConfigurableExecutionChainFluentDsl from(final String uri) {
        final DSLParser.Step inStep = DSLParser.parse(uri);
        return new ConfigurableExecutionChainFluentDsl() {

            private ChainConfiguration configuration = new ChainConfiguration("Job", true, data -> {
                // no-op
            }, (data, exception) -> {
                throw exception;
            });

            private ExecutionChainBuilder.InputConfigurer in;

            private ExecutionChainBuilder.ProcessorConfigurer<?> processor;

            @Override
            public ExecutionChainFluentDsl configure(final ChainConfiguration configuration) {
                this.configuration = configuration;
                this.in = ExecutionChainBuilder
                        .start()
                        .withConfiguration(ofNullable(configuration.getName()).orElse("Job"),
                                configuration.isSupportsOverride())
                        .fromInput(inStep.getFamily(), inStep.getComponent(), inStep.getVersion(),
                                inStep.getConfiguration());
                return this;
            }

            @Override
            public ExecutionChainFluentDsl to(final String uri) {
                if (in == null) {
                    in = ExecutionChainBuilder.start().fromInput(inStep.getFamily(), inStep.getComponent(),
                            inStep.getVersion(), inStep.getConfiguration());
                }
                final DSLParser.Step step = DSLParser.parse(uri);
                final String marker = step.getConfiguration().remove("__branch");
                processor = (processor == null ? in : processor).linkProcessor(marker, step.getFamily(),
                        step.getComponent(), step.getVersion(), step.getConfiguration());
                return this;
            }

            @Override
            public Execution create() {
                if (in == null) {
                    throw new IllegalArgumentException("No processor in the Job, this is an invalid chain.");
                }
                final ComponentManager manager = ComponentManager.instance();
                final ExecutionChain chainSupplier = processor
                        .create(manager, manager.getContainer()::resolve, configuration.getSuccessListener(),
                                configuration.getErrorHandler())
                        .get();
                return chainSupplier::execute;
            }
        };
    }

    interface ConfigurableExecutionChainFluentDsl extends ExecutionChainFluentDsl {

        ExecutionChainFluentDsl configure(ChainConfiguration supportsOverride);
    }

    interface ExecutionChainFluentDsl {

        ExecutionChainFluentDsl to(String uri);

        Execution create();
    }

    @FunctionalInterface
    interface Execution {

        void execute();
    }

    @Data
    @Builder
    class ChainConfiguration {

        private String name;

        private boolean supportsOverride;

        private ExecutionChain.SuccessListener successListener;

        private ExecutionChain.ErrorHandler errorHandler;
    }
}
