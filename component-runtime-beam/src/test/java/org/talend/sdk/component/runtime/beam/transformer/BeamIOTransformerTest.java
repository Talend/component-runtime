/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.beam.transformer;

import static org.apache.ziplock.JarLocation.jarLocation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CustomCoder;
import org.apache.beam.sdk.coders.StringUtf8Coder;
import org.apache.beam.sdk.io.BoundedSource;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.display.DisplayData;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.runtime.serialization.ContainerFinder;
import org.talend.sdk.component.runtime.serialization.LightContainer;

class BeamIOTransformerTest {

    @Test
    void doFn() {
        scenario((transformer, loader) -> {
            final Class<?> aClass = loader.loadClass(MyDoFn.class.getName());
            assertEquals(loader, aClass.getClassLoader());
            final Object instance = newInstance(aClass, loader);
            aClass.getMethod("setup").invoke(instance);
            aClass.getMethod("doFn", String.class).invoke(instance, "test");
            aClass.getMethod("teardown").invoke(instance);
        });
    }

    @Test
    void boundedSource() {
        scenario((transformer, loader) -> {
            final Class<?> aClass = loader.loadClass(MyBoundedSource.class.getName());
            assertEquals(loader, aClass.getClassLoader());
            final Object instance = newInstance(aClass, loader);
            final PipelineOptions pipelineOptions = PipelineOptionsFactory.create();
            aClass.getMethod("split", long.class, PipelineOptions.class).invoke(instance, 1, pipelineOptions);
            aClass.getMethod("createReader", PipelineOptions.class).invoke(instance, pipelineOptions);
            aClass.getMethod("getEstimatedSizeBytes", PipelineOptions.class).invoke(instance, pipelineOptions);
            aClass.getMethod("getDefaultOutputCoder").invoke(instance);
            aClass.getMethod("populateDisplayData", DisplayData.Builder.class).invoke(instance, new Object[] { null });
        });
    }

    @Test
    void coder() {
        scenario((transformer, loader) -> {
            final Class<?> aClass = loader.loadClass(MyCoder.class.getName());
            assertEquals(loader, aClass.getClassLoader());
            final Object instance = newInstance(aClass, loader);
            aClass.getMethod("encode", String.class, OutputStream.class).invoke(instance, null, null);
            aClass.getMethod("decode", InputStream.class).invoke(instance, new Object[] { null });
        });
    }

    private Object newInstance(final Class<?> aClass, final ClassLoader validationLoader) {
        try {
            final Object instance = aClass.getConstructor().newInstance();
            SetValidator.class
                    .cast(instance)
                    .setValidator(() -> assertEquals(Thread.currentThread().getContextClassLoader(), validationLoader));
            return instance;
        } catch (final Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    private void scenario(final Scenario runnable) {
        final AtomicReference<ContainerFinder> containerFinder;
        try {
            final Field finder = ContainerFinder.Instance.class.getDeclaredField("FINDER");
            if (!finder.isAccessible()) {
                finder.setAccessible(true);
            }
            containerFinder = (AtomicReference<ContainerFinder>) finder.get(null);
            final ContainerFinder oldContainerFinder = containerFinder.get();
            try {
                final Thread thread = Thread.currentThread();
                final ClassLoader originalLoader = thread.getContextClassLoader();
                final String prefix = BeamIOTransformerTest.class.getName() + "$";
                final Predicate<String> parentPredicate =
                        it -> SetValidator.class.getName().equals(it) || !it.startsWith(prefix);
                try (final ConfigurableClassLoader loader = new ConfigurableClassLoader("test",
                        new URL[] { jarLocation(BeamIOTransformerTest.class).toURI().toURL() }, originalLoader,
                        parentPredicate, parentPredicate.negate(), new String[0])) {
                    // thread.setContextClassLoader(loader); // don't set it, this is what we test!
                    final BeamIOTransformer transformer = new BeamIOTransformer();
                    loader.registerTransformer(transformer);
                    containerFinder.set(plugin -> new LightContainer() {

                        @Override
                        public ClassLoader classloader() {
                            return loader;
                        }

                        @Override
                        public <T> T findService(final Class<T> key) {
                            return null;
                        }
                    });
                    runnable.execute(transformer, loader);
                } catch (final Exception e) {
                    doFail(e);
                } finally {
                    thread.setContextClassLoader(originalLoader);
                }
            } finally {
                containerFinder.set(oldContainerFinder);
            }
        } catch (final Exception t) {
            doFail(t);
        }
    }

    private void doFail(final Throwable t) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (final PrintStream pout = new PrintStream(out)) {
            t.printStackTrace(pout);
        }
        fail(new String(out.toByteArray(), StandardCharsets.UTF_8));
    }

    protected interface SetValidator {

        void setValidator(Runnable runnable);
    }

    public static class MyDoFn extends DoFn<String, String> implements SetValidator

    {

        private Runnable validator;

        @Setup
        public void setup() {
            validator.run();
        }

        @Teardown
        public void teardown() {
            validator.run();
        }

        @ProcessElement
        public void doFn(@Element final String element) {
            validator.run();
        }

        @Override
        public void setValidator(final Runnable runnable) {
            this.validator = runnable;
        }
    }

    public static class MyCoder extends CustomCoder<String> implements SetValidator

    {

        private Runnable validator;

        @Override
        public void setValidator(final Runnable runnable) {
            this.validator = runnable;
        }

        @Override
        public void encode(final String value, final OutputStream outStream) {
            validator.run();
        }

        @Override
        public String decode(final InputStream inStream) {
            validator.run();
            return null;
        }
    }

    public static class MyBoundedSource extends BoundedSource<String> implements SetValidator {

        private Runnable validator;

        @Override
        public List<? extends BoundedSource<String>> split(final long desiredBundleSizeBytes,
                final PipelineOptions options) throws Exception {
            validator.run();
            return null;
        }

        @Override
        public long getEstimatedSizeBytes(final PipelineOptions options) throws Exception {
            validator.run();
            return 0;
        }

        @Override
        public BoundedReader<String> createReader(final PipelineOptions options) throws IOException {
            validator.run();
            return null;
        }

        @Override
        public void setValidator(final Runnable runnable) {
            this.validator = runnable;
        }

        @Override
        public Coder<String> getDefaultOutputCoder() {
            return StringUtf8Coder.of();
        }
    }

    private interface Scenario {

        void execute(BeamIOTransformer transformer, ClassLoader componentLoader) throws Exception;
    }
}
