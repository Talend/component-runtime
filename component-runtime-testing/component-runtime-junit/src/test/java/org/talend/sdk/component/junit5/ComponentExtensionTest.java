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
package org.talend.sdk.component.junit5;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.junit.ComponentsHandler;
import org.talend.sdk.component.junit.JoinInputFactory;
import org.talend.sdk.component.junit.SimpleComponentRule;
import org.talend.sdk.component.junit.component.BatchTransform;
import org.talend.sdk.component.junit.component.DuplicateEmitTransform;
import org.talend.sdk.component.junit.component.Source;
import org.talend.sdk.component.junit.component.Transform;
import org.talend.sdk.component.runtime.input.Input;
import org.talend.sdk.component.runtime.input.InputImpl;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.input.PartitionMapperImpl;
import org.talend.sdk.component.runtime.output.Processor;

@WithComponents("org.talend.sdk.component.junit.component")
class ComponentExtensionTest {

    @Injected
    private ComponentsHandler handler;

    @Service
    private LocalConfiguration configuration;

    @Service
    private Jsonb jsonb;

    @Test
    void serviceInjection() {
        Stream.of(configuration, jsonb).forEach(Assertions::assertNotNull);
    }

    @Test
    void manualMapper() {
        final Mapper mapper = handler.createMapper(Source.class, new Source.Config() {

            {
                values = asList("a", "b");
            }
        });
        assertFalse(mapper.isStream());
        final Input input = mapper.create();
        assertEquals("a", input.next());
        assertEquals("b", input.next());
        assertNull(input.next());
    }

    @Test
    void sourceCollector() {
        final Mapper mapper = handler.createMapper(Source.class, new Source.Config() {

            {
                values = asList("a", "b");
            }
        });
        assertEquals(asList("a", "b"), handler.collectAsList(String.class, mapper));
    }

    @Test
    void sourceCollectorParallel() {
        final CountDownLatch latch = new CountDownLatch(1);
        final Mapper mapper = new PartitionMapperImpl() {

            @Override
            public long assess() {
                return 2;
            }

            @Override
            public List<Mapper> split(final long desiredSize) {
                assertEquals(1, desiredSize);
                return asList(this, this);
            }

            @Override
            public Input create() {
                return new InputImpl() {

                    private final AtomicBoolean done = new AtomicBoolean();

                    @Override
                    public Object next() {
                        try {
                            latch.await(1, MINUTES);
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            fail();
                        }
                        return done.compareAndSet(false, true) ? Thread.currentThread().getName() : null;
                    }

                    @Override
                    protected Stream<Method> findMethods(final Class<? extends Annotation> marker) {
                        return Stream.empty();
                    }
                };
            }

            @Override
            protected Stream<Method> findMethods(final Class<? extends Annotation> marker) {
                return Stream.empty();
            }
        };
        latch.countDown();

        final Stream<String> collect = handler.collect(String.class, mapper, 2, 2);
        final List<String> threads = collect.collect(toList());
        assertEquals(threads.toString(), 2, threads.size());
        threads.forEach(n -> assertTrue(n, n.startsWith("ComponentExtension-pool-")));
    }

    @Test
    void processorCollector() {
        final Processor processor = handler.createProcessor(Transform.class, null);
        final SimpleComponentRule.Outputs outputs = handler
                .collect(processor,
                        new JoinInputFactory()
                                .withInput("__default__", asList(new Transform.Record("a"), new Transform.Record("bb")))
                                .withInput("second", asList(new Transform.Record("1"), new Transform.Record("2"))));
        assertEquals(2, outputs.size());
        assertEquals(asList(2, 3), outputs.get(Integer.class, "size"));
        assertEquals(asList("a1", "bb2"), outputs.get(String.class, "value"));
    }

    @Test
    void batchProcessorCollector() {
        final Processor processor = handler.createProcessor(BatchTransform.class, null);
        final SimpleComponentRule.Outputs outputs = handler
                .collect(processor, new JoinInputFactory()
                        .withInput("__default__", asList(new BatchTransform.Record("a"), new Transform.Record("bb")))
                        .withInput("second", asList(new BatchTransform.Record("1"), new Transform.Record("2"))));
        assertEquals(2, outputs.size());
        assertEquals(asList(2, 3), outputs.get(Integer.class, "size"));
        assertEquals(asList("a1", "bb2"), outputs.get(String.class, "value"));
    }

    @Test
    void multipleProcessorCollector() {
        final Processor processor = handler.createProcessor(DuplicateEmitTransform.class, null);
        final SimpleComponentRule.Outputs outputs =
                handler
                        .collect(processor,
                                new JoinInputFactory()
                                        .withInput("__default__",
                                                asList(new DuplicateEmitTransform.Record("a"),
                                                        new Transform.Record("bb")))
                                        .withInput("second", asList(new DuplicateEmitTransform.Record("1"),
                                                new Transform.Record("2"))));
        assertEquals(2, outputs.size());
        assertEquals(asList(2, 3), outputs.get(Integer.class, "size"));
        assertEquals(asList("a1", "a1", "bb2", "bb2"), outputs.get(String.class, "value"));
    }
}
