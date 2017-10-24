/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.component.runtime.beam.impl;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.state.State;
import org.apache.beam.sdk.state.Timer;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.splittabledofn.RestrictionTracker;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.talend.component.runtime.base.Delegated;
import org.talend.component.runtime.base.Serializer;
import org.talend.component.runtime.output.InputFactory;
import org.talend.component.runtime.output.OutputFactory;
import org.talend.component.runtime.output.Processor;
import org.talend.component.runtime.serialization.ContainerFinder;
import org.talend.component.runtime.serialization.EnhancedObjectInputStream;

import lombok.AllArgsConstructor;

// light reflection based impl of ByteBuddyDoFnRunner of beam
// see org.apache.beam.sdk.transforms.reflect.DoFnSignatures.parseSignature() for the logic
//
// note: this is output oriented so states, timers, new SDF API, ... are not supported
public class BeamProcessorImpl implements Processor, Serializable, Delegated {

    private static final Object[] EMPTY_ARGS = new Object[0];

    private static final Supplier<Object[]> EMPTY_ARGS_SUPPLIER = () -> EMPTY_ARGS;

    private final PipelineOptions options = PipelineOptionsFactory.create();

    private final Object original;

    private final String family;

    private final String name;

    private final String plugin;

    private final DoFn<?, ?> delegate;

    private final InMemoryArgumentProvider argumentProvider;

    private final Method processElement;

    private final Method setup;

    private final Method tearDown;

    private final Method startBundle;

    private final Method finishBundle;

    private final Supplier<Object[]> processArgumentFactory;

    private final Supplier<Object[]> startBundleArgumentFactory;

    private final Supplier<Object[]> finishBundleArgumentFactory;

    private final ClassLoader loader;

    protected BeamProcessorImpl(final Object initialInstance, final DoFn<?, ?> transform, final String plugin,
            final String family, final String name) {
        loader = ContainerFinder.Instance.get().find(plugin).classloader();
        original = initialInstance;
        delegate = transform;
        if (delegate == null) {
            throw new IllegalArgumentException("Didn't find the do fn associated with " + transform);
        }

        argumentProvider = new InMemoryArgumentProvider(options);

        processElement = findMethod(delegate.getClass(), DoFn.ProcessElement.class).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No @ProcessElement on " + delegate));
        setup = findMethod(delegate.getClass(), DoFn.Setup.class).findFirst().orElse(null);
        tearDown = findMethod(delegate.getClass(), DoFn.Teardown.class).findFirst().orElse(null);
        startBundle = findMethod(delegate.getClass(), DoFn.StartBundle.class).findFirst().orElse(null);
        finishBundle = findMethod(delegate.getClass(), DoFn.FinishBundle.class).findFirst().orElse(null);
        Stream.of(setup, tearDown, startBundle, finishBundle, processElement).filter(Objects::nonNull).forEach(m -> {
            if (!m.isAccessible()) {
                m.setAccessible(true);
            }
        });

        {
            final Collection<Supplier<Object>> argSupplier = Stream.of(processElement.getParameters()).map(p -> {
                final Class<?> type = p.getType();
                if (DoFn.ProcessContext.class == type) {
                    return (Supplier<Object>) () -> argumentProvider.processContext(

                            delegate);
                }
                if (DoFn.OnTimerContext.class == type) {
                    return (Supplier<Object>) () -> argumentProvider.onTimerContext(delegate);
                }
                if (BoundedWindow.class.isAssignableFrom(type)) {
                    return (Supplier<Object>) argumentProvider::window;
                }
                if (PipelineOptions.class == type) {
                    return (Supplier<Object>) () -> options;
                }
                if (RestrictionTracker.class.isAssignableFrom(type)) {
                    return (Supplier<Object>) argumentProvider::restrictionTracker;
                }
                if (Timer.class == type) {
                    final String id = ofNullable(p.getAnnotation(DoFn.TimerId.class)).map(DoFn.TimerId::value)
                            .orElseThrow(() -> new IllegalArgumentException("Missing @TimerId on " + p.getName()));
                    return (Supplier<Object>) () -> argumentProvider.timer(id);
                }
                if (State.class == type) {
                    final String id = ofNullable(p.getAnnotation(DoFn.StateId.class)).map(DoFn.StateId::value)
                            .orElseThrow(() -> new IllegalArgumentException("Missing @StateId on " + p.getName()));
                    return (Supplier<Object>) () -> argumentProvider.state(id);
                }
                throw new IllegalArgumentException("unsupported parameter of type " + type);
            }).collect(toList());
            processArgumentFactory = () -> argSupplier.stream().map(Supplier::get).toArray(Object[]::new);
        }
        if (startBundle != null) {
            final Collection<Supplier<Object>> argSupplier = Stream.of(startBundle.getParameters()).map(p -> {
                final Class<?> type = p.getType();
                if (DoFn.StartBundleContext.class == type) {
                    return (Supplier<Object>) () -> argumentProvider.startBundleContext(

                            delegate);
                }
                throw new IllegalArgumentException("unsupported parameter of type " + type + " for " + startBundle);
            }).collect(toList());
            startBundleArgumentFactory = argSupplier.isEmpty() ? EMPTY_ARGS_SUPPLIER
                    : () -> argSupplier.stream().map(Supplier::get).toArray(Object[]::new);
        } else {
            startBundleArgumentFactory = null;
        }
        if (finishBundle != null) {
            final Collection<Supplier<Object>> argSupplier = Stream.of(finishBundle.getParameters()).map(p -> {
                final Class<?> type = p.getType();
                if (DoFn.FinishBundleContext.class == type) {
                    return (Supplier<Object>) () -> argumentProvider.finishBundleContext(

                            delegate);
                }
                throw new IllegalArgumentException("unsupported parameter of type " + type + " for " + finishBundle);
            }).collect(toList());
            finishBundleArgumentFactory = argSupplier.isEmpty() ? EMPTY_ARGS_SUPPLIER
                    : () -> argSupplier.stream().map(Supplier::get).toArray(Object[]::new);
        } else {
            finishBundleArgumentFactory = null;
        }

        this.plugin = plugin;
        this.family = family;
        this.name = name;
    }

    @Override
    public void start() {
        execute(() -> {
            if (setup != null) {
                try {
                    setup.invoke(delegate);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getTargetException());
                }
            }
        });
    }

    @Override
    public void beforeGroup() {
        execute(() -> {
            if (startBundle != null) {
                try {
                    startBundle.invoke(delegate, startBundleArgumentFactory.get());
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getTargetException());
                }
            }
        });
    }

    @Override
    public void onNext(final InputFactory input, final OutputFactory output) {
        execute(() -> {
            argumentProvider.setInputs(input);
            argumentProvider.setOutputs(output);
            try {
                processElement.invoke(delegate, processArgumentFactory.get());
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalStateException(e.getTargetException());
            } finally {
                argumentProvider.setInputs(null);
                argumentProvider.setOutputs(null);
            }
        });
    }

    @Override
    public void afterGroup(final OutputFactory outputs) {
        execute(() -> {
            if (finishBundle != null) {
                argumentProvider.setOutputs(outputs);
                try {
                    finishBundle.invoke(delegate, finishBundleArgumentFactory.get());
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getTargetException());
                } finally {
                    argumentProvider.setOutputs(null);
                }
            }
        });
    }

    @Override
    public void stop() {
        execute(() -> {
            if (tearDown != null) {
                try {
                    tearDown.invoke(delegate);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalStateException(e.getTargetException());
                }
            }
        });
    }

    private Stream<Method> findMethod(final Class<?> aClass, final Class<? extends Annotation> marker) {
        return Stream.concat(Stream.of(aClass.getDeclaredMethods()).filter(m -> m.isAnnotationPresent(marker)),
                DoFn.class == aClass.getSuperclass() ? Stream.empty() : findMethod(aClass.getSuperclass(), marker));
    }

    private void execute(final Runnable task) {
        final Thread thread = Thread.currentThread();
        final ClassLoader tccl = thread.getContextClassLoader();
        thread.setContextClassLoader(this.loader);
        try {
            task.run();
        } finally {
            thread.setContextClassLoader(tccl);
        }
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
    public Object getDelegate() {
        return original;
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializationReplacer(plugin(), rootName(), name(), Serializer.toBytes(delegate));
    }

    @AllArgsConstructor
    private static class SerializationReplacer implements Serializable {

        private final String plugin;

        private final String component;

        private final String name;

        private final byte[] value;

        Object readResolve() throws ObjectStreamException {
            try {
                final DoFn doFn = DoFn.class.cast(loadDelegate());
                return new BeamProcessorImpl(doFn, doFn, plugin, component, name);
            } catch (final IOException | ClassNotFoundException e) {
                throw new InvalidObjectException(e.getMessage());
            }
        }

        private Serializable loadDelegate() throws IOException, ClassNotFoundException {
            try (final ObjectInputStream ois = new EnhancedObjectInputStream(new ByteArrayInputStream(value),
                    ContainerFinder.Instance.get().find(plugin).classloader())) {
                return Serializable.class.cast(ois.readObject());
            }
        }
    }
}
