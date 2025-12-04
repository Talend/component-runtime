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
package org.talend.sdk.component.runtime.manager;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.list;
import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.xbean.finder.archive.FileArchive.decode;
import static org.talend.sdk.component.classloader.ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY;
import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;
import static org.talend.sdk.component.runtime.manager.ComponentManager.ComponentType.DRIVER_RUNNER;
import static org.talend.sdk.component.runtime.manager.ComponentManager.ComponentType.MAPPER;
import static org.talend.sdk.component.runtime.manager.ComponentManager.ComponentType.PROCESSOR;
import static org.talend.sdk.component.runtime.manager.reflect.Constructors.findConstructor;
import static org.talend.sdk.component.runtime.manager.util.Lazy.lazy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.instrument.ClassFileTransformer;
import java.lang.management.ManagementFactory;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.json.JsonArray;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriterFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParserFactory;

import org.apache.xbean.asm9.Type;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.ClassFinder;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.apache.xbean.finder.archive.ClasspathArchive;
import org.apache.xbean.finder.archive.CompositeArchive;
import org.apache.xbean.finder.archive.FileArchive;
import org.apache.xbean.finder.archive.FilteredArchive;
import org.apache.xbean.finder.archive.JarArchive;
import org.apache.xbean.finder.filter.ContainsFilter;
import org.apache.xbean.finder.filter.ExcludeIncludeFilter;
import org.apache.xbean.finder.filter.Filter;
import org.apache.xbean.finder.filter.FilterList;
import org.apache.xbean.finder.filter.Filters;
import org.apache.xbean.finder.filter.IncludeExcludeFilter;
import org.apache.xbean.finder.filter.PrefixFilter;
import org.apache.xbean.finder.util.Files;
import org.apache.xbean.propertyeditor.Converter;
import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.injector.Injector;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.standalone.DriverRunner;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.classloader.ThreadHelper;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerListener;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.dependencies.EmptyResolver;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.jmx.JmxManager;
import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.base.Lifecycle;
import org.talend.sdk.component.runtime.impl.Mode;
import org.talend.sdk.component.runtime.input.CheckpointState;
import org.talend.sdk.component.runtime.input.LocalPartitionMapper;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.input.PartitionMapperImpl;
import org.talend.sdk.component.runtime.internationalization.InternationalizationServiceFactory;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.manager.builtinparams.MaxBatchSizeParamBuilder;
import org.talend.sdk.component.runtime.manager.builtinparams.StreamingLongParamBuilder.StreamingMaxDurationMsParamBuilder;
import org.talend.sdk.component.runtime.manager.builtinparams.StreamingLongParamBuilder.StreamingMaxRecordsParamBuilder;
import org.talend.sdk.component.runtime.manager.extension.ComponentContextImpl;
import org.talend.sdk.component.runtime.manager.extension.ComponentContexts;
import org.talend.sdk.component.runtime.manager.json.TalendAccessMode;
import org.talend.sdk.component.runtime.manager.proxy.JavaProxyEnricherFactory;
import org.talend.sdk.component.runtime.manager.reflect.ComponentMetadataService;
import org.talend.sdk.component.runtime.manager.reflect.IconFinder;
import org.talend.sdk.component.runtime.manager.reflect.MigrationHandlerFactory;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.DefaultServiceProvider;
import org.talend.sdk.component.runtime.manager.service.MavenRepositoryDefaultResolver;
import org.talend.sdk.component.runtime.manager.service.ServiceHelper;
import org.talend.sdk.component.runtime.manager.service.api.ComponentInstantiator;
import org.talend.sdk.component.runtime.manager.service.record.RecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.manager.spi.ContainerListenerExtension;
import org.talend.sdk.component.runtime.manager.util.Lazy;
import org.talend.sdk.component.runtime.manager.util.LazyMap;
import org.talend.sdk.component.runtime.manager.xbean.KnownClassesFilter;
import org.talend.sdk.component.runtime.manager.xbean.NestedJarArchive;
import org.talend.sdk.component.runtime.manager.xbean.registry.EnrichedPropertyEditorRegistry;
import org.talend.sdk.component.runtime.output.ProcessorImpl;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.serialization.LightContainer;
import org.talend.sdk.component.runtime.standalone.DriverRunnerImpl;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;
import org.talend.sdk.component.spi.component.ComponentExtension;
import org.talend.sdk.component.spi.component.GenericComponentExtension;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ComponentManager implements AutoCloseable {

    private static class SingletonHolder {

        protected static final AtomicReference<ComponentManager> CONTEXTUAL_INSTANCE = new AtomicReference<>();

        private static ComponentManager buildNewComponentManager() {
            final Thread shutdownHook = SingletonHolder.buildShutDownHook();
            ComponentManager componentManager = new ComponentManager(findM2()) {

                private final AtomicBoolean closed = new AtomicBoolean(false);

                {
                    info("ComponentManager version: " + ComponentManagerVersion.VERSION);
                    info("Creating the contextual ComponentManager instance " + getIdentifiers());

                    parallelIf(Boolean.getBoolean("talend.component.manager.plugins.parallel"),
                            container.getDefinedNestedPlugin().stream().filter(p -> !hasPlugin(p)))
                            .forEach(this::addPlugin);
                    info("Components: " + availablePlugins());
                }

                @Override
                public void close() {
                    log.debug("Closing ComponentManager.");
                    if (!closed.compareAndSet(false, true)) {
                        log.debug("ComponentManager already closed");
                        return;
                    }
                    try {
                        synchronized (CONTEXTUAL_INSTANCE) {
                            if (CONTEXTUAL_INSTANCE.compareAndSet(this, null)) {
                                try {
                                    log.debug("ComponentManager : remove shutdown hook");
                                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                                } catch (final IllegalStateException ise) {
                                    // already shutting down
                                }
                            }
                        }
                    } finally {
                        CONTEXTUAL_INSTANCE.set(null);
                        super.close();
                        info("Released the contextual ComponentManager instance " + getIdentifiers());
                    }
                }

                Object readResolve() throws ObjectStreamException {
                    return new SerializationReplacer();
                }
            };

            try {
                Runtime.getRuntime().addShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                log.warn("addShutdownHook: Shutdown already in progress.");
            }
            componentManager.info("Created the contextual ComponentManager instance " + getIdentifiers());
            if (!CONTEXTUAL_INSTANCE.compareAndSet(null, componentManager)) { // unlikely it fails in a synch block
                componentManager = CONTEXTUAL_INSTANCE.get();
            }
            return componentManager;
        }

        private static synchronized ComponentManager renew(final ComponentManager current) {
            final ComponentManager manager;
            if (current == null) {
                log.info("rebuild new component manager");
                manager = SingletonHolder.buildNewComponentManager();
            } else {
                manager = current;
            }
            return manager;
        }

        private static Thread buildShutDownHook() {
            return new Thread(ComponentManager.class.getName() + "-" + ComponentManager.class.hashCode()) {

                @Override
                public void run() {
                    ofNullable(CONTEXTUAL_INSTANCE.get()).ifPresent(ComponentManager::close);
                }
            };
        }

        static {
            ComponentManager manager = SingletonHolder.buildNewComponentManager();
        }

    }

    private static final Components DEFAULT_COMPONENT = new Components() {

        @Override
        public Class<? extends Annotation> annotationType() {
            return Components.class;
        }

        @Override
        public String family() {
            return "";
        }

        @Override
        public String[] categories() {
            return new String[] { "Misc" };
        }
    };

    @Getter
    protected final ContainerManager container;

    // tcomp (org.talend + javax.annotation + jsonp) + logging (slf4j) are/can be provided service
    // + tcomp "runtime" indeed (invisible from the components but required for the runtime
    private final Filter classesFilter;

    private final Filter resourcesFilter;

    private final ParameterModelService parameterModelService;

    private final InternationalizationServiceFactory internationalizationServiceFactory;

    @Getter
    private final JsonProvider jsonpProvider;

    @Getter
    private final JsonGeneratorFactory jsonpGeneratorFactory;

    @Getter
    private final JsonReaderFactory jsonpReaderFactory;

    @Getter
    private final JsonBuilderFactory jsonpBuilderFactory;

    @Getter
    private final JsonParserFactory jsonpParserFactory;

    @Getter
    private final JsonWriterFactory jsonpWriterFactory;

    @Getter
    private final JsonbProvider jsonbProvider;

    private final ProxyGenerator proxyGenerator = new ProxyGenerator();

    private final JavaProxyEnricherFactory javaProxyEnricherFactory = new JavaProxyEnricherFactory();

    // kind of extracted to ensure we can switch it later if needed
    // (like using a Spring/CDI context and something else than xbean)
    private final ReflectionService reflections;

    @Getter // for extensions
    private final MigrationHandlerFactory migrationHandlerFactory;

    private final Collection<ComponentExtension> extensions;

    private final Collection<ClassFileTransformer> transformers;

    private final Collection<LocalConfiguration> localConfigurations;

    // org.slf4j.event but https://issues.apache.org/jira/browse/MNG-6360
    private final Level logInfoLevelMapping;

    @Getter // use with caution
    private final List<Customizer> customizers;

    @Getter
    private final Function<String, RecordBuilderFactory> recordBuilderFactoryProvider;

    @Getter
    private final JsonbConfig jsonbConfig = new JsonbConfig()
            .withBinaryDataStrategy(BinaryDataStrategy.BASE_64)
            .setProperty("johnzon.cdi.activated", false)
            .setProperty("johnzon.accessModeDelegate", new TalendAccessMode());

    private final EnrichedPropertyEditorRegistry propertyEditorRegistry;

    private final List<ContainerClasspathContributor> classpathContributors;

    private final IconFinder iconFinder = new IconFinder();

    private final DefaultServiceProvider defaultServiceProvider;

    private final ReentrantReadWriteLock containerLock = new ReentrantReadWriteLock();

    public ComponentManager(final File m2) {
        this(m2.toPath());
    }

    public ComponentManager(final File m2, final String dependenciesResource, final String jmxNamePattern) {
        this(m2.toPath(), dependenciesResource, jmxNamePattern);
    }

    public ComponentManager(final Path m2) {
        this(m2, "TALEND-INF/dependencies.txt", "org.talend.sdk.component:type=component,value=%s");
    }

    /**
     * @param m2 the maven repository location if on the file system.
     * @param dependenciesResource the resource path containing dependencies.
     * @param jmxNamePattern a pattern to register the plugins (containers) in JMX, null
     * otherwise.
     */
    public ComponentManager(final Path m2, final String dependenciesResource, final String jmxNamePattern) {
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        internationalizationServiceFactory = new InternationalizationServiceFactory(getLocalSupplier());
        customizers = toStream(loadServiceProviders(Customizer.class, tccl)).collect(toList()); // must stay first
        if (!customizers.isEmpty()) {
            customizers.forEach(c -> c.setCustomizers(customizers));
        }
        if (!Boolean.getBoolean("talend.component.manager.classpathcontributor.skip")) {
            classpathContributors =
                    toStream(loadServiceProviders(ContainerClasspathContributor.class, tccl)).collect(toList());
        } else {
            classpathContributors = emptyList();
        }
        classesFilter = new FilterList(Stream.concat(
                Stream.of("org.talend.sdk.component.api.",
                        "org.talend.sdk.component.spi.",
                        "javax.annotation.",
                        "javax.json.",
                        "org.talend.sdk.component.classloader.",
                        "org.talend.sdk.component.runtime.",
                        "org.talend.sdk.component.container.",
                        "org.talend.sdk.component.dependencies.",
                        "org.slf4j.",
                        "org.apache.johnzon."),
                additionalContainerClasses())
                .distinct()
                .map(PrefixFilter::new)
                .toArray(Filter[]::new));

        resourcesFilter = new FilterList(Stream.concat(
                Stream.of("META-INF/services/"),
                additionalParentResources())
                .distinct()
                .map(ContainsFilter::new)
                .toArray(Filter[]::new));

        jsonpProvider = loadJsonProvider();
        jsonbProvider = loadJsonbProvider();
        // these factories have memory caches so ensure we reuse them properly
        jsonpGeneratorFactory = JsonGeneratorFactory.class
                .cast(javaProxyEnricherFactory
                        .asSerializable(tccl, null, JsonGeneratorFactory.class.getName(),
                                jsonpProvider.createGeneratorFactory(emptyMap())));
        jsonpReaderFactory = JsonReaderFactory.class
                .cast(javaProxyEnricherFactory
                        .asSerializable(tccl, null, JsonReaderFactory.class.getName(),
                                jsonpProvider.createReaderFactory(emptyMap())));
        jsonpBuilderFactory = JsonBuilderFactory.class
                .cast(javaProxyEnricherFactory
                        .asSerializable(tccl, null, JsonBuilderFactory.class.getName(),
                                jsonpProvider.createBuilderFactory(emptyMap())));
        jsonpParserFactory = JsonParserFactory.class
                .cast(javaProxyEnricherFactory
                        .asSerializable(tccl, null, JsonParserFactory.class.getName(),
                                jsonpProvider.createParserFactory(emptyMap())));
        jsonpWriterFactory = JsonWriterFactory.class
                .cast(javaProxyEnricherFactory
                        .asSerializable(tccl, null, JsonWriterFactory.class.getName(),
                                jsonpProvider.createWriterFactory(emptyMap())));

        logInfoLevelMapping = findLogInfoLevel();

        propertyEditorRegistry = createPropertyEditorRegistry();
        localConfigurations = createRawLocalConfigurations();
        parameterModelService = new ParameterModelService(propertyEditorRegistry);
        reflections = new ReflectionService(parameterModelService, propertyEditorRegistry);
        migrationHandlerFactory = new MigrationHandlerFactory(reflections);

        final Predicate<String> isContainerClass = name -> isContainerClass(classesFilter, name);
        final Predicate<String> isParentResource = name -> isContainerResource(resourcesFilter, name);
        final ContainerManager.ClassLoaderConfiguration defaultClassLoaderConfiguration =
                ContainerManager.ClassLoaderConfiguration
                        .builder()
                        .parent(tccl)
                        .parentClassesFilter(isContainerClass)
                        .classesFilter(isContainerClass.negate())
                        .supportsResourceDependencies(true)
                        .parentResourcesFilter(isParentResource)
                        .create();
        this.container = new ContainerManager(ContainerManager.DependenciesResolutionConfiguration
                .builder()
                .resolver(customizers.stream().noneMatch(Customizer::ignoreDefaultDependenciesDescriptor)
                        ? new MvnDependencyListLocalRepositoryResolver(dependenciesResource, this::resolve)
                        : new EmptyResolver())
                .rootRepositoryLocation(m2)
                .create(), defaultClassLoaderConfiguration, container -> {
                }, logInfoLevelMapping) {

            @Override
            public Path resolve(final String path) {
                return classpathContributors
                        .stream()
                        .filter(it -> it.canResolve(path))
                        .map(it -> it.resolve(path))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElseGet(() -> super.resolve(path));
            }
        };
        this.container.registerListener(new Updater(dependenciesResource));
        if (!Boolean.getBoolean("talend.component.manager.jmx.skip")) {
            ofNullable(jmxNamePattern)
                    .map(String::trim)
                    .filter(n -> !n.isEmpty())
                    .ifPresent(p -> this.container
                            .registerListener(
                                    new JmxManager(container, p, ManagementFactory.getPlatformMBeanServer())));
        }
        toStream(loadServiceProviders(ContainerListenerExtension.class, tccl))
                .peek(e -> e.setComponentManager(ComponentManager.this))
                .sorted(comparing(ContainerListenerExtension::order))
                .forEach(container::registerListener);
        this.extensions = toStream(loadServiceProviders(ComponentExtension.class, tccl))
                .filter(ComponentExtension::isActive)
                .sorted(comparing(ComponentExtension::priority))
                .collect(toList());
        this.transformers = extensions.stream().flatMap(e -> e.getTransformers().stream()).collect(toList());

        final Iterator<RecordBuilderFactoryProvider> recordBuilderFactoryIterator =
                ServiceLoader.load(RecordBuilderFactoryProvider.class, tccl).iterator();
        if (recordBuilderFactoryIterator.hasNext()) {
            final RecordBuilderFactoryProvider factory = recordBuilderFactoryIterator.next();
            recordBuilderFactoryProvider = factory::apply;
            if (recordBuilderFactoryIterator.hasNext()) {
                throw new IllegalArgumentException(
                        "Ambiguous recordBuilderFactory: " + factory + "/" + recordBuilderFactoryIterator.next());
            }
        } else {
            recordBuilderFactoryProvider = RecordBuilderFactoryImpl::new;
        }

        this.defaultServiceProvider = new DefaultServiceProvider(reflections, jsonpProvider, jsonpGeneratorFactory,
                jsonpReaderFactory, jsonpBuilderFactory, jsonpParserFactory, jsonpWriterFactory, jsonbConfig,
                jsonbProvider, proxyGenerator, javaProxyEnricherFactory, localConfigurations,
                recordBuilderFactoryProvider, propertyEditorRegistry);
    }

    private JsonbProvider loadJsonbProvider() {
        try {
            return new org.apache.johnzon.jsonb.JohnzonProvider();
        } catch (final RuntimeException re) {
            return JsonbProvider.provider();
        }
    }

    private JsonProvider loadJsonProvider() {
        try {
            return new org.apache.johnzon.core.JsonProviderImpl();
        } catch (final RuntimeException re) {
            return JsonProvider.provider();
        }
    }

    protected Supplier<Locale> getLocalSupplier() {
        return Locale::getDefault;
    }

    private Path resolve(final String artifact) {
        return container.resolve(artifact);
    }

    private EnrichedPropertyEditorRegistry createPropertyEditorRegistry() {
        return new EnrichedPropertyEditorRegistry();
    }

    private Level findLogInfoLevel() {
        if (Boolean.getBoolean("talend.component.manager.log.info")) {
            return Level.INFO;
        }
        try {
            ComponentManager.class.getClassLoader().loadClass("routines.TalendString");
            return Level.FINE;
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            return Level.INFO;
        }
    }

    /**
     * Creates a default manager with default maven local repository,
     * TALEND-INF/dependencies.txt file to find the dependencies of the plugins and
     * a default JMX pattern for plugins. It also adds the caller as a plugin.
     *
     * @return the contextual manager instance.
     */
    public static ComponentManager instance() {
        return SingletonHolder.CONTEXTUAL_INSTANCE.updateAndGet(SingletonHolder::renew);
    }

    /**
     * For test purpose only.
     *
     * @return the contextual instance
     */
    protected static AtomicReference<ComponentManager> contextualInstance() {
        return SingletonHolder.CONTEXTUAL_INSTANCE;
    }

    private static <T> Stream<T> parallelIf(final boolean condition, final Stream<T> stringStream) {
        return condition ? stringStream.parallel() : stringStream;
    }

    protected void info(final String msg) {
        switch (logInfoLevelMapping.intValue()) {
            case 500: // FINE
                log.debug(msg);
                break;
            case 800: // INFo
            default:
                log.info(msg);
        }
    }

    private Stream<String> additionalContainerClasses() {
        return Stream
                .concat(customizers.stream().flatMap(Customizer::containerClassesAndPackages),
                        ofNullable(
                                System.getProperty("talend.component.manager.classloader.container.classesAndPackages"))
                                .map(s -> s.split(","))
                                .map(Stream::of)
                                .orElseGet(Stream::empty));
    }

    private Stream<String> additionalParentResources() {
        return Stream
                .concat(customizers.stream().flatMap(Customizer::parentResources),
                        ofNullable(
                                System.getProperty("talend.component.manager.classloader.container.parentResources"))
                                .map(s -> s.split(","))
                                .map(Stream::of)
                                .orElseGet(Stream::empty));
    }

    public static Path findM2() {
        return new MavenRepositoryDefaultResolver().discover();
    }

    private static String getIdentifiers() {
        return "(classloader=" + ComponentManager.class.getClassLoader() + ", jvm="
                + ManagementFactory.getRuntimeMXBean().getName() + ")";
    }

    private static <T> Stream<T> toStream(final Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.IMMUTABLE), false);
    }

    private static <T> Iterator<T> loadServiceProviders(final Class<T> service, final ClassLoader classLoader) {
        return ServiceLoader.load(service, classLoader).iterator();
    }

    private static Path toFile(final String classFileName, final URL url) {
        String path = url.getFile();
        path = path.substring(0, path.length() - classFileName.length());
        return PathFactory.get(decode(path));
    }

    public void addCallerAsPlugin() {
        try {
            final ClassLoader tmpLoader = ofNullable(Thread.currentThread().getContextClassLoader())
                    .orElseGet(ClassLoader::getSystemClassLoader);
            final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            final Class<?> jarMarker = tmpLoader
                    .loadClass(Stream
                            .of(stackTrace)
                            .filter(c -> !c.getClassName().startsWith("org.talend.sdk.component.runtime.manager.")
                                    && !c.getClassName().startsWith("org.talend.sdk.component.runtime.beam.dsl.")
                                    && !c.getClassName().startsWith("org.talend.sdk.component.runtime.standalone.")
                                    && !c.getClassName().startsWith("org.talend.sdk.component.runtime.avro.")
                                    && !c.getClassName().startsWith("org.talend.daikon.")
                                    && !c.getClassName().startsWith("org.talend.designer.")
                                    && !c.getClassName().startsWith("org.eclipse.")
                                    && !c.getClassName().startsWith("java.") && !c.getClassName().startsWith("javax.")
                                    && !c.getClassName().startsWith("sun.") && !c.getClassName().startsWith("com.sun.")
                                    && !c.getClassName().startsWith("com.oracle."))
                            .findFirst()
                            .map(StackTraceElement::getClassName)
                            .orElse(ComponentManager.class.getName()));
            if (jarMarker == ComponentManager.class) {
                return;
            }

            addJarContaining(tmpLoader, jarMarker.getName().replace(".", "/") + ".class");
        } catch (final ClassNotFoundException e) {
            // ignore, not important. if we can't do it then the plugins should be
            // registered normally
        }
    }

    protected List<String> addJarContaining(final ClassLoader loader, final String resource) {
        final URL url = loader.getResource(resource);
        if (url != null) {
            Path plugin = null;
            switch (url.getProtocol()) {
                case "bundleresource": // studio on equinox, this is the definition part so we don't register it
                    break;
                case "file":
                    plugin = toFile(resource, url);
                    break;
                case "jar":
                    if (url.getPath() != null && url.getPath().startsWith("mvn:")) { // pax mvn
                        // studio temporary integration, to drop when studio integrates correctly tcomp
                        break;
                    }
                    final String spec = url.getFile();
                    final int separator = spec.indexOf('!');
                    if (separator > 0) {
                        try {
                            plugin = PathFactory.get(decode(new URL(spec.substring(0, separator)).getFile()));
                        } catch (final MalformedURLException e) {
                            // no-op
                        }
                    }
                    break;
                default:
            }
            if (plugin == null) {
                log.warn("Can't find " + url);
                return null;
            }
            return Stream
                    .of(plugin)
                    // just a small workaround for maven
                    .flatMap(this::toPluginLocations)
                    .filter(path -> !findPlugin(path.getFileName().toString()).isPresent())
                    .map(file -> {
                        final String id = addPlugin(file.toAbsolutePath().toString());
                        if (findPlugin(id).get().get(ContainerComponentRegistry.class).getComponents().isEmpty()) {
                            removePlugin(id);
                            return null;
                        }
                        return id;
                    })
                    .filter(Objects::nonNull)
                    .collect(toList());
        }
        return emptyList();
    }

    private Stream<Path> toPluginLocations(final Path src) {
        final String filename = src.getFileName().toString();
        if ("test-classes".equals(filename) && src.getParent() != null) { // maven
            return Stream.of(src.getParent().resolve("classes"), src);
        }

        if ("test".equals(filename) && src.getParent() != null
                && "java".equals(src.getParent().getFileName().toString())) {
            return Stream.of(src.getParent().resolve("main"), src).filter(java.nio.file.Files::exists);
        }

        return Stream.of(src);
    }

    public Map<String, String> mergeCheckpointConfiguration(final String plugin, final String name,
            final ComponentType componentType, final Map<String, String> configuration) {
        if (!MAPPER.equals(componentType)) {
            return configuration;
        }
        if (!Boolean.parseBoolean(System.getProperty("talend.checkpoint.enabled", "false"))) {
            return configuration;
        }
        final ParameterMeta checkpoint = findCheckpointParameterMeta(plugin, name);
        if (checkpoint == null) {
            return configuration;
        }
        return replaceKeys(configuration, CheckpointState.CHECKPOINT_KEY, checkpoint.getPath());
    }

    public ParameterMeta findConfigurationType(final String plugin, final String name, final String configurationType) {
        return findAll()
                .map(c -> c.get(ContainerComponentRegistry.class))
                .map(registry -> registry.findComponentFamily(plugin))
                .filter(Objects::nonNull)
                .map(family -> family.getPartitionMappers().get(name).getParameterMetas().get())
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .flatMap(np -> np.getNestedParameters().stream())
                .filter(m -> configurationType
                        .equals(m.getMetadata().getOrDefault("tcomp::configurationtype::type", "")))
                .findFirst()
                .orElse(null);
    }

    public ParameterMeta findCheckpointParameterMeta(final String plugin, final String name) {
        return findConfigurationType(plugin, name, "checkpoint");
    }

    public ParameterMeta findDatasetParameterMeta(final String plugin, final String name) {
        return findConfigurationType(plugin, name, "dataset");
    }

    public ParameterMeta findDatastoreParameterMeta(final String plugin, final String name) {
        return findConfigurationType(plugin, name, "datastore");
    }

    /**
     * Convert a json value to a configuration map.
     *
     * @param jsonValue json value to convert to a configuration map
     * @param path optional path to add to keys as prefix
     * @return a configuration map from a json value
     */
    public static Map<String, String> jsonToMap(final JsonValue jsonValue, final String path) {
        final Map<String, String> result = new HashMap<>();
        if (jsonValue instanceof JsonObject) {
            JsonObject jsonObj = (JsonObject) jsonValue;
            for (String key : jsonObj.keySet()) {
                String newPath = path.isEmpty() ? key : path + "." + key;
                result.putAll(jsonToMap(jsonObj.get(key), newPath));
            }
        } else if (jsonValue instanceof JsonArray) {
            JsonArray jsonArray = (JsonArray) jsonValue;
            for (int i = 0; i < jsonArray.size(); i++) {
                String newPath = path + "[" + i + "]";
                result.putAll(jsonToMap(jsonArray.get(i), newPath));
            }
        } else {
            String str;
            if (jsonValue.getValueType() == JsonValue.ValueType.STRING) {
                str = ((JsonString) (jsonValue)).getString();
            } else {
                str = jsonValue.toString();
            }
            result.put(path, str);
        }
        return result;
    }

    /**
     * Convert a json value to a configuration map.
     *
     * @param jsonValue json value to convert to a configuration map
     * @return a configuration map from a json value
     */
    public static Map<String, String> jsonToMap(final JsonValue jsonValue) {
        return jsonToMap(jsonValue, "");
    }

    /**
     * Replace some keys in the configuration map.
     *
     * @param configuration original configuration
     * @param oldPrefix old prefix to replace
     * @param newPrefix new prefix to replace with
     * @return Map with keys replaced
     */
    public static Map<String, String> replaceKeys(final Map<String, String> configuration, final String oldPrefix,
            final String newPrefix) {
        final Map<String, String> replaced = new HashMap<>();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith(oldPrefix)) {
                String newKey = newPrefix + key.substring(oldPrefix.length());
                replaced.put(newKey, value);
            } else {
                replaced.put(key, value);
            }
        }
        return replaced;
    }

    public <T> Stream<T> find(final Function<Container, Stream<T>> mapper) {
        return findAll().flatMap(mapper);
    }

    // really a DIY entry point for custom flow, it creates component instances but
    // nothing more
    public Optional<Object> createComponent(final String plugin, final String name, final ComponentType componentType,
            final int version, final Map<String, String> configuration) {
        return findComponentInternal(plugin, name, componentType, version, configuration)
                // unwrap to access the actual instance which is the desired one
                .map(i -> Delegated.class.isInstance(i) ? Delegated.class.cast(i).getDelegate() : i);
    }

    private Optional<Object> findComponentInternal(final String plugin, final String name,
            final ComponentType componentType, final int version, final Map<String, String> configuration) {
        autoDiscoverPluginsIfEmpty(false, true);

        final Map<String, String> conf = mergeCheckpointConfiguration(plugin, name, componentType, configuration);
        return find(pluginContainer -> Stream
                .of(findInstance(plugin, name, componentType, version, conf, pluginContainer)))
                .filter(Objects::nonNull)
                .findFirst();
    }

    public void autoDiscoverPluginsIfEmpty(final boolean callers, final boolean classpath) {
        if (hasPlugins()) {
            return;
        }

        final WriteLock writeLock = containerLock.writeLock();
        writeLock.lock();
        try {
            if (hasPlugins()) {
                return;
            }

            autoDiscoverPlugins0(callers, classpath);
        } finally {
            writeLock.unlock();
        }
    }

    public void autoDiscoverPlugins(final boolean callers, final boolean classpath) {
        final WriteLock writeLock = containerLock.writeLock();
        writeLock.lock();
        try {
            autoDiscoverPlugins0(callers, classpath);
        } finally {
            writeLock.unlock();
        }
    }

    private void autoDiscoverPlugins0(final boolean callers, final boolean classpath) {
        if (callers && !Boolean.getBoolean("component.manager.callers.skip")) {
            addCallerAsPlugin();
        }

        // common for studio until job generation is updated to build a tcomp friendly bundle
        if (classpath && !Boolean.getBoolean("component.manager.classpath.skip")) {
            try {
                final String markerValue = "TALEND-INF/dependencies.txt";
                final Enumeration<URL> componentMarkers =
                        Thread.currentThread().getContextClassLoader().getResources(markerValue);
                while (componentMarkers.hasMoreElements()) {
                    final URL marker = componentMarkers.nextElement();
                    File file = Files.toFile(marker);
                    if (file != null) {
                        if (file.getName().equals("dependencies.txt") && file.getParentFile() != null
                                && file.getParentFile().getName().equals("TALEND-INF")) {
                            file = file.getParentFile().getParentFile();
                        }
                        if (!hasPlugin(container.buildAutoIdFromName(file.getName()))) {
                            addPlugin(file.getAbsolutePath());
                        }
                    } else {
                        // lookup nested jar
                        if (marker != null && "jar".equals(marker.getProtocol())) {
                            final String urlFile = marker.getFile();
                            final String jarPath = urlFile.substring(0, urlFile.lastIndexOf("!"));
                            final String jarFilePath = jarPath.substring(jarPath.lastIndexOf("/") + 1);
                            if (!hasPlugin(container.buildAutoIdFromName(jarFilePath))) {
                                addPlugin(jarPath);
                            }
                        }
                    }
                }
            } catch (final IOException e) {
                // no-op
            }
        }
    }

    private Object findInstance(final String plugin, final String name, final ComponentType componentType,
            final int version, final Map<String, String> configuration, final Container pluginContainer) {
        return findGenericInstance(plugin, name, componentType, version, configuration, pluginContainer)
                .orElseGet(
                        () -> findDeployedInstance(plugin, name, componentType, version, configuration, pluginContainer)
                                .orElse(null));
    }

    private Optional<Object> findDeployedInstance(final String plugin, final String name,
            final ComponentType componentType, final int version, final Map<String, String> configuration,
            final Container pluginContainer) {

        final String pluginIdentifier = container.buildAutoIdFromName(plugin);

        final ComponentInstantiator.Builder builder = new ComponentInstantiator.BuilderDefault(
                () -> Stream.of(pluginContainer.get(ContainerComponentRegistry.class)));
        return ofNullable(
                builder.build(pluginIdentifier, ComponentInstantiator.MetaFinder.ofComponent(name), componentType))
                .map((ComponentInstantiator instantiator) -> instantiator.instantiate(configuration, version));
    }

    private Optional<Object> findGenericInstance(final String plugin, final String name,
            final ComponentType componentType, final int version, final Map<String, String> configuration,
            final Container pluginContainer) {
        return ofNullable(pluginContainer.get(GenericComponentExtension.class))
                .filter(ext -> ext.canHandle(componentType.runtimeType(), plugin, name))
                .map(ext -> ext.createInstance(componentType.runtimeType(), plugin, name, version, configuration,
                        ofNullable(pluginContainer.get(AllServices.class))
                                .map(AllServices::getServices)
                                .orElseGet(Collections::emptyMap)));
    }

    public Optional<Mapper> findMapper(final String plugin, final String name, final int version,
            final Map<String, String> configuration) {
        return findComponentInternal(plugin, name, MAPPER, version, configuration).map(Mapper.class::cast);
    }

    public Optional<org.talend.sdk.component.runtime.standalone.DriverRunner> findDriverRunner(final String plugin,
            final String name, final int version, final Map<String, String> configuration) {
        return findComponentInternal(plugin, name, DRIVER_RUNNER, version, configuration)
                .map(org.talend.sdk.component.runtime.standalone.DriverRunner.class::cast);
    }

    public Optional<org.talend.sdk.component.runtime.output.Processor> findProcessor(final String plugin,
            final String name, final int version, final Map<String, String> configuration) {
        return findComponentInternal(plugin, name, PROCESSOR, version, configuration)
                .map(org.talend.sdk.component.runtime.output.Processor.class::cast);
    }

    public boolean hasPlugin(final String plugin) {
        return findPlugin(plugin).isPresent();
    }

    public Optional<Container> findPlugin(final String plugin) {
        final ReadLock readLock = containerLock.readLock();
        readLock.lock();
        try {
            return container.find(plugin);
        } finally {
            readLock.unlock();
        }
    }

    public String addPlugin(final String pluginRootFile) {
        final WriteLock writeLock = containerLock.writeLock();
        writeLock.lock();
        try {
            final String pluginId = findPluginId(pluginRootFile);
            if (pluginId != null) {
                return pluginId;
            }

            final String id = this.container
                    .builder(pluginRootFile)
                    .withCustomizer(createContainerCustomizer(pluginRootFile))
                    .withAdditionalClasspath(findAdditionalClasspathFor(container.buildAutoIdFromName(pluginRootFile)))
                    .create()
                    .getId();
            info("Adding plugin: " + pluginRootFile + ", as " + id);
            return id;
        } finally {
            writeLock.unlock();
        }
    }

    private String findPluginId(final String pluginRootFile) {
        return findPlugin(pluginRootFile).map(Container::getId).orElse(null);
    }

    public String addWithLocationPlugin(final String location, final String pluginRootFile) {
        final String id = this.container
                .builder(pluginRootFile)
                .withCustomizer(createContainerCustomizer(location))
                .withAdditionalClasspath(findAdditionalClasspathFor(container.buildAutoIdFromName(location)))
                .create()
                .getId();
        info("Adding plugin: " + pluginRootFile + ", as " + id);
        return id;
    }

    protected String addPlugin(final String forcedId, final String pluginRootFile) {
        final String id = this.container
                .builder(forcedId, pluginRootFile)
                .withCustomizer(createContainerCustomizer(forcedId))
                .withAdditionalClasspath(findAdditionalClasspathFor(forcedId))
                .create()
                .getId();
        info("Adding plugin: " + pluginRootFile + ", as " + id);
        return id;
    }

    private Collection<Artifact> findAdditionalClasspathFor(final String pluginId) {
        return classpathContributors
                .stream()
                .flatMap(it -> it.findContributions(pluginId).stream())
                .distinct()
                .collect(toList())/* keep order */;
    }

    public void removePlugin(final String id) {
        findPlugin(id).ifPresent(Container::close);
        info("Removed plugin: " + id);
    }

    protected boolean isContainerClass(final Filter filter, final String name) {
        return name != null && filter.accept(name);
    }

    protected boolean isContainerResource(final Filter filter, final String name) {
        return name != null && filter.accept(name);
    }

    @Override
    public void close() {
        container.close();
        propertyEditorRegistry.close();
    }

    private Consumer<Container> createContainerCustomizer(final String originalId) {
        return c -> {
            c.set(OriginalId.class, new OriginalId(originalId));
            transformers.forEach(c::registerTransformer);
        };
    }

    private <T> T executeInContainer(final String plugin, final Supplier<T> supplier) {
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        thread
                .setContextClassLoader(
                        findPlugin(plugin).map(Container::getLoader).map(ClassLoader.class::cast).orElse(old));
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    private boolean hasPlugins() {
        final ReadLock readLock = containerLock.readLock();
        readLock.lock();
        try {
            return !container.findAll().isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    private Stream<Container> findAll() {
        final ReadLock readLock = containerLock.readLock();
        readLock.lock();
        try {
            return container.findAll().stream();
        } finally {
            readLock.unlock();
        }
    }

    public List<String> availablePlugins() {
        final ReadLock readLock = containerLock.readLock();
        readLock.lock();
        try {
            return container.getPluginsList();
        } finally {
            readLock.unlock();
        }
    }

    protected void containerServices(final Container container, final Map<Class<?>, Object> services) {
        // no-op
    }

    protected static Collection<LocalConfiguration> createRawLocalConfigurations() {
        final List<LocalConfiguration> configurations = new ArrayList<>(2);
        if (!Boolean.getBoolean("talend.component.manager.localconfiguration.skip")) {
            configurations
                    .addAll(toStream(
                            loadServiceProviders(LocalConfiguration.class, LocalConfiguration.class.getClassLoader()))
                            .collect(toList()));
        }
        configurations.addAll(asList(new LocalConfiguration() {

            @Override
            public String get(final String key) {
                return System.getProperty(key);
            }

            @Override
            public Set<String> keys() {
                return System.getProperties().stringPropertyNames();
            }
        }, new LocalConfiguration() {

            @Override
            public String get(final String key) {
                String val = System.getenv(key);
                if (val != null) {
                    return val;
                }
                String k = key.replaceAll("[^A-Za-z0-9]", "_");
                val = System.getenv(k);
                if (val != null) {
                    return val;
                }
                val = System.getenv(k.toUpperCase(ROOT));
                if (val != null) {
                    return val;
                }
                return null;
            }

            @Override
            public Set<String> keys() {
                return System.getenv().keySet();
            }
        }));
        return configurations;
    }

    private <T extends Annotation> T findComponentsConfig(final Map<String, AnnotatedElement> componentDefaults,
            final Class<?> type, final ConfigurableClassLoader loader, final Class<T> annotation,
            final T defaultValue) {

        final AnnotatedElement annotatedElement =
                componentDefaults.computeIfAbsent(getAnnotatedElementCacheKey(type), p -> {
                    if (p != null) {
                        String currentPackage = p;
                        do {
                            try {
                                final Class<?> pckInfo = loader.loadClass(currentPackage + ".package-info");
                                if (pckInfo.isAnnotationPresent(annotation)) {
                                    return pckInfo;
                                }
                            } catch (final ClassNotFoundException e) {
                                // no-op
                            }

                            final int endPreviousPackage = currentPackage.lastIndexOf('.');
                            if (endPreviousPackage < 0) { // we don't accept default package since it is not specific
                                // enough
                                break;
                            }

                            currentPackage = currentPackage.substring(0, endPreviousPackage);
                        } while (true);
                    }

                    return new AnnotatedElement() {

                        @Override
                        public <T extends Annotation> T getAnnotation(final Class<T> annotationClass) {
                            return annotationClass == annotation ? annotationClass.cast(defaultValue) : null;
                        }

                        @Override
                        public Annotation[] getAnnotations() {
                            return new Annotation[] { defaultValue };
                        }

                        @Override
                        public Annotation[] getDeclaredAnnotations() {
                            return getAnnotations();
                        }
                    };
                });
        return annotatedElement.getAnnotation(annotation);
    }

    private String getAnnotatedElementCacheKey(final Class<?> type) {
        return ofNullable(type.getPackage().getName()).orElse("");
    }

    private Function<Map<String, String>, Object[]> createParametersFactory(final String plugin,
            final Executable method, final Map<Class<?>, Object> services, final Supplier<List<ParameterMeta>> metas) {
        // it is "slow" for cold boots so let's delay it
        return config -> executeInContainer(plugin,
                lazy(() -> reflections.parameterFactory(method, services, metas == null ? null : metas.get())))
                .apply(config);
    }

    public enum ComponentType {

        MAPPER {

            @Override
            public Map<String, ? extends ComponentFamilyMeta.BaseMeta> findMeta(final ComponentFamilyMeta family) {
                return family.getPartitionMappers();
            }

            @Override
            Class<? extends Lifecycle> runtimeType() {
                return Mapper.class;
            }
        },
        PROCESSOR {

            @Override
            public Map<String, ? extends ComponentFamilyMeta.BaseMeta> findMeta(final ComponentFamilyMeta family) {
                return family.getProcessors();
            }

            @Override
            Class<? extends Lifecycle> runtimeType() {
                return org.talend.sdk.component.runtime.output.Processor.class;
            }
        },
        DRIVER_RUNNER {

            @Override
            public Map<String, ? extends ComponentFamilyMeta.BaseMeta> findMeta(final ComponentFamilyMeta family) {
                return family.getDriverRunners();
            }

            @Override
            Class<? extends Lifecycle> runtimeType() {
                return org.talend.sdk.component.runtime.standalone.DriverRunner.class;
            }
        };

        abstract public Map<String, ? extends ComponentFamilyMeta.BaseMeta> findMeta(ComponentFamilyMeta family);

        abstract Class<? extends Lifecycle> runtimeType();
    }

    @AllArgsConstructor
    private static class SerializationReplacer implements Serializable {

        Object readResolve() throws ObjectStreamException {
            return instance();
        }
    }

    @Data
    @AllArgsConstructor
    public static class AllServices {

        private final Map<Class<?>, Object> services;
    }

    @Data
    public static class OriginalId {

        private final String value;
    }

    @RequiredArgsConstructor
    private class Updater implements ContainerListener {

        private final String dependenciesResource;

        private final ModelVisitor visitor = new ModelVisitor();

        private final Collection<String> supportedAnnotations = Stream
                .of(Internationalized.class, Service.class, Request.class, PartitionMapper.class, Processor.class,
                        Emitter.class, DriverRunner.class)
                .map(Type::getDescriptor)
                .collect(toSet());

        @Override
        public void onCreate(final Container container) {
            final ConfigurableClassLoader loader = container.getLoader();
            final OriginalId originalId = OriginalId.class.cast(container.get(OriginalId.class));
            final Map<java.lang.reflect.Type, Optional<Converter>> xbeanConverterCache = new ConcurrentHashMap<>();

            final AnnotationFinder finder;
            Archive archive = null;
            final String rootModule = container.getRootModule();
            final boolean nested = rootModule != null && rootModule.startsWith("nested:");
            try {
                String alreadyScannedClasses = null;
                Filter filter = KnownClassesFilter.INSTANCE;
                try (final InputStream containerFilterConfig = nested
                        ? loader.getNestedResource(rootModule + "!/TALEND-INF/scanning.properties")
                        : loader.getResourceAsStream("TALEND-INF/scanning.properties")) {
                    if (containerFilterConfig != null) {
                        final Properties config = new Properties();
                        config.load(containerFilterConfig);
                        filter = createScanningFilter(config);
                        alreadyScannedClasses = config.getProperty("classes.list");
                    }
                } catch (final IOException e) {
                    log.debug(e.getMessage(), e);
                }

                AnnotationFinder optimizedFinder = null;
                if (alreadyScannedClasses != null
                        && !(alreadyScannedClasses = alreadyScannedClasses.trim()).isEmpty()) {
                    final List<? extends Class<?>> classes =
                            Stream.of(alreadyScannedClasses.split(",")).map(String::trim).map(it -> {
                                try {
                                    return loader.loadClass(it);
                                } catch (final ClassNotFoundException e) {
                                    throw new IllegalArgumentException(e);
                                }
                            }).collect(toList());
                    if (KnownClassesFilter.INSTANCE == filter) {
                        archive = new ClassesArchive(/* empty */);
                        optimizedFinder = new AnnotationFinder(archive) {

                            @Override
                            public List<Class<?>> findAnnotatedClasses(final Class<? extends Annotation> marker) {
                                return classes.stream().filter(c -> c.isAnnotationPresent(marker)).collect(toList());
                            }

                            @Override
                            public List<Method> findAnnotatedMethods(final Class<? extends Annotation> annotation) {
                                if (Request.class == annotation) { // optimized
                                    return classes
                                            .stream()
                                            .filter(HttpClient.class::isAssignableFrom)
                                            .flatMap(client -> Stream
                                                    .of(client.getMethods())
                                                    .filter(m -> m.isAnnotationPresent(annotation)))
                                            .collect(toList());
                                }
                                return super.findAnnotatedMethods(annotation);
                            }

                            // finder.findAnnotatedMethods(Request.class)
                        };
                    }
                } else {
                    /*
                     * container.findExistingClasspathFiles() - we just scan the root module for
                     * now, no need to scan all the world
                     */
                    archive = toArchive(container.getRootModule(), originalId, loader);
                }
                finder = optimizedFinder == null ? new AnnotationFinder(new FilteredArchive(archive, filter)) {

                    @Override
                    protected boolean cleanOnNaked() {
                        return true;
                    }

                    @Override
                    protected boolean isTracked(final String annotationType) {
                        return supportedAnnotations.contains(annotationType);
                    }
                } : optimizedFinder;
            } finally {
                if (AutoCloseable.class.isInstance(archive)) {
                    try {
                        AutoCloseable.class.cast(archive).close();
                    } catch (final Exception e) {
                        log.warn(e.getMessage());
                    }
                }
            }
            final ContainerComponentRegistry registry = new ContainerComponentRegistry();
            container.set(ContainerComponentRegistry.class, registry);

            final boolean isGeneric;
            final Iterator<GenericComponentExtension> genericExtension =
                    ServiceLoader.load(GenericComponentExtension.class, container.getLoader()).iterator();
            if (genericExtension.hasNext()) {
                final GenericComponentExtension first = genericExtension.next();
                container.set(GenericComponentExtension.class, first);
                isGeneric = true;
                if (genericExtension.hasNext()) {
                    throw new IllegalArgumentException("A component can't have two generic component extensions: "
                            + finder + ", " + genericExtension.next());
                }
            } else {
                isGeneric = false;
            }

            final AtomicReference<Map<Class<?>, Object>> seviceLookupRef = new AtomicReference<>();

            final ContainerManager containerManager = ComponentManager.this.getContainer();
            final Supplier<Stream<ContainerComponentRegistry>> registriesSupplier = () -> containerManager
                    .findAll()
                    .stream()
                    .map((Container c) -> c.get(ContainerComponentRegistry.class));
            final ComponentInstantiator.Builder builder = new ComponentInstantiator.BuilderDefault(registriesSupplier);

            final Map<Class<?>, Object> services = new LazyMap<>(24,
                    type -> defaultServiceProvider
                            .lookup(container.getId(), container.getLoader(),
                                    () -> container
                                            .getLoader()
                                            .findContainedResources("TALEND-INF/local-configuration.properties"),
                                    container.getLocalDependencyRelativeResolver(), type, seviceLookupRef, builder));
            seviceLookupRef.set(services);

            final AllServices allServices = new AllServices(services);
            container.set(AllServices.class, allServices);
            // container services
            containerServices(container, services);

            container.set(LightContainer.class, new LightContainer() {

                @Override
                public ClassLoader classloader() {
                    return container.getLoader();
                }

                @Override
                public <T> T findService(final Class<T> key) {
                    return key.cast(services.get(key));
                }
            });

            final Map<String, AnnotatedElement> componentDefaults = new HashMap<>();

            finder.findAnnotatedClasses(Internationalized.class).forEach((Class proxy) -> {
                final Object service = internationalizationServiceFactory.create(proxy, container.getLoader());
                final Object instance = javaProxyEnricherFactory
                        .asSerializable(container.getLoader(), container.getId(), proxy.getName(),
                                service, true);
                services.put(proxy, instance);
                registry.getServices().add(new ServiceMeta(instance, emptyList()));
            });
            finder
                    .findAnnotatedMethods(Request.class)
                    .stream()
                    .map(Method::getDeclaringClass)
                    .distinct()
                    .filter(HttpClient.class::isAssignableFrom) // others are created manually
                    .forEach(proxy -> {
                        final Object instance =
                                HttpClientFactory.class.cast(services.get(HttpClientFactory.class)).create(proxy, null);
                        services.put(proxy, instance);
                        registry.getServices().add(new ServiceMeta(instance, emptyList()));
                    });
            final ServiceHelper serviceHelper = new ServiceHelper(ComponentManager.this.proxyGenerator, services);
            final Map<Class<?>, Object> userServices = finder
                    .findAnnotatedClasses(Service.class)
                    .stream()
                    .filter(s -> !services.containsKey(s))
                    .collect(toMap(identity(), (Class<?> service) -> ThreadHelper
                            .runWithClassLoader(
                                    () -> serviceHelper
                                            .createServiceInstance(container.getLoader(), container.getId(), service),
                                    container.getLoader())));
            // now we created all instances we can inject *then* postconstruct
            final Injector injector = Injector.class.cast(services.get(Injector.class));
            services.putAll(userServices);
            userServices.forEach((service, instance) -> {
                injector.inject(instance);
                doInvoke(container.getId(), instance, PostConstruct.class);
                services.put(service, instance);
                registry
                        .getServices()
                        .add(new ServiceMeta(instance, Stream
                                .of(service.getMethods())
                                .filter(m -> Stream
                                        .of(m.getAnnotations())
                                        .anyMatch(a -> a.annotationType().isAnnotationPresent(ActionType.class)))
                                .map(serviceMethod -> createServiceMeta(container, services, componentDefaults, service,
                                        instance, serviceMethod, service))
                                .collect(toList())));
                info("Added @Service " + service + " for container-id=" + container.getId());
            });

            final ComponentContexts componentContexts = new ComponentContexts();
            container.set(ComponentContexts.class, componentContexts);
            if (!isGeneric) {
                Stream
                        .of(PartitionMapper.class, Processor.class, Emitter.class, DriverRunner.class)
                        .flatMap(a -> finder.findAnnotatedClasses(a).stream())
                        .filter(t -> Modifier.isPublic(t.getModifiers()))
                        .forEach(type -> onComponent(container, registry, services, allServices, componentDefaults,
                                componentContexts, type, xbeanConverterCache));
            }
        }

        private Filter createScanningFilter(final Properties config) {
            final String includes = config.getProperty("classloader.includes");
            final String excludes = config.getProperty("classloader.excludes");
            if (includes == null && excludes == null) {
                return KnownClassesFilter.INSTANCE;
            }
            final Filter accept = ofNullable(includes)
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .map(s -> s.split(","))
                    .map(Filters::patterns)
                    .orElseGet(() -> name -> true);
            final Filter reject = ofNullable(excludes)
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .map(s -> s.split(","))
                    .map(Filters::patterns)
                    .orElseGet(() -> name -> false);
            if ("include-exclude".equals(config.getProperty("classloader.filter.strategy"))) {
                return new IncludeExcludeFilter(accept, reject);
            }
            return new ExcludeIncludeFilter(accept, reject);
        }

        private void onComponent(final Container container, final ContainerComponentRegistry registry,
                final Map<Class<?>, Object> services, final AllServices allServices,
                final Map<String, AnnotatedElement> componentDefaults, final ComponentContexts componentContexts,
                final Class<?> type, final Map<java.lang.reflect.Type, Optional<Converter>> xbeanConverterCache) {
            final Components components = findComponentsConfig(componentDefaults, type, container.getLoader(),
                    Components.class, DEFAULT_COMPONENT);

            final ComponentContextImpl context = new ComponentContextImpl(type);
            componentContexts.getContexts().put(type, context);
            extensions.forEach(e -> {
                context.setCurrentExtension(e);
                try {
                    e.onComponent(context);
                } finally {
                    context.setCurrentExtension(null);
                }
                if (context.getOwningExtension() == e) {
                    ofNullable(e.getExtensionServices(container.getId())).ifPresent(services::putAll);
                }
            });

            final ComponentMetaBuilder builder = new ComponentMetaBuilder(container.getId(), allServices, components,
                    componentDefaults.get(getAnnotatedElementCacheKey(type)), context, migrationHandlerFactory,
                    iconFinder, xbeanConverterCache);

            final Thread thread = Thread.currentThread();
            final ClassLoader old = thread.getContextClassLoader();
            thread.setContextClassLoader(container.getLoader());
            try {
                visitor.visit(type, builder, Mode.mode != Mode.UNSAFE && !context.isNoValidation());
            } finally {
                thread.setContextClassLoader(old);
            }

            ofNullable(builder.component).ifPresent(c -> {
                // for now we assume one family per module, we can remove this constraint if
                // really needed
                // but kind of enforce a natural modularity

                final ComponentFamilyMeta componentFamilyMeta =
                        registry.getComponents().computeIfAbsent(c.getName(), n -> c);
                if (componentFamilyMeta != c) {
                    if (componentFamilyMeta
                            .getProcessors()
                            .keySet()
                            .stream()
                            .anyMatch(k -> c.getProcessors().containsKey(k))) {
                        throw new IllegalArgumentException("Conflicting processors in " + c);
                    }
                    if (componentFamilyMeta
                            .getPartitionMappers()
                            .keySet()
                            .stream()
                            .anyMatch(k -> c.getPartitionMappers().containsKey(k))) {
                        throw new IllegalArgumentException("Conflicting mappers in " + c);
                    }
                    if (componentFamilyMeta
                            .getDriverRunners()
                            .keySet()
                            .stream()
                            .anyMatch(k -> c.getDriverRunners().containsKey(k))) {
                        throw new IllegalArgumentException("Conflicting driver runners in " + c);
                    }

                    // if we passed validations then merge
                    componentFamilyMeta.getProcessors().putAll(c.getProcessors());
                    componentFamilyMeta.getPartitionMappers().putAll(c.getPartitionMappers());
                    componentFamilyMeta.getDriverRunners().putAll(c.getDriverRunners());
                }
            });

            info("Parsed component " + type + " for container-id=" + container.getId());
        }

        private ServiceMeta.ActionMeta createServiceMeta(final Container container,
                final Map<Class<?>, Object> services, final Map<String, AnnotatedElement> componentDefaults,
                final Class<?> service, final Object instance, final Method serviceMethod,
                final Class<?> declaringClass) {
            final Components components = findComponentsConfig(componentDefaults, declaringClass, container.getLoader(),
                    Components.class, DEFAULT_COMPONENT);

            final Annotation marker = Stream
                    .of(serviceMethod.getAnnotations())
                    .filter(a -> a.annotationType().isAnnotationPresent(ActionType.class))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Something went wrong with " + serviceMethod));
            final ActionType actionType = marker.annotationType().getAnnotation(ActionType.class);

            if (actionType.expectedReturnedType() != Object.class
                    && !actionType.expectedReturnedType().isAssignableFrom(serviceMethod.getReturnType())) {
                throw new IllegalArgumentException(
                        "Can't use " + marker + " on " + serviceMethod + ", expected returned type: "
                                + actionType.expectedReturnedType() + ", actual one: " + serviceMethod.getReturnType());
            }

            String component = "";
            try {
                component = ofNullable(String.class.cast(marker.annotationType().getMethod("family").invoke(marker)))
                        .filter(c -> !c.isEmpty())
                        .orElseGet(components::family);
            } catch (final NoSuchMethodException e) {
                component = components.family();
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw toRuntimeException(e);
            }
            if (component.isEmpty()) {
                throw new IllegalArgumentException("No component for " + serviceMethod
                        + ", maybe add a @Components on your package " + service.getPackage());
            }

            final String name = Stream.of("name", "value").map(mName -> {
                try {
                    return String.class.cast(marker.annotationType().getMethod(mName).invoke(marker));
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw toRuntimeException(e);
                } catch (final NoSuchMethodException e) {
                    return null;
                }
            }).filter(Objects::nonNull).findFirst().orElse("default");

            final Function<Map<String, String>, Object[]> parameterFactory =
                    /*
                     * think user flow in a form, we can't validate these actions. Maybe we need to add an API for that
                     * later
                     */
                    createParametersFactory(container.getId(), serviceMethod, services, null);
            final Object actionInstance = Modifier.isStatic(serviceMethod.getModifiers()) ? null : instance;
            final Function<Map<String, String>, Object> invoker = arg -> executeInContainer(container.getId(), () -> {
                try {
                    final Object[] args = parameterFactory.apply(arg);
                    return serviceMethod.invoke(actionInstance, args);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final InvocationTargetException e) {
                    throw toRuntimeException(e);
                }
            });

            return new ServiceMeta.ActionMeta(component, actionType.value(), name,
                    serviceMethod.getGenericParameterTypes(),
                    () -> executeInContainer(container.getId(),
                            () -> parameterModelService
                                    .buildServiceParameterMetas(serviceMethod,
                                            ofNullable(serviceMethod.getDeclaringClass().getPackage())
                                                    .map(Package::getName)
                                                    .orElse(""),
                                            new BaseParameterEnricher.Context(LocalConfiguration.class
                                                    .cast(container
                                                            .get(AllServices.class)
                                                            .getServices()
                                                            .get(LocalConfiguration.class))))),
                    invoker);
        }

        private Archive toArchive(final String module, final OriginalId originalId,
                final ConfigurableClassLoader loader) {
            final Collection<Archive> archives = new ArrayList<>();
            final Archive mainArchive =
                    toArchive(module, ofNullable(originalId).map(OriginalId::getValue).orElse(module), loader);
            archives.add(mainArchive);
            final URL mainUrl = archiveToUrl(mainArchive);
            try {
                archives.addAll(list(loader.getResources(dependenciesResource)).stream().map(url -> { // strip resource
                    final String rawUrl = url.toExternalForm();
                    try {
                        if (rawUrl.startsWith("nested")) {
                            return loader
                                    .getParent()
                                    .getResource(rawUrl.substring("nested:".length(), rawUrl.indexOf("!/")));
                        }
                        return new URL(rawUrl.substring(0, rawUrl.length() - dependenciesResource.length()));
                    } catch (final MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                }).filter(Objects::nonNull).filter(url -> {
                    if (Objects.equals(mainUrl, url)) {
                        return false;
                    }
                    if (mainUrl == null) {
                        return true;
                    }

                    // if not the same url, ensure it is not the same jar coming from file and nested repo
                    // this is very unlikely but happens on dirty systems/setups
                    final String mainPath = mainUrl.getPath();
                    final String path = url.getPath();
                    final String marker = "!/" + NESTED_MAVEN_REPOSITORY;
                    if (path != null && path.contains(marker) && !mainPath.contains(marker)) {
                        final String mvnPath = path.substring(path.lastIndexOf(marker) + marker.length());
                        final Path asFile = ofNullable(container.getRootRepositoryLocationPath())
                                .orElseGet(() -> PathFactory.get("."))
                                .resolve(mvnPath);
                        return !Objects.equals(Files.toFile(mainUrl).toPath(), asFile);
                    }
                    return true;
                }).map(nested -> {
                    if ("nested".equals(nested.getProtocol())
                            || (nested.getPath() != null && nested.getPath().contains("!/MAVEN-INF/repository/"))) {
                        JarInputStream jarStream = null;
                        try {
                            jarStream = new JarInputStream(nested.openStream());
                            log.debug("Found a nested resource for " + nested);
                            return new NestedJarArchive(nested, jarStream, loader);
                        } catch (final IOException e) {
                            if (jarStream != null) {
                                try { // normally not needed
                                    jarStream.close();
                                } catch (final IOException e1) {
                                    // no-op
                                }
                            }
                            throw new IllegalStateException(e);
                        }
                    }
                    try {
                        return ClasspathArchive.archive(loader, Files.toFile(nested).toURI().toURL());
                    } catch (final MalformedURLException e) {
                        throw new IllegalStateException(e);
                    }
                }).collect(toList()));
            } catch (final IOException e) {
                throw new IllegalArgumentException("Error scanning " + module, e);
            }
            return new CompositeArchive(archives);
        }

        private URL archiveToUrl(final Archive mainArchive) {
            if (JarArchive.class.isInstance(mainArchive)) {
                return JarArchive.class.cast(mainArchive).getUrl();
            } else if (FileArchive.class.isInstance(mainArchive)) {
                try {
                    return FileArchive.class.cast(mainArchive).getDir().toURI().toURL();
                } catch (final MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
            } else if (NestedJarArchive.class.isInstance(mainArchive)) {
                return NestedJarArchive.class.cast(mainArchive).getRootMarker();
            }
            return null;
        }

        private Archive toArchive(final String module, final String moduleId, final ConfigurableClassLoader loader) {
            final Path file = of(PathFactory.get(module))
                    .filter(java.nio.file.Files::exists)
                    .orElseGet(() -> container.resolve(module));
            if (java.nio.file.Files.exists(file)) {
                try {
                    return ClasspathArchive.archive(loader, file.toUri().toURL());
                } catch (final MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            info(module + " (" + moduleId + ") is not a file, will try to look it up from a nested maven repository");
            URL nestedJar = loader.getParent().getResource(ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY + module);
            if (nestedJar == null) {
                nestedJar = loader.getParent().getResource(module);
            }
            if (nestedJar != null) {
                InputStream nestedStream = null;
                final JarInputStream jarStream;
                try {
                    nestedStream = nestedJar.openStream();
                    jarStream = new JarInputStream(nestedStream);
                    log.debug("Found a nested resource for " + module);
                    return new NestedJarArchive(nestedJar, jarStream, loader);
                } catch (final IOException e) {
                    if (nestedStream != null) {
                        try { // normally not needed
                            nestedStream.close();
                        } catch (final IOException e1) {
                            // no-op
                        }
                    }
                }
            }
            throw new IllegalArgumentException(
                    "Module error: check that the module exist and is a jar or a directory. " + moduleId);
        }

        @Override
        public void onClose(final Container container) {
            // ensure we don't keep any data/ref after the classloader of the container is
            // released
            ofNullable(container.get(ContainerComponentRegistry.class)).ifPresent(r -> {
                final ContainerComponentRegistry registry = container.remove(ContainerComponentRegistry.class);
                registry.getComponents().clear();
                registry
                        .getServices()
                        .stream()
                        .filter(i -> !Proxy.isProxyClass(i.getInstance().getClass()))
                        .forEach(s -> doInvoke(container.getId(), s.getInstance(), PreDestroy.class));
                registry.getServices().clear();
            });
            ofNullable(container.get(AllServices.class))
                    .map(s -> s.getServices().get(Jsonb.class))
                    .map(Jsonb.class::cast)
                    .ifPresent(jsonb -> {
                        try {
                            jsonb.close();
                        } catch (final Exception e) {
                            log.warn(e.getMessage(), e);
                        }
                    });
        }

        private void doInvoke(final String container, final Object instance, final Class<? extends Annotation> marker) {
            executeInContainer(container, () -> {
                final Class<?> instanceClass = instance.getClass();
                new ClassFinder(instanceClass.getName().contains("$$") ? instanceClass.getSuperclass() : instanceClass)
                        .findAnnotatedMethods(marker)
                        .stream() // we don't limit to one for now
                        .filter(m -> Modifier.isPublic(m.getModifiers()))
                        .forEach(m -> {
                            try {
                                m.invoke(instance);
                            } catch (final IllegalAccessException e) {
                                throw new IllegalStateException(e);
                            } catch (final InvocationTargetException e) {
                                throw toRuntimeException(e);
                            }
                        });
                return null;
            });
        }
    }

    @RequiredArgsConstructor
    private class ComponentMetaBuilder implements ModelListener {

        private final String plugin;

        private final AllServices services;

        private final Components components;

        private final AnnotatedElement familyAnnotationElement;

        private final ComponentContextImpl context;

        private final MigrationHandlerFactory migrationHandlerFactory;

        private final IconFinder iconFinder;

        private final Map<java.lang.reflect.Type, Optional<Converter>> xbeanConverterCache;

        private ComponentMetadataService metadataService = new ComponentMetadataService();

        private ComponentFamilyMeta component;

        @Override
        public void onPartitionMapper(final Class<?> type, final PartitionMapper partitionMapper) {
            final Constructor<?> constructor = findConstructor(type);
            final boolean infinite = partitionMapper.infinite();
            final Supplier<List<ParameterMeta>> parameterMetas = lazy(() -> executeInContainer(plugin,
                    () -> {
                        final List<ParameterMeta> params = parameterModelService
                                .buildParameterMetas(constructor, getPackage(type),
                                        new BaseParameterEnricher.Context(LocalConfiguration.class
                                                .cast(services.getServices().get(LocalConfiguration.class))));
                        if (infinite) {
                            if (partitionMapper.stoppable()) {
                                addInfiniteMapperBuiltInParameters(type, params);
                            }
                        }
                        return params;
                    }));
            final Function<Map<String, String>, Object[]> parameterFactory =
                    createParametersFactory(plugin, constructor, services.getServices(), parameterMetas);
            final String name = of(partitionMapper.name()).filter(n -> !n.isEmpty()).orElseGet(type::getName);
            final ComponentFamilyMeta component = getOrCreateComponent(partitionMapper.family());

            final Function<Map<String, String>, Mapper> instantiator =
                    context.getOwningExtension() != null && context.getOwningExtension().supports(Mapper.class)
                            ? config -> executeInContainer(plugin,
                                    () -> context
                                            .getOwningExtension()
                                            .convert(new ComponentInstanceImpl(
                                                    doInvoke(constructor, parameterFactory.apply(config)), plugin,
                                                    component.getName(), name), Mapper.class))
                            : config -> new PartitionMapperImpl(component.getName(), name, null, plugin, infinite,
                                    ofNullable(config)
                                            .map(it -> it
                                                    .entrySet()
                                                    .stream()
                                                    .filter(e -> e.getKey().startsWith("$")
                                                            || e.getKey().contains(".$"))
                                                    .collect(toMap(java.util.Map.Entry::getKey,
                                                            java.util.Map.Entry::getValue)))
                                            .orElseGet(Collections::emptyMap),
                                    doInvoke(constructor, parameterFactory.apply(config)));
            final Map<String, String> metadata = metadataService.getMetadata(type);

            component
                    .getPartitionMappers()
                    .put(name,
                            new ComponentFamilyMeta.PartitionMapperMeta(component, name, iconFinder.findIcon(type),
                                    findVersion(type), type, parameterMetas,
                                    args -> propertyEditorRegistry
                                            .withCache(xbeanConverterCache, () -> instantiator.apply(args)),
                                    Lazy
                                            .lazy(() -> migrationHandlerFactory
                                                    .findMigrationHandler(parameterMetas, type, services)),
                                    !context.isNoValidation(), metadata));
        }

        @Override
        public void onEmitter(final Class<?> type, final Emitter emitter) {
            final Constructor<?> constructor = findConstructor(type);
            final Supplier<List<ParameterMeta>> parameterMetas = lazy(() -> executeInContainer(plugin,
                    () -> parameterModelService
                            .buildParameterMetas(constructor, getPackage(type),
                                    new BaseParameterEnricher.Context(LocalConfiguration.class
                                            .cast(services.getServices().get(LocalConfiguration.class))))));
            final Function<Map<String, String>, Object[]> parameterFactory =
                    createParametersFactory(plugin, constructor, services.getServices(), parameterMetas);
            final String name = of(emitter.name()).filter(n -> !n.isEmpty()).orElseGet(type::getName);
            final ComponentFamilyMeta component = getOrCreateComponent(emitter.family());
            final Function<Map<String, String>, Mapper> instantiator =
                    context.getOwningExtension() != null && context.getOwningExtension().supports(Mapper.class)
                            ? config -> executeInContainer(plugin,
                                    () -> context
                                            .getOwningExtension()
                                            .convert(new ComponentInstanceImpl(
                                                    doInvoke(constructor, parameterFactory.apply(config)), plugin,
                                                    component.getName(), name), Mapper.class))
                            : config -> new LocalPartitionMapper(component.getName(), name, plugin,
                                    doInvoke(constructor, parameterFactory.apply(config)));

            final Map<String, String> metadata = metadataService.getMetadata(type);

            component
                    .getPartitionMappers()
                    .put(name,
                            new ComponentFamilyMeta.PartitionMapperMeta(component, name, iconFinder.findIcon(type),
                                    findVersion(type), type, parameterMetas,
                                    args -> propertyEditorRegistry
                                            .withCache(xbeanConverterCache, () -> instantiator.apply(args)),
                                    Lazy
                                            .lazy(() -> migrationHandlerFactory
                                                    .findMigrationHandler(parameterMetas, type, services)),
                                    !context.isNoValidation(), metadata));
        }

        @Override
        public void onProcessor(final Class<?> type, final Processor processor) {
            final Constructor<?> constructor = findConstructor(type);
            final Supplier<List<ParameterMeta>> parameterMetas = lazy(() -> executeInContainer(plugin, () -> {
                final List<ParameterMeta> params = parameterModelService
                        .buildParameterMetas(constructor, getPackage(type), new BaseParameterEnricher.Context(
                                LocalConfiguration.class.cast(services.getServices().get(LocalConfiguration.class))));
                addProcessorsBuiltInParameters(type, params);
                return params;
            }));
            final Function<Map<String, String>, Object[]> parameterFactory =
                    createParametersFactory(plugin, constructor, services.getServices(), parameterMetas);
            final String name = of(processor.name()).filter(n -> !n.isEmpty()).orElseGet(type::getName);
            final ComponentFamilyMeta component = getOrCreateComponent(processor.family());
            final Function<Map<String, String>, org.talend.sdk.component.runtime.output.Processor> instantiator =
                    context.getOwningExtension() != null && context
                            .getOwningExtension()
                            .supports(org.talend.sdk.component.runtime.output.Processor.class)
                                    ? config -> executeInContainer(
                                            plugin,
                                            () -> context
                                                    .getOwningExtension()
                                                    .convert(
                                                            new ComponentInstanceImpl(doInvoke(constructor,
                                                                    parameterFactory.apply(config)), plugin,
                                                                    component.getName(), name),
                                                            org.talend.sdk.component.runtime.output.Processor.class))
                                    : config -> new ProcessorImpl(this.component.getName(), name, plugin,
                                            ofNullable(config)
                                                    .map(it -> it
                                                            .entrySet()
                                                            .stream()
                                                            .filter(e -> e.getKey().startsWith("$")
                                                                    || e.getKey().contains(".$"))
                                                            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)))
                                                    .orElseGet(Collections::emptyMap),
                                            doInvoke(constructor, parameterFactory.apply(config)));

            final Map<String, String> metadata = metadataService.getMetadata(type);

            component
                    .getProcessors()
                    .put(name,
                            new ComponentFamilyMeta.ProcessorMeta(component, name, iconFinder.findIcon(type),
                                    findVersion(type), type, parameterMetas,
                                    args -> propertyEditorRegistry
                                            .withCache(xbeanConverterCache, () -> instantiator.apply(args)),
                                    Lazy
                                            .lazy(() -> migrationHandlerFactory
                                                    .findMigrationHandler(parameterMetas, type, services)),
                                    !context.isNoValidation(), metadata));
        }

        private void addInfiniteMapperBuiltInParameters(final Class<?> type, final List<ParameterMeta> parameterMetas) {
            final ParameterMeta root =
                    parameterMetas.stream().filter(p -> p.getName().equals(p.getPath())).findFirst().orElseGet(() -> {
                        final ParameterMeta umbrella = new ParameterMeta(new ParameterMeta.Source() {

                            @Override
                            public String name() {
                                return "$configuration";
                            }

                            @Override
                            public Class<?> declaringClass() {
                                return Object.class;
                            }
                        }, Object.class, ParameterMeta.Type.OBJECT, "$configuration", "$configuration", new String[0],
                                new ArrayList<>(), new ArrayList<>(), new HashMap<>(), true);
                        parameterMetas.add(umbrella);
                        return umbrella;
                    });

            final StreamingMaxRecordsParamBuilder paramBuilder = new StreamingMaxRecordsParamBuilder(root,
                    type.getSimpleName(),
                    LocalConfiguration.class.cast(services.services.get(LocalConfiguration.class)));
            final ParameterMeta maxRecords = paramBuilder.newBulkParameter();
            final ParameterMeta maxDuration = new StreamingMaxDurationMsParamBuilder(root, type.getSimpleName(),
                    LocalConfiguration.class.cast(services.services.get(LocalConfiguration.class))).newBulkParameter();
            final String layoutOptions = maxRecords.getName() + "|" + maxDuration.getName();
            final String layoutType = paramBuilder.getLayoutType();
            if (layoutType == null) {
                root.getMetadata().put("tcomp::ui::gridlayout::Advanced::value", layoutOptions);
                root.getMetadata()
                        .put("tcomp::ui::gridlayout::Main::value", root.getNestedParameters()
                                .stream()
                                .map(ParameterMeta::getName)
                                .collect(joining("|")));
            } else if (!root.getMetadata().containsKey(layoutType)) {
                root.getMetadata().put(layoutType, layoutType.contains("gridlayout") ? layoutOptions : "true");
            } else if (layoutType.contains("gridlayout")) {
                final String oldLayout = root.getMetadata().get(layoutType);
                root.getMetadata().put(layoutType, layoutOptions + "|" + oldLayout);
            }
            root.getNestedParameters().add(maxRecords);
            root.getNestedParameters().add(maxDuration);
        }

        private void addProcessorsBuiltInParameters(final Class<?> type, final List<ParameterMeta> parameterMetas) {
            final ParameterMeta root =
                    parameterMetas.stream().filter(p -> p.getName().equals(p.getPath())).findFirst().orElseGet(() -> {
                        final ParameterMeta umbrella = new ParameterMeta(new ParameterMeta.Source() {

                            @Override
                            public String name() {
                                return "$configuration";
                            }

                            @Override
                            public Class<?> declaringClass() {
                                return Object.class;
                            }
                        }, Object.class, ParameterMeta.Type.OBJECT, "$configuration", "$configuration", new String[0],
                                new ArrayList<>(), new ArrayList<>(), new HashMap<>(), true);
                        parameterMetas.add(umbrella);
                        return umbrella;
                    });

            if (Stream.of(type.getMethods()).anyMatch(p -> p.isAnnotationPresent(AfterGroup.class))) {
                final MaxBatchSizeParamBuilder paramBuilder = new MaxBatchSizeParamBuilder(root, type.getSimpleName(),
                        LocalConfiguration.class.cast(services.services.get(LocalConfiguration.class)));
                final ParameterMeta maxBatchSize = paramBuilder.newBulkParameter();
                if (maxBatchSize != null) {
                    final String layoutType = paramBuilder.getLayoutType();
                    if (layoutType == null) {
                        root.getMetadata().put("tcomp::ui::gridlayout::Advanced::value", maxBatchSize.getName());
                        root
                                .getMetadata()
                                .put("tcomp::ui::gridlayout::Main::value",
                                        root
                                                .getNestedParameters()
                                                .stream()
                                                .map(ParameterMeta::getName)
                                                .collect(joining("|")));
                    } else if (!root.getMetadata().containsKey(layoutType)) {
                        root
                                .getMetadata()
                                .put(layoutType, layoutType.contains("gridlayout") ? maxBatchSize.getName() : "true");
                    } else if (layoutType.contains("gridlayout")) {
                        final String oldLayout = root.getMetadata().get(layoutType);
                        root.getMetadata().put(layoutType, maxBatchSize.getName() + "|" + oldLayout);
                    }
                    root.getNestedParameters().add(maxBatchSize);
                }
            }
        }

        @Override
        public void onDriverRunner(final Class<?> type, final DriverRunner processor) {
            final Constructor<?> constructor = findConstructor(type);
            final Supplier<List<ParameterMeta>> parameterMetas = lazy(() -> executeInContainer(plugin,
                    () -> parameterModelService
                            .buildParameterMetas(constructor, getPackage(type),
                                    new BaseParameterEnricher.Context(LocalConfiguration.class
                                            .cast(services.getServices().get(LocalConfiguration.class))))));
            final Function<Map<String, String>, Object[]> parameterFactory =
                    createParametersFactory(plugin, constructor, services.getServices(), parameterMetas);
            final String name = of(processor.name()).filter(n -> !n.isEmpty()).orElseGet(type::getName);
            final ComponentFamilyMeta component = getOrCreateComponent(processor.family());
            final Function<Map<String, String>, org.talend.sdk.component.runtime.standalone.DriverRunner> instantiator =
                    context.getOwningExtension() != null && context
                            .getOwningExtension()
                            .supports(org.talend.sdk.component.runtime.standalone.DriverRunner.class)
                                    ? config -> executeInContainer(plugin,
                                            () -> context
                                                    .getOwningExtension()
                                                    .convert(new ComponentInstanceImpl(doInvoke(
                                                            constructor, parameterFactory.apply(config)), plugin,
                                                            component.getName(), name),
                                                            org.talend.sdk.component.runtime.standalone.DriverRunner.class))
                                    : config -> new DriverRunnerImpl(this.component.getName(), name, plugin,
                                            doInvoke(constructor, parameterFactory.apply(config)));
            final Map<String, String> metadata = metadataService.getMetadata(type);

            component
                    .getDriverRunners()
                    .put(name,
                            new ComponentFamilyMeta.DriverRunnerMeta(component, name, iconFinder.findIcon(type),
                                    findVersion(type), type, parameterMetas,
                                    args -> propertyEditorRegistry
                                            .withCache(xbeanConverterCache, () -> instantiator.apply(args)),
                                    Lazy
                                            .lazy(() -> migrationHandlerFactory
                                                    .findMigrationHandler(parameterMetas, type, services)),
                                    !context.isNoValidation(), metadata));
        }

        private String getPackage(final Class<?> type) {
            return ofNullable(type.getPackage()).map(Package::getName).orElse("");
        }

        private int findVersion(final Class<?> type) {
            return ofNullable(type.getAnnotation(Version.class)).map(Version::value).orElse(1);
        }

        // we keep the component (family) value since it is more user fiendly than
        // having a generated id and it makes the
        // component
        // logical "id" more accurate
        private ComponentFamilyMeta getOrCreateComponent(final String component) {
            final String comp = ofNullable(component).filter(s -> !s.isEmpty()).orElseGet(components::family);
            if (comp.isEmpty()) {
                throw new IllegalArgumentException("Missing component");
            }
            return this.component == null || !component.equals(this.component.getName())
                    ? (this.component = new ComponentFamilyMeta(plugin, asList(components.categories()),
                            iconFinder.findIcon(familyAnnotationElement), comp,
                            Class.class.isInstance(familyAnnotationElement)
                                    ? getPackage(Class.class.cast(familyAnnotationElement))
                                    : ""))
                    : this.component;
        }

        private Serializable doInvoke(final Constructor<?> constructor, final Object[] args) {
            return executeInContainer(plugin, () -> {
                try {
                    return Serializable.class.cast(constructor.newInstance(args));
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException(e);
                } catch (final ClassCastException e) {
                    throw new IllegalArgumentException(constructor + " should return a Serializable", e);
                } catch (final InvocationTargetException e) {
                    throw toRuntimeException(e);
                } catch (final InstantiationException e) {
                    throw new IllegalArgumentException(e);
                }
            });
        }
    }

    /**
     * WARNING: use with caution and only if you fully understand the consequences.
     */
    public interface Customizer {

        /**
         * @return a stream of string representing classes or packages. They will be considered
         * as loaded from the "container" (ComponentManager loader) and not the components classloaders.
         */
        Stream<String> containerClassesAndPackages();

        /**
         * @return
         */
        default Stream<String> parentResources() {
            return Stream.empty();
        }

        /**
         * @return advanced toggle to ignore built-in beam exclusions and let this customizer override them.
         */
        default boolean ignoreBeamClassLoaderExclusions() {
            return false;
        }

        /**
         * Disable default built-in component classpath building mecanism. This is useful when relying on
         * a custom {@link ContainerClasspathContributor} handling it.
         *
         * @return true if the default dependencies descriptor (TALEND-INF/dependencies.txt) must be ignored.
         */
        default boolean ignoreDefaultDependenciesDescriptor() {
            return false;
        }

        /**
         * Enables a customizer to know other configuration.
         *
         * @param customizers all customizers.
         *
         * @deprecated Mainly here for backward compatibility for beam customizer.
         */
        @Deprecated
        default void setCustomizers(final Collection<Customizer> customizers) {
            // no-op
        }
    }

    /**
     * WARNING: internal extension point, use it only if you really understand what it implies.
     */
    public interface ContainerClasspathContributor {

        Collection<Artifact> findContributions(String pluginId);

        boolean canResolve(String path);

        Path resolve(String path);
    }
}
