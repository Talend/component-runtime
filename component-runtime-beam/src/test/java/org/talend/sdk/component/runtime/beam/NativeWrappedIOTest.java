/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.beam;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;
import javax.json.spi.JsonProvider;

import org.apache.beam.sdk.io.jdbc.JdbcIO;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.xbean.finder.filter.Filter;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.beam.coder.JsonpJsonObjectCoder;
import org.talend.sdk.component.runtime.manager.ComponentFamilyMeta;
import org.talend.sdk.component.runtime.manager.ComponentManager;
import org.talend.sdk.component.runtime.manager.ContainerComponentRegistry;
import org.talend.sdk.component.runtime.manager.ParameterMeta;
import org.talend.sdk.component.runtime.manager.configuration.ConfigurationMapper;
import org.talend.sdk.component.runtime.manager.json.PreComputedJsonpProvider;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.test.wrapped.JdbcSource;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

public class NativeWrappedIOTest {

    @ClassRule
    public static final SimpleComponentRule COMPONENTS =
            new SimpleComponentRule(JdbcSource.class.getPackage().getName())
                    .withIsolatedPackage(JdbcSource.class.getPackage().getName(), JdbcIO.class.getPackage().getName());

    @Rule
    public transient final TestPipeline pipeline = TestPipeline.create();

    @Test
    public void dofn() {
        final JdbcSource.Config config = new JdbcSource.Config();
        config.setDriver("org.hsqldb.jdbcDriver");
        config.setUrl("jdbc:hsqldb:mem:foo");
        config.setUsername("sa");
        config.setPassword("");
        config.setQuery("SELECT * FROM   INFORMATION_SCHEMA.TABLES");
        final Map<String, String> map = SimpleFactory.configurationByExample().forInstance(config).configured().toMap();
        final String plugin = COMPONENTS.getTestPlugins().iterator().next();
        final PTransform<PBegin, PCollection<JsonObject>> jdbc = PTransform.class
                .cast(COMPONENTS
                        .asManager()
                        .createComponent("Jdbc", "Input", ComponentManager.ComponentType.MAPPER, 1, map)
                        .orElseThrow(() -> new IllegalArgumentException("no jdbc input")));
        PAssert
                .that(pipeline.apply(jdbc).setCoder(JsonpJsonObjectCoder.of(plugin)))
                .satisfies((SerializableFunction<Iterable<JsonObject>, Void>) input -> {
                    final JsonObject next = input.iterator().next();
                    assertEquals("PUBLIC", next.getString("TABLE_CATALOG"));
                    return null;
                });
        pipeline.run().waitUntilFinish();
    }

    @Test
    public void source() {
        final String plugin = COMPONENTS.getTestPlugins().iterator().next();
        final PTransform<PBegin, PCollection<JsonObject>> jdbc = PTransform.class
                .cast(COMPONENTS
                        .asManager()
                        .createComponent("beamtest", "source", ComponentManager.ComponentType.MAPPER, 1, emptyMap())
                        .orElseThrow(() -> new IllegalArgumentException("no beamtest#source component")));
        PAssert
                .that(pipeline.apply(jdbc).setCoder(JsonpJsonObjectCoder.of(plugin)))
                .satisfies((SerializableFunction<Iterable<JsonObject>, Void>) input -> {
                    assertEquals("test", input.iterator().next().getString("id"));
                    return null;
                });
        pipeline.run().waitUntilFinish();
    }

    @Slf4j // forked to avoid a dependency cycle
    public static class SimpleComponentRule implements TestRule {

        private static final Local<State> STATE = new Local<>();

        private final ThreadLocal<PreState> initState = ThreadLocal.withInitial(PreState::new);

        private String packageName;

        private Collection<String> isolatedPackages;

        private SimpleComponentRule withIsolatedPackage(final String packageName, final String... packages) {
            isolatedPackages = Stream
                    .concat(Stream.of(packageName), Stream.of(packages))
                    .filter(Objects::nonNull)
                    .collect(toList());
            if (isolatedPackages.isEmpty()) {
                isolatedPackages = null;
            }
            return this;
        }

        public EmbeddedComponentManager start() {
            final EmbeddedComponentManager embeddedComponentManager = new EmbeddedComponentManager(packageName) {

                @Override
                protected boolean isContainerClass(final Filter filter, final String name) {
                    if (name == null) {
                        return super.isContainerClass(filter, null);
                    }
                    return (isolatedPackages == null || isolatedPackages.stream().noneMatch(name::startsWith))
                            && super.isContainerClass(filter, name);
                }

                @Override
                public void close() {
                    try {
                        final State state = STATE.get();
                        if (state.jsonb != null) {
                            try {
                                state.jsonb.close();
                            } catch (final Exception e) {
                                // no-op: not important
                            }
                        }
                        STATE.remove();
                        initState.remove();
                    } finally {
                        super.close();
                    }
                }
            };

            STATE
                    .set(new State(embeddedComponentManager, new ArrayList<>(), initState.get().emitter, null, null,
                            null, null));
            return embeddedComponentManager;
        }

        private <T> ComponentFamilyMeta.BaseMeta<? extends Lifecycle> findMeta(final Class<T> componentType) {
            return asManager()
                    .find(c -> c.get(ContainerComponentRegistry.class).getComponents().values().stream())
                    .flatMap(f -> Stream
                            .concat(f.getProcessors().values().stream(), f.getPartitionMappers().values().stream()))
                    .filter(m -> m.getType().getName().equals(componentType.getName()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No component " + componentType));
        }

        private ComponentManager asManager() {
            return STATE.get().manager;
        }

        private Set<String> getTestPlugins() {
            return new HashSet<>(EmbeddedComponentManager.class.cast(asManager()).testPlugins);
        }

        static class PreState {

            Iterator<?> emitter;
        }

        @AllArgsConstructor
        protected static class State {

            final ComponentManager manager;

            final Collection<Object> collector;

            Iterator<?> emitter;

            volatile Jsonb jsonb;

            volatile JsonProvider jsonProvider;

            volatile JsonBuilderFactory jsonBuilderFactory;

            volatile RecordBuilderFactory recordBuilderFactory;

            synchronized Jsonb jsonb() {
                if (jsonb == null) {
                    jsonb = manager
                            .getJsonbProvider()
                            .create()
                            .withProvider(new PreComputedJsonpProvider("test", manager.getJsonpProvider(),
                                    manager.getJsonpParserFactory(), manager.getJsonpWriterFactory(),
                                    manager.getJsonpBuilderFactory(), manager.getJsonpGeneratorFactory(),
                                    manager.getJsonpReaderFactory())) // reuses the same memory buffers
                            .withConfig(new JsonbConfig().setProperty("johnzon.cdi.activated", false))
                            .build();
                }
                return jsonb;
            }

            synchronized JsonProvider jsonProvider() {
                if (jsonProvider == null) {
                    jsonProvider = manager.getJsonpProvider();
                }
                return jsonProvider;
            }

            synchronized JsonBuilderFactory jsonBuilderFactory() {
                if (jsonBuilderFactory == null) {
                    jsonBuilderFactory = manager.getJsonpBuilderFactory();
                }
                return jsonBuilderFactory;
            }

            synchronized RecordBuilderFactory recordBuilderFactory() {
                if (recordBuilderFactory == null) {
                    recordBuilderFactory = manager.getRecordBuilderFactoryProvider().apply("test");
                }
                return recordBuilderFactory;
            }
        }

        public static class EmbeddedComponentManager extends ComponentManager {

            private final ComponentManager oldInstance;

            private final List<String> testPlugins;

            private EmbeddedComponentManager(final String componentPackage) {
                super(findM2(), "TALEND-INF/dependencies.txt", "org.talend.sdk.component:type=component,value=%s");
                testPlugins = addJarContaining(Thread.currentThread().getContextClassLoader(),
                        componentPackage.replace('.', '/'));
                oldInstance = ComponentManager.contextualInstance().get();
                ComponentManager.contextualInstance().set(this);
            }

            @Override
            public void close() {
                try {
                    super.close();
                } finally {
                    ComponentManager.contextualInstance().compareAndSet(this, oldInstance);
                }
            }

            @Override
            protected boolean isContainerClass(final Filter filter, final String name) {
                // embedded mode (no plugin structure) so just run with all classes in parent classloader
                return true;
            }
        }

        private static class Local<T> {

            private final AtomicReference<T> state = new AtomicReference<>();

            private void set(final T value) {
                state.set(value);
            }

            private T get() {
                return state.get();
            }

            private void remove() {
                state.set(null);
            }
        }

        private SimpleComponentRule(final String packageName) {
            this.packageName = packageName;
        }

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    try (final EmbeddedComponentManager manager = start()) {
                        base.evaluate();
                    }
                }
            };
        }
    }

    static class SimpleFactory { // forked to avoid a dependency cycle

        static SimpleFactory.ByExample configurationByExample() {
            return new SimpleFactory.ByExample();
        }

        private static class SimpleParameterModelService extends ParameterModelService {

            private SimpleParameterModelService() {
                super(new PropertyEditorRegistry());
            }

            private ParameterMeta build(final String name, final String prefix, final Type genericType,
                    final Annotation[] annotations, final Collection<String> i18nPackages) {
                return super.buildParameter(name, prefix, null, genericType, annotations, i18nPackages, false,
                        new BaseParameterEnricher.Context(new LocalConfigurationService(emptyList(), "test")));
            }
        }

        @NoArgsConstructor(access = PRIVATE)
        private static class ByExample {

            private Object instance;

            private <T> SimpleFactory.ByExample forInstance(final T instance) {
                this.instance = instance;
                return this;
            }

            private SimpleFactory.ConfigurationByExample configured() {
                return new SimpleFactory.ConfigurationByExample(this);
            }
        }

        @AllArgsConstructor(access = PRIVATE)
        private static class ConfigurationByExample {

            private static final ConfigurationMapper CONFIGURATION_MAPPER = new ConfigurationMapper();

            private final SimpleFactory.ByExample byExample;

            private Map<String, String> toMap() {
                if (byExample.instance == null) {
                    return emptyMap();
                }
                final ParameterMeta params = new SimpleFactory.SimpleParameterModelService()
                        .build("configuration.", "configuration.", byExample.instance.getClass(), new Annotation[0],
                                new ArrayList<>(singletonList(byExample.instance.getClass().getPackage().getName())));
                return CONFIGURATION_MAPPER.map(params.getNestedParameters(), byExample.instance);
            }
        }
    }
}
