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
package org.talend.sdk.component.runtime.manager;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.xbean.finder.archive.FileArchive.decode;
import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;
import static org.talend.sdk.component.runtime.manager.reflect.Constructors.findConstructor;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
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
import javax.json.JsonBuilderFactory;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriterFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParserFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.ClassFinder;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.ClasspathArchive;
import org.apache.xbean.finder.archive.FilteredArchive;
import org.apache.xbean.finder.filter.ExcludeIncludeFilter;
import org.apache.xbean.finder.filter.Filter;
import org.apache.xbean.finder.filter.Filters;
import org.apache.xbean.finder.util.Files;
import org.apache.xbean.propertyeditor.AbstractConverter;
import org.apache.xbean.propertyeditor.BigDecimalEditor;
import org.apache.xbean.propertyeditor.BigIntegerEditor;
import org.apache.xbean.propertyeditor.BooleanEditor;
import org.apache.xbean.propertyeditor.CharacterEditor;
import org.apache.xbean.propertyeditor.ClassEditor;
import org.apache.xbean.propertyeditor.Converter;
import org.apache.xbean.propertyeditor.DateEditor;
import org.apache.xbean.propertyeditor.DoubleEditor;
import org.apache.xbean.propertyeditor.FileEditor;
import org.apache.xbean.propertyeditor.HashMapEditor;
import org.apache.xbean.propertyeditor.HashtableEditor;
import org.apache.xbean.propertyeditor.Inet4AddressEditor;
import org.apache.xbean.propertyeditor.Inet6AddressEditor;
import org.apache.xbean.propertyeditor.InetAddressEditor;
import org.apache.xbean.propertyeditor.ListEditor;
import org.apache.xbean.propertyeditor.MapEditor;
import org.apache.xbean.propertyeditor.ObjectNameEditor;
import org.apache.xbean.propertyeditor.PatternConverter;
import org.apache.xbean.propertyeditor.PropertiesEditor;
import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.apache.xbean.propertyeditor.SetEditor;
import org.apache.xbean.propertyeditor.SortedMapEditor;
import org.apache.xbean.propertyeditor.SortedSetEditor;
import org.apache.xbean.propertyeditor.StringEditor;
import org.apache.xbean.propertyeditor.URIEditor;
import org.apache.xbean.propertyeditor.URLEditor;
import org.talend.sdk.component.api.component.Components;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.input.Emitter;
import org.talend.sdk.component.api.input.PartitionMapper;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.processor.AfterGroup;
import org.talend.sdk.component.api.processor.Processor;
import org.talend.sdk.component.api.service.ActionType;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.api.service.factory.ObjectFactory;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.injector.Injector;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.container.Container;
import org.talend.sdk.component.container.ContainerListener;
import org.talend.sdk.component.container.ContainerManager;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.jmx.JmxManager;
import org.talend.sdk.component.runtime.base.Delegated;
import org.talend.sdk.component.runtime.input.LocalPartitionMapper;
import org.talend.sdk.component.runtime.input.Mapper;
import org.talend.sdk.component.runtime.input.PartitionMapperImpl;
import org.talend.sdk.component.runtime.internationalization.InternationalizationServiceFactory;
import org.talend.sdk.component.runtime.jsonb.MultipleFormatDateAdapter;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.manager.builtinparams.MaxBatchSizeParamBuilder;
import org.talend.sdk.component.runtime.manager.extension.ComponentContextImpl;
import org.talend.sdk.component.runtime.manager.extension.ComponentContexts;
import org.talend.sdk.component.runtime.manager.interceptor.InterceptorHandlerFacade;
import org.talend.sdk.component.runtime.manager.json.PreComputedJsonpProvider;
import org.talend.sdk.component.runtime.manager.json.TalendAccessMode;
import org.talend.sdk.component.runtime.manager.proxy.JavaProxyEnricherFactory;
import org.talend.sdk.component.runtime.manager.reflect.MigrationHandlerFactory;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.reflect.parameterenricher.BaseParameterEnricher;
import org.talend.sdk.component.runtime.manager.service.ContainerInfo;
import org.talend.sdk.component.runtime.manager.service.InjectorImpl;
import org.talend.sdk.component.runtime.manager.service.LocalCacheService;
import org.talend.sdk.component.runtime.manager.service.LocalConfigurationService;
import org.talend.sdk.component.runtime.manager.service.ObjectFactoryImpl;
import org.talend.sdk.component.runtime.manager.service.ResolverImpl;
import org.talend.sdk.component.runtime.manager.service.configuration.PropertiesConfiguration;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;
import org.talend.sdk.component.runtime.manager.service.record.RecordBuilderFactoryProvider;
import org.talend.sdk.component.runtime.manager.spi.ContainerListenerExtension;
import org.talend.sdk.component.runtime.manager.xbean.KnownClassesFilter;
import org.talend.sdk.component.runtime.manager.xbean.NestedJarArchive;
import org.talend.sdk.component.runtime.output.ProcessorImpl;
import org.talend.sdk.component.runtime.record.RecordBuilderFactoryImpl;
import org.talend.sdk.component.runtime.serialization.LightContainer;
import org.talend.sdk.component.runtime.visitor.ModelListener;
import org.talend.sdk.component.runtime.visitor.ModelVisitor;
import org.talend.sdk.component.spi.component.ComponentExtension;
import org.w3c.dom.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ComponentManager implements AutoCloseable {

    protected static final AtomicReference<ComponentManager> CONTEXTUAL_INSTANCE = new AtomicReference<>();

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

    private final Filter excludeClassesFilter;

    private final ParameterModelService parameterModelService;

    private final InternationalizationServiceFactory internationalizationServiceFactory =
            new InternationalizationServiceFactory();

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

    private final List<Customizer> customizers;

    @Getter
    private final Function<String, RecordBuilderFactory> recordBuilderFactoryProvider;

    @Getter
    private final JsonbConfig jsonbConfig = new JsonbConfig()
            .withAdapters(new MultipleFormatDateAdapter())
            .withBinaryDataStrategy(BinaryDataStrategy.BASE_64)
            .setProperty("johnzon.cdi.activated", false)
            .setProperty("johnzon.accessModeDelegate", new TalendAccessMode());

    private final PropertyEditorRegistry propertyEditorRegistry;

    /**
     * @param m2 the maven repository location if on the file system.
     * @param dependenciesResource the resource path containing dependencies.
     * @param jmxNamePattern a pattern to register the plugins (containers) in JMX, null
     * otherwise.
     */
    public ComponentManager(final File m2, final String dependenciesResource, final String jmxNamePattern) {
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        customizers =
                StreamSupport.stream(ServiceLoader.load(Customizer.class, tccl).spliterator(), false).collect(toList());
        classesFilter = Filters
                .prefixes(Stream
                        .concat(Stream
                                .of("org.talend.sdk.component.api.", "org.talend.sdk.component.spi.",
                                        "javax.annotation.", "javax.json.", "org.talend.sdk.component.classloader.",
                                        "org.talend.sdk.component.runtime.", "org.slf4j.", "org.apache.johnzon."),
                                additionalContainerClasses())
                        .distinct()
                        .toArray(String[]::new));
        excludeClassesFilter = Filters.prefixes("org.apache.beam.sdk.io.", "org.apache.beam.sdk.extensions.");

        jsonpProvider = JsonProvider.provider();
        jsonbProvider = JsonbProvider.provider();
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
        final ContainerManager.ClassLoaderConfiguration defaultClassLoaderConfiguration =
                ContainerManager.ClassLoaderConfiguration
                        .builder()
                        .parent(tccl)
                        .parentClassesFilter(isContainerClass)
                        .classesFilter(isContainerClass.negate())
                        .supportsResourceDependencies(true)
                        .create();
        this.container = new ContainerManager(ContainerManager.DependenciesResolutionConfiguration
                .builder()
                .resolver(new MvnDependencyListLocalRepositoryResolver(dependenciesResource, this::resolve))
                .rootRepositoryLocation(m2)
                .create(), defaultClassLoaderConfiguration, container -> {
                }, logInfoLevelMapping);
        this.container.registerListener(new Updater());
        ofNullable(jmxNamePattern)
                .map(String::trim)
                .filter(n -> !n.isEmpty())
                .ifPresent(p -> this.container
                        .registerListener(new JmxManager(container, p, ManagementFactory.getPlatformMBeanServer())));
        toStream(loadServiceProviders(ContainerListenerExtension.class, tccl))
                .peek(e -> e.setComponentManager(ComponentManager.this))
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
    }

    private File resolve(final String artifact) {
        return container.resolve(artifact);
    }

    private PropertyEditorRegistry createPropertyEditorRegistry() {
        final PropertyEditorRegistry registry = new PropertyEditorRegistry();
        final DoubleEditor doubleEditor = new DoubleEditor();
        // the front always sends us doubles so
        // if we don't map double to the native number we get number format exceptions
        final BiFunction<Class<?>, Function<Double, Object>, Converter> numberConverter =
                (type, mapper) -> new AbstractConverter(type) {

                    @Override
                    protected Object toObjectImpl(final String text) {
                        final Object o = doubleEditor.toObject(text);
                        return mapper.apply(Double.class.cast(o));
                    }
                };
        registry.register(new BooleanEditor());
        registry.register(numberConverter.apply(Byte.class, Double::byteValue));
        registry.register(numberConverter.apply(Short.class, Double::shortValue));
        registry.register(numberConverter.apply(Integer.class, Double::intValue));
        registry.register(numberConverter.apply(Long.class, Double::longValue));
        registry.register(numberConverter.apply(Float.class, Double::floatValue));
        registry.register(doubleEditor);
        registry.register(new BigDecimalEditor());
        registry.register(new BigIntegerEditor());
        registry.register(new StringEditor());
        registry.register(new CharacterEditor());
        registry.register(new ClassEditor());
        registry.register(new DateEditor());
        registry.register(new FileEditor());
        registry.register(new HashMapEditor());
        registry.register(new HashtableEditor());
        registry.register(new Inet4AddressEditor());
        registry.register(new Inet6AddressEditor());
        registry.register(new InetAddressEditor());
        registry.register(new ListEditor());
        registry.register(new SetEditor());
        registry.register(new MapEditor());
        registry.register(new SortedMapEditor());
        registry.register(new SortedSetEditor());
        registry.register(new ObjectNameEditor());
        registry.register(new PropertiesEditor());
        registry.register(new URIEditor());
        registry.register(new URLEditor());
        registry.register(new PatternConverter());
        return registry;
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
        ComponentManager manager = CONTEXTUAL_INSTANCE.get();
        if (manager == null) {
            synchronized (CONTEXTUAL_INSTANCE) {
                if (CONTEXTUAL_INSTANCE.get() == null) {
                    final Thread shutdownHook =
                            new Thread(ComponentManager.class.getName() + "-" + ComponentManager.class.hashCode()) {

                                @Override
                                public void run() {
                                    ofNullable(CONTEXTUAL_INSTANCE.get()).ifPresent(ComponentManager::close);
                                }
                            };

                    manager = new ComponentManager(findM2(), "TALEND-INF/dependencies.txt",
                            "org.talend.sdk.component:type=component,value=%s") {

                        private final AtomicBoolean closed = new AtomicBoolean(false);

                        {

                            info("Creating the contextual ComponentManager instance " + getIdentifiers());
                            if (!Boolean.getBoolean("component.manager.callers.skip")) {
                                addCallerAsPlugin();
                            }

                            // common for studio until job generation is updated to build a tcomp friendly bundle
                            if (!Boolean.getBoolean("component.manager.classpath.skip")) {
                                try {
                                    final Enumeration<URL> componentMarkers = Thread
                                            .currentThread()
                                            .getContextClassLoader()
                                            .getResources("TALEND-INF/dependencies.txt");
                                    while (componentMarkers.hasMoreElements()) {
                                        File file = Files.toFile(componentMarkers.nextElement());
                                        if (file.getName().equals("dependencies.txt") && file.getParentFile() != null
                                                && file.getParentFile().getName().equals("TALEND-INF")) {
                                            file = file.getParentFile().getParentFile();
                                        }
                                        if (!hasPlugin(container.buildAutoIdFromName(file.getName()))) {
                                            addPlugin(file.getAbsolutePath());
                                        }
                                    }
                                } catch (final IOException e) {
                                    // no-op
                                }
                            }

                            container
                                    .getDefinedNestedPlugin()
                                    .stream()
                                    .filter(p -> !hasPlugin(p))
                                    .forEach(this::addPlugin);
                            info("Components: " + availablePlugins());
                        }

                        @Override
                        public void close() {
                            if (!closed.compareAndSet(false, true)) {
                                return;
                            }
                            try {
                                synchronized (CONTEXTUAL_INSTANCE) {
                                    if (CONTEXTUAL_INSTANCE.compareAndSet(this, null)) {
                                        try {
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

                    Runtime.getRuntime().addShutdownHook(shutdownHook);
                    manager.info("Created the contextual ComponentManager instance " + getIdentifiers());
                    if (!CONTEXTUAL_INSTANCE.compareAndSet(null, manager)) { // unlikely it fails in a synch block
                        manager = CONTEXTUAL_INSTANCE.get();
                    }
                }
            }
        }

        return manager;
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

    private Stream<String> additionalContainerClassesThroughExtension() {
        return Stream
                .concat(customizers.stream().flatMap(Customizer::containerClassesAndPackages),
                        ofNullable(
                                System.getProperty("talend.component.manager.classloader.container.classesAndPackages"))
                                        .map(s -> s.split(","))
                                        .map(Stream::of)
                                        .orElseGet(Stream::empty));
    }

    private Stream<String> additionalContainerClasses() {
        try { // if beam is here just skips beam sdk java core classes and load them from the container
            ComponentManager.class.getClassLoader().loadClass("org.apache.beam.sdk.Pipeline");
            return Stream
                    .concat(customizers.stream().anyMatch(Customizer::ignoreBeamClassLoaderExclusions) ? Stream.empty()
                            : Stream
                                    .of(
                                            // beam
                                            "org.apache.beam.runners.", "org.apache.beam.sdk.",
                                            "org.apache.beam.repackaged.", "org.apache.beam.vendor.",
                                            "org.talend.sdk.component.runtime.beam.", "org.codehaus.jackson.",
                                            "com.fasterxml.jackson.annotation.", "com.fasterxml.jackson.core.",
                                            "com.fasterxml.jackson.databind.", "com.thoughtworks.paranamer.",
                                            "org.apache.commons.compress.", "org.tukaani.xz.", "org.objenesis.",
                                            "org.joda.time.", "org.xerial.snappy.", "avro.shaded.com.google.",
                                            // avro package is used for hadoop, mapred etc, so doing a precise list
                                            "org.apache.avro.data.", "org.apache.avro.file.",
                                            "org.apache.avro.generic.", "org.apache.avro.io.",
                                            "org.apache.avro.message.", "org.apache.avro.reflect.",
                                            "org.apache.avro.specific.", "org.apache.avro.util.",
                                            "org.apache.avro.Avro", "org.apache.avro.Conversion",
                                            "org.apache.avro.Guava", "org.apache.avro.Json", "org.apache.avro.Logical",
                                            "org.apache.avro.Protocol", "org.apache.avro.Schema",
                                            "org.apache.avro.Unresolved", "org.apache.avro.Validate",
                                            // scala - most engines
                                            "scala.",
                                            // engines
                                            "org.apache.spark.", "org.apache.flink."),
                            additionalContainerClassesThroughExtension());
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            return additionalContainerClassesThroughExtension();
        }
    }

    protected static File findM2() {
        return ofNullable(System.getProperty("talend.component.manager.m2.repository")).map(File::new).orElseGet(() -> {
            // check if we are in the studio process if so just grab the the studio config
            final String m2Repo = System.getProperty("maven.repository");
            if (!"global".equals(m2Repo)) {
                final File localM2 = new File(System.getProperty("osgi.configuration.area"), ".m2");
                if (localM2.exists()) {
                    return localM2;
                }
            }
            return findDefaultM2();
        });
    }

    private static File findDefaultM2() {
        // check out settings.xml first
        final File settings = new File(System
                .getProperty("talend.component.manager.m2.settings",
                        System.getProperty("user.home") + "/.m2/settings.xml"));
        if (settings.exists()) {
            try {
                final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                final DocumentBuilder builder = factory.newDocumentBuilder();

                final Document document = builder.parse(settings);
                final XPathFactory xpf = XPathFactory.newInstance();
                final XPath xp = xpf.newXPath();
                final String localM2RepositoryFromSettings =
                        xp.evaluate("//settings/localRepository/text()", document.getDocumentElement());
                if (localM2RepositoryFromSettings != null && !localM2RepositoryFromSettings.isEmpty()) {
                    return new File(localM2RepositoryFromSettings);
                }
            } catch (final Exception ignore) {
                // fallback on default local path
            }
        }

        return new File(System.getProperty("user.home"), ".m2/repository");
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

    private static File toFile(final String classFileName, final URL url) {
        String path = url.getFile();
        path = path.substring(0, path.length() - classFileName.length());
        return new File(decode(path));
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
            File plugin = null;
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
                        plugin = new File(decode(new URL(spec.substring(0, separator)).getFile()));
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
                    // just a small workaround for maven/gradle
                    .flatMap(this::toPluginLocations)
                    .filter(path -> !container.find(path.getName()).isPresent())
                    .map(file -> {
                        final String id = addPlugin(file.getAbsolutePath());
                        if (container.find(id).get().get(ContainerComponentRegistry.class).getComponents().isEmpty()) {
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

    private Stream<File> toPluginLocations(final File src) {
        if ("test-classes".equals(src.getName()) && src.getParentFile() != null) { // maven
            return Stream.of(new File(src.getParentFile(), "classes"), src);
        }

        // gradle (v3 & v4)
        if ("classes".equals(src.getName()) && src.getParentFile() != null
                && "test".equals(src.getParentFile().getName()) && src.getParentFile().getParentFile() != null) {
            return Stream
                    .of(new File(src.getParentFile().getParentFile(), "production/classes"), src)
                    .filter(File::exists);
        }
        if ("test".equals(src.getName()) && src.getParentFile() != null
                && "java".equals(src.getParentFile().getName())) {
            return Stream.of(new File(src.getParentFile(), "main"), src).filter(File::exists);
        }
        return Stream.of(src);
    }

    public <T> Stream<T> find(final Function<Container, Stream<T>> mapper) {
        return container.findAll().stream().flatMap(mapper);
    }

    // really a DIY entry point for custom flow, it creates component instances but
    // nothing more
    public Optional<Object> createComponent(final String plugin, final String name, final ComponentType componentType,
            final int version, final Map<String, String> configuration) {
        final String familyName = container.buildAutoIdFromName(plugin);
        return find(pluginContainer -> Stream.of(pluginContainer.get(ContainerComponentRegistry.class)))
                .filter(Objects::nonNull)
                .map(r -> r.getComponents().get(familyName))
                .filter(Objects::nonNull)
                .map(component -> ofNullable(componentType.findMeta(component).get(name))
                        .map(comp -> comp
                                .getInstantiator()
                                .apply(configuration == null ? null
                                        : comp.getMigrationHandler().migrate(version, configuration))))
                .findFirst()
                .flatMap(identity())
                // unwrap to access the actual instance which is the desired one
                .map(i -> Delegated.class.isInstance(i) ? Delegated.class.cast(i).getDelegate() : i);
    }

    public Optional<Mapper> findMapper(final String plugin, final String name, final int version,
            final Map<String, String> configuration) {
        return find(pluginContainer -> Stream
                .of(pluginContainer
                        .get(ContainerComponentRegistry.class)
                        .getComponents()
                        .get(container.buildAutoIdFromName(plugin))))
                                .filter(Objects::nonNull)
                                .map(component -> ofNullable(component.getPartitionMappers().get(name))
                                        .map(mapper -> mapper
                                                .getInstantiator()
                                                .apply(configuration == null ? null
                                                        : mapper.getMigrationHandler().migrate(version, configuration)))
                                        .map(Mapper.class::cast))
                                .findFirst()
                                .flatMap(identity());
    }

    public Optional<org.talend.sdk.component.runtime.output.Processor> findProcessor(final String plugin,
            final String name, final int version, final Map<String, String> configuration) {
        return find(pluginContainer -> Stream
                .of(pluginContainer
                        .get(ContainerComponentRegistry.class)
                        .getComponents()
                        .get(container.buildAutoIdFromName(plugin))))
                                .filter(Objects::nonNull)
                                .map(component -> ofNullable(component.getProcessors().get(name))
                                        .map(proc -> proc
                                                .getInstantiator()
                                                .apply(configuration == null ? null
                                                        : proc.getMigrationHandler().migrate(version, configuration)))
                                        .map(org.talend.sdk.component.runtime.output.Processor.class::cast))
                                .findFirst()
                                .flatMap(identity());
    }

    public boolean hasPlugin(final String plugin) {
        return container.find(plugin).isPresent();
    }

    public Optional<Container> findPlugin(final String plugin) {
        return container.find(plugin);
    }

    public String addPlugin(final String pluginRootFile) {
        final String id = this.container
                .builder(pluginRootFile)
                .withCustomizer(createContainerCustomizer(pluginRootFile))
                .create()
                .getId();
        info("Adding plugin: " + pluginRootFile + ", as " + id);
        return id;
    }

    public String addWithLocationPlugin(final String location, final String pluginRootFile) {
        final String id = this.container
                .builder(pluginRootFile)
                .withCustomizer(createContainerCustomizer(location))
                .create()
                .getId();
        info("Adding plugin: " + pluginRootFile + ", as " + id);
        return id;
    }

    protected String addPlugin(final String forcedId, final String pluginRootFile) {
        final String id = this.container
                .builder(forcedId, pluginRootFile)
                .withCustomizer(createContainerCustomizer(forcedId))
                .create()
                .getId();
        info("Adding plugin: " + pluginRootFile + ", as " + id);
        return id;
    }

    public void removePlugin(final String id) {
        container.find(id).ifPresent(Container::close);
        info("Removed plugin: " + id);
    }

    protected boolean isContainerClass(final Filter filter, final String name) {
        // workaround until beam is able to have a consistent packaging - i.e. no extensions/io in its core
        if (excludeClassesFilter.accept(name)) {
            // check if it is beam-sdks-java-core, if so then it is considered as a
            // container class
            final URL resource = ComponentManager.class.getClassLoader().getResource(name.replace('.', '/') + ".class");
            if (resource != null) {
                if (resource.getFile().startsWith("mvn:org.apache.beam/beam-sdks-java-core/")) { // studio
                    return true;
                } else if ("jar".equals(resource.getProtocol())) { // standalone
                    final String file = resource.getFile();
                    final int separator = file.indexOf('!');
                    if (separator > 0) {
                        try {
                            return new File(decode(new URL(file.substring(0, separator)).getFile()))
                                    .getName()
                                    .startsWith("beam-sdks-java-core-");

                        } catch (final MalformedURLException e) {
                            // let it return false
                        }
                    }
                }
            }
            return false;
        }
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
                        container.find(plugin).map(Container::getLoader).map(ClassLoader.class::cast).orElse(old));
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(old);
        }
    }

    public List<String> availablePlugins() {
        return container.findAll().stream().map(Container::getId).collect(toList());
    }

    protected void containerServices(final Container container, final Map<Class<?>, Object> services) {
        final String containerId = container.getId();

        // JSON services
        final JsonProvider jsonpProvider = new PreComputedJsonpProvider(containerId, this.jsonpProvider,
                jsonpParserFactory, jsonpWriterFactory, jsonpBuilderFactory, jsonpGeneratorFactory, jsonpReaderFactory);
        services.put(JsonProvider.class, jsonpProvider);
        services
                .put(JsonBuilderFactory.class,
                        javaProxyEnricherFactory
                                .asSerializable(container.getLoader(), containerId, JsonBuilderFactory.class.getName(),
                                        jsonpBuilderFactory));
        services
                .put(JsonParserFactory.class,
                        javaProxyEnricherFactory
                                .asSerializable(container.getLoader(), containerId, JsonParserFactory.class.getName(),
                                        jsonpParserFactory));
        services
                .put(JsonReaderFactory.class,
                        javaProxyEnricherFactory
                                .asSerializable(container.getLoader(), containerId, JsonReaderFactory.class.getName(),
                                        jsonpReaderFactory));
        services
                .put(JsonWriterFactory.class,
                        javaProxyEnricherFactory
                                .asSerializable(container.getLoader(), containerId, JsonWriterFactory.class.getName(),
                                        jsonpWriterFactory));
        services
                .put(JsonGeneratorFactory.class,
                        javaProxyEnricherFactory
                                .asSerializable(container.getLoader(), containerId,
                                        JsonGeneratorFactory.class.getName(), jsonpGeneratorFactory));

        final Jsonb jsonb = jsonbProvider
                .create()
                .withProvider(jsonpProvider) // reuses the same memory buffering
                .withConfig(jsonbConfig)
                .build();
        final Jsonb serializableJsonb = Jsonb.class
                .cast(javaProxyEnricherFactory
                        .asSerializable(container.getLoader(), containerId, Jsonb.class.getName(), jsonb));
        services.put(Jsonb.class, serializableJsonb);

        // not JSON services
        final List<LocalConfiguration> containerConfigurations = new ArrayList<>(localConfigurations);
        if (!Boolean.getBoolean("talend.component.configuration." + containerId + ".ignoreLocalConfiguration")) {
            try (final InputStream stream =
                    container.getLoader().findContainedResource("TALEND-INF/local-configuration" + ".properties")) {
                if (stream != null) {
                    final Properties properties = new Properties();
                    properties.load(stream);
                    containerConfigurations.add(new PropertiesConfiguration(properties));
                }
            } catch (final IOException e) {
                throw new IllegalArgumentException(e);
            }
        }
        services.put(LocalConfiguration.class, new LocalConfigurationService(containerConfigurations, containerId));
        services
                .put(HttpClientFactory.class,
                        new HttpClientFactoryImpl(containerId, reflections, serializableJsonb, services));
        services.put(LocalCache.class, new LocalCacheService(containerId));
        services.put(ProxyGenerator.class, proxyGenerator);
        services.put(Resolver.class, new ResolverImpl(containerId, container.getLocalDependencyRelativeResolver()));
        services.put(Injector.class, new InjectorImpl(containerId, reflections, services));
        services.put(ObjectFactory.class, new ObjectFactoryImpl(containerId, propertyEditorRegistry));
        services.put(RecordBuilderFactory.class, recordBuilderFactoryProvider.apply(containerId));
        services.put(ContainerInfo.class, new ContainerInfo(containerId));
    }

    protected static Collection<LocalConfiguration> createRawLocalConfigurations() {
        final List<LocalConfiguration> configurations = new ArrayList<>(2);
        configurations
                .addAll(toStream(
                        loadServiceProviders(LocalConfiguration.class, LocalConfiguration.class.getClassLoader()))
                                .collect(toList()));
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
                return System.getenv(key);
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

    private Class<?> handleProxy(final Container container, final Class<?> type) {
        if (!proxyGenerator.hasInterceptors(type) && proxyGenerator.isSerializable(type)) {
            return type;
        }
        return container
                .execute(() -> proxyGenerator
                        .generateProxy(container.getLoader(), type, container.getId(), type.getName()));
    }

    private Function<Map<String, String>, Object[]> createParametersFactory(final String plugin,
            final Executable method, final Map<Class<?>, Object> services, final List<ParameterMeta> metas) {
        return executeInContainer(plugin, () -> reflections.parameterFactory(method, services, metas));
    }

    public enum ComponentType {
        MAPPER {

            @Override
            Map<String, ? extends ComponentFamilyMeta.BaseMeta> findMeta(final ComponentFamilyMeta family) {
                return family.getPartitionMappers();
            }
        },
        PROCESSOR {

            @Override
            Map<String, ? extends ComponentFamilyMeta.BaseMeta> findMeta(final ComponentFamilyMeta family) {
                return family.getProcessors();
            }
        };

        abstract Map<String, ? extends ComponentFamilyMeta.BaseMeta> findMeta(ComponentFamilyMeta family);
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

    private class Updater implements ContainerListener {

        private final ModelVisitor visitor = new ModelVisitor();

        @Override
        public void onCreate(final Container container) {
            final ConfigurableClassLoader loader = container.getLoader();
            final OriginalId originalId = OriginalId.class.cast(container.get(OriginalId.class));

            final AnnotationFinder finder;
            Archive archive = null;
            try {
                /*
                 * container.findExistingClasspathFiles() - we just scan the root module for
                 * now, no need to scan all the world
                 */
                archive = toArchive(container.getRootModule(), originalId, loader);

                // undocumented scanning config for now since we would document it only if
                // proven useful
                Filter filter = KnownClassesFilter.INSTANCE;
                try (final InputStream containerFilterConfig =
                        container.getLoader().getResourceAsStream("TALEND-INF/scanning.properties")) {
                    if (containerFilterConfig != null) {
                        final Properties config = new Properties();
                        config.load(containerFilterConfig);
                        final Filter accept = ofNullable(config.getProperty("classloader.includes"))
                                .map(String::trim)
                                .filter(v -> !v.isEmpty())
                                .map(s -> s.split(","))
                                .map(Filters::patterns)
                                .orElseGet(() -> name -> true);
                        final Filter reject = ofNullable(config.getProperty("classloader.excludes"))
                                .map(String::trim)
                                .filter(v -> !v.isEmpty())
                                .map(s -> s.split(","))
                                .map(Filters::patterns)
                                .orElseGet(() -> name -> false);
                        filter = new ExcludeIncludeFilter(accept, reject);
                    }
                } catch (final IOException e) {
                    log.debug(e.getMessage(), e);
                }
                finder = new AnnotationFinder(new FilteredArchive(archive, filter));
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

            final Map<Class<?>, Object> services = new HashMap<>();
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

            finder.findAnnotatedClasses(Internationalized.class).forEach(proxy -> {
                final Object instance = javaProxyEnricherFactory
                        .asSerializable(container.getLoader(), container.getId(), proxy.getName(),
                                internationalizationServiceFactory.create(proxy, container.getLoader()));
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
            final Map<Class<?>, Object> userServices = finder
                    .findAnnotatedClasses(Service.class)
                    .stream()
                    .filter(s -> !services.keySet().contains(s))
                    .collect(toMap(identity(), service -> {
                        try {
                            final Object instance;
                            final Thread thread = Thread.currentThread();
                            final ClassLoader old = thread.getContextClassLoader();
                            thread.setContextClassLoader(container.getLoader());
                            try {
                                instance = handleProxy(container, service).getConstructor().newInstance();
                                if (proxyGenerator.hasInterceptors(service)) {
                                    proxyGenerator
                                            .initialize(instance, new InterceptorHandlerFacade(
                                                    service.getConstructor().newInstance(), services));
                                }
                                return instance;
                            } catch (final InstantiationException | IllegalAccessException e) {
                                throw new IllegalArgumentException(e);
                            } catch (final InvocationTargetException e) {
                                throw toRuntimeException(e);
                            } finally {
                                thread.setContextClassLoader(old);
                            }
                        } catch (final NoSuchMethodException e) {
                            throw new IllegalArgumentException("No default constructor for " + service);
                        }
                    }));
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
                                        instance, serviceMethod))
                                .collect(toList())));
                info("Added @Service " + service + " for container-id=" + container.getId());
            });

            final ComponentContexts componentContexts = new ComponentContexts();
            container.set(ComponentContexts.class, componentContexts);
            Stream
                    .of(PartitionMapper.class, Processor.class, Emitter.class)
                    .flatMap(a -> finder.findAnnotatedClasses(a).stream())
                    .filter(t -> Modifier.isPublic(t.getModifiers()))
                    .forEach(type -> {
                        final Components components = findComponentsConfig(componentDefaults, type,
                                container.getLoader(), Components.class, DEFAULT_COMPONENT);

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

                        final ComponentMetaBuilder builder = new ComponentMetaBuilder(container.getId(), allServices,
                                components, componentDefaults.get(getAnnotatedElementCacheKey(type)), context,
                                migrationHandlerFactory);

                        final Thread thread = Thread.currentThread();
                        final ClassLoader old = thread.getContextClassLoader();
                        thread.setContextClassLoader(container.getLoader());
                        try {
                            visitor.visit(type, builder, !context.isNoValidation());
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
                                        .anyMatch(k -> c.getProcessors().keySet().contains(k))) {
                                    throw new IllegalArgumentException("Conflicting processors in " + c);
                                }
                                if (componentFamilyMeta
                                        .getPartitionMappers()
                                        .keySet()
                                        .stream()
                                        .anyMatch(k -> c.getPartitionMappers().keySet().contains(k))) {
                                    throw new IllegalArgumentException("Conflicting mappers in " + c);
                                }

                                // if we passed validations then merge
                                componentFamilyMeta.getProcessors().putAll(c.getProcessors());
                                componentFamilyMeta.getPartitionMappers().putAll(c.getPartitionMappers());
                            }
                        });

                        info("Parsed component " + type + " for container-id=" + container.getId());
                    });
        }

        private ServiceMeta.ActionMeta createServiceMeta(final Container container,
                final Map<Class<?>, Object> services, final Map<String, AnnotatedElement> componentDefaults,
                final Class<?> service, final Object instance, final Method serviceMethod) {
            final Components components = findComponentsConfig(componentDefaults, serviceMethod.getDeclaringClass(),
                    container.getLoader(), Components.class, DEFAULT_COMPONENT);

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

            final String component;
            try {
                component = ofNullable(String.class.cast(marker.annotationType().getMethod("family").invoke(marker)))
                        .filter(c -> !c.isEmpty())
                        .orElseGet(components::family);
                if (component.isEmpty()) {
                    throw new IllegalArgumentException("No component for " + serviceMethod
                            + ", maybe add a @Components on your package " + service.getPackage());
                }
            } catch (final NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw toRuntimeException(e);
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
                    parameterModelService
                            .buildServiceParameterMetas(serviceMethod,
                                    ofNullable(serviceMethod.getDeclaringClass().getPackage())
                                            .map(Package::getName)
                                            .orElse(""),
                                    new BaseParameterEnricher.Context(LocalConfiguration.class
                                            .cast(container
                                                    .get(AllServices.class)
                                                    .getServices()
                                                    .get(LocalConfiguration.class)))),
                    invoker);
        }

        private Archive toArchive(final String module, final OriginalId originalId,
                final ConfigurableClassLoader loader) {
            final File file = of(new File(module)).filter(File::exists).orElseGet(() -> container.resolve(module));
            if (file.exists()) {
                try {
                    return ClasspathArchive.archive(loader, file.toURI().toURL());
                } catch (final MalformedURLException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            info(module + " is not a file, will try to look it up from a nested maven repository");
            final InputStream nestedJar =
                    loader.getParent().getResourceAsStream(ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY + module);
            if (nestedJar != null) {
                final JarInputStream jarStream;
                try {
                    jarStream = new JarInputStream(nestedJar);
                    log.debug("Found a nested resource for " + module);
                    return new NestedJarArchive(jarStream, loader);
                } catch (final IOException e) {
                    try {
                        nestedJar.close();
                    } catch (final IOException e1) {
                        // no-op
                    }
                }
            }

            throw new IllegalArgumentException("Module error: check that the module exist and is a jar or a directory. "
                    + ofNullable(originalId.getValue()).orElse(module));
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

        private ComponentFamilyMeta component;

        @Override
        public void onPartitionMapper(final Class<?> type, final PartitionMapper partitionMapper) {
            final Constructor<?> constructor = findConstructor(type);
            final List<ParameterMeta> parameterMetas = parameterModelService
                    .buildParameterMetas(constructor, getPackage(type), new BaseParameterEnricher.Context(
                            LocalConfiguration.class.cast(services.getServices().get(LocalConfiguration.class))));
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
                            : config -> new PartitionMapperImpl(component.getName(), name, null, plugin,
                                    partitionMapper.infinite(), doInvoke(constructor, parameterFactory.apply(config)));

            component
                    .getPartitionMappers()
                    .put(name,
                            new ComponentFamilyMeta.PartitionMapperMeta(component, name, findIcon(type),
                                    findVersion(type), type, parameterMetas, instantiator,
                                    migrationHandlerFactory.findMigrationHandler(parameterMetas, type, services),
                                    !context.isNoValidation()));
        }

        @Override
        public void onEmitter(final Class<?> type, final Emitter emitter) {
            final Constructor<?> constructor = findConstructor(type);
            final List<ParameterMeta> parameterMetas = parameterModelService
                    .buildParameterMetas(constructor, getPackage(type), new BaseParameterEnricher.Context(
                            LocalConfiguration.class.cast(services.getServices().get(LocalConfiguration.class))));
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
            component
                    .getPartitionMappers()
                    .put(name,
                            new ComponentFamilyMeta.PartitionMapperMeta(component, name, findIcon(type),
                                    findVersion(type), type, parameterMetas, instantiator,
                                    migrationHandlerFactory.findMigrationHandler(parameterMetas, type, services),
                                    !context.isNoValidation()));
        }

        @Override
        public void onProcessor(final Class<?> type, final Processor processor) {
            final Constructor<?> constructor = findConstructor(type);
            final List<ParameterMeta> parameterMetas = parameterModelService
                    .buildParameterMetas(constructor, getPackage(type), new BaseParameterEnricher.Context(
                            LocalConfiguration.class.cast(services.getServices().get(LocalConfiguration.class))));
            addProcessorsBuiltInParameters(type, parameterMetas);
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
            component
                    .getProcessors()
                    .put(name,
                            new ComponentFamilyMeta.ProcessorMeta(component, name, findIcon(type), findVersion(type),
                                    type, parameterMetas, instantiator,
                                    migrationHandlerFactory.findMigrationHandler(parameterMetas, type, services),
                                    !context.isNoValidation()));
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
                final MaxBatchSizeParamBuilder paramBuilder = new MaxBatchSizeParamBuilder(root);
                final ParameterMeta maxBatchSize = paramBuilder.newBulkParameter();
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

        private String getPackage(final Class<?> type) {
            return ofNullable(type.getPackage()).map(Package::getName).orElse("");
        }

        private int findVersion(final Class<?> type) {
            return ofNullable(type.getAnnotation(Version.class)).map(Version::value).orElse(1);
        }

        private String findIcon(final AnnotatedElement type) {
            return ofNullable(type.getAnnotation(Icon.class))
                    .map(i -> i.value() == Icon.IconType.CUSTOM
                            ? of(i.custom()).filter(s -> !s.isEmpty()).orElse("default")
                            : i.value().getKey())
                    .orElse("default");
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
                            findIcon(familyAnnotationElement), comp,
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
         * @return advanced toggle to ignore built-in beam exclusions and let this customizer override them.
         */
        boolean ignoreBeamClassLoaderExclusions();
    }
}
