/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.json.JsonBuilderFactory;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriterFactory;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.spi.JsonbProvider;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParserFactory;

import org.apache.johnzon.mapper.MapperBuilder;
import org.talend.sdk.component.api.record.RecordPointerFactory;
import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.configuration.LocalConfiguration;
import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.api.service.factory.ObjectFactory;
import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.injector.Injector;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;
import org.talend.sdk.component.api.service.record.RecordService;
import org.talend.sdk.component.api.service.source.ProducerFinder;
import org.talend.sdk.component.runtime.manager.asm.ProxyGenerator;
import org.talend.sdk.component.runtime.manager.json.PreComputedJsonpProvider;
import org.talend.sdk.component.runtime.manager.proxy.JavaProxyEnricherFactory;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.service.api.ComponentInstantiator;
import org.talend.sdk.component.runtime.manager.service.configuration.PropertiesConfiguration;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;
import org.talend.sdk.component.runtime.manager.util.Lazy;
import org.talend.sdk.component.runtime.manager.util.MemoizingSupplier;
import org.talend.sdk.component.runtime.manager.xbean.registry.EnrichedPropertyEditorRegistry;
import org.talend.sdk.component.runtime.record.json.RecordJsonGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultServiceProvider {

    private final ReflectionService reflections;

    private final JsonProvider jsonpProvider;

    private final JsonGeneratorFactory jsonpGeneratorFactory;

    private final JsonReaderFactory jsonpReaderFactory;

    private final JsonBuilderFactory jsonpBuilderFactory;

    private final JsonParserFactory jsonpParserFactory;

    private final JsonWriterFactory jsonpWriterFactory;

    private final JsonbConfig jsonbConfig;

    private final JsonbProvider jsonbProvider;

    private final ProxyGenerator proxyGenerator;

    private final JavaProxyEnricherFactory javaProxyEnricherFactory;

    private final Collection<LocalConfiguration> localConfigurations;

    private final Function<String, RecordBuilderFactory> recordBuilderFactoryProvider;

    private final EnrichedPropertyEditorRegistry propertyEditorRegistry;

    private final Supplier<ScheduledExecutorService> executorService =
            new MemoizingSupplier<>(this::buildExecutorService);

    public <T> T lookup(final String id, final ClassLoader loader, final Supplier<List<InputStream>> localConfigLookup,
            final Function<String, Path> resolver, final Class<T> api,
            final AtomicReference<Map<Class<?>, Object>> services, final ComponentInstantiator.Builder instantiators) {
        return api.cast(doLookup(id, loader, localConfigLookup, resolver, api, services, instantiators));
    }

    private Object doLookup(final String id, final ClassLoader loader,
            final Supplier<List<InputStream>> localConfigLookup, final Function<String, Path> resolver,
            final Class<?> api, final AtomicReference<Map<Class<?>, Object>> services,
            final ComponentInstantiator.Builder instantiators) {
        if (JsonProvider.class == api) {
            return new PreComputedJsonpProvider(id, jsonpProvider, jsonpParserFactory, jsonpWriterFactory,
                    jsonpBuilderFactory, jsonpGeneratorFactory, jsonpReaderFactory);
        }
        if (JsonBuilderFactory.class == api) {
            return javaProxyEnricherFactory
                    .asSerializable(loader, id, JsonBuilderFactory.class.getName(), jsonpBuilderFactory);
        }
        if (JsonParserFactory.class == api) {
            return javaProxyEnricherFactory
                    .asSerializable(loader, id, JsonParserFactory.class.getName(), jsonpParserFactory);
        }
        if (JsonReaderFactory.class == api) {
            return javaProxyEnricherFactory
                    .asSerializable(loader, id, JsonReaderFactory.class.getName(), jsonpReaderFactory);
        }
        if (JsonWriterFactory.class == api) {
            return javaProxyEnricherFactory
                    .asSerializable(loader, id, JsonWriterFactory.class.getName(), jsonpWriterFactory);
        }
        if (JsonGeneratorFactory.class == api) {
            return javaProxyEnricherFactory
                    .asSerializable(loader, id, JsonGeneratorFactory.class.getName(), jsonpGeneratorFactory);
        }
        if (Jsonb.class == api) {
            final JsonbBuilder jsonbBuilder = createPojoJsonbBuilder(id,
                    Lazy
                            .lazy(() -> Jsonb.class
                                    .cast(doLookup(id, loader, localConfigLookup, resolver, Jsonb.class, services,
                                            instantiators))));
            return new GenericOrPojoJsonb(id, jsonbProvider
                    .create()
                    .withProvider(jsonpProvider) // reuses the same memory buffering
                    .withConfig(jsonbConfig)
                    .build(), jsonbBuilder.build());
        }
        if (LocalConfiguration.class == api) {
            final List<LocalConfiguration> containerConfigurations = new ArrayList<>(localConfigurations);
            if (!Boolean.getBoolean("talend.component.configuration." + id + ".ignoreLocalConfiguration")) {
                final Stream<InputStream> localConfigs = localConfigLookup.get().stream();
                final Properties aggregatedLocalConfigs = aggregateConfigurations(localConfigs);
                if (!aggregatedLocalConfigs.isEmpty()) {
                    containerConfigurations.add(new PropertiesConfiguration(aggregatedLocalConfigs));
                }
            }
            return new LocalConfigurationService(containerConfigurations, id);
        }
        if (RecordBuilderFactory.class == api) {
            return recordBuilderFactoryProvider.apply(id);
        }
        if (ProxyGenerator.class == api) {
            return proxyGenerator;
        }
        if (LocalCache.class == api) {
            final LocalCacheService service =
                    new LocalCacheService(id, System::currentTimeMillis, this.executorService);
            Injector.class.cast(services.get().get(Injector.class)).inject(service);
            return service;
        }
        if (Injector.class == api) {
            return new InjectorImpl(id, reflections, proxyGenerator, services.get());
        }
        if (HttpClientFactory.class == api) {
            return new HttpClientFactoryImpl(id, reflections, Jsonb.class.cast(services.get().get(Jsonb.class)),
                    services.get());
        }
        if (Resolver.class == api) {
            return new ResolverImpl(id, resolver);
        }
        if (ObjectFactory.class == api) {
            return new ObjectFactoryImpl(id, propertyEditorRegistry);
        }
        if (ProducerFinder.class == api) {
            final RecordService recordService =
                    this.lookup(id, loader, localConfigLookup, resolver, RecordService.class, services, instantiators);
            Iterator<ProducerFinder> producerFinders = ServiceLoader.load(ProducerFinder.class, loader).iterator();
            if (producerFinders.hasNext()) {
                ProducerFinder producerFinder = producerFinders.next();
                if (producerFinders.hasNext()) {
                    log.warn("More than one ProducerFinder are available via SPI, using {}.",
                            producerFinder.getClass().getSimpleName());
                }
                return producerFinder.init(id, instantiators, recordService::toRecord);
            } else {
                return new ProducerFinderImpl().init(id, instantiators, recordService::toRecord);
            }
        }
        if (RecordPointerFactory.class == api) {
            return new RecordPointerFactoryImpl(id);
        }
        if (ContainerInfo.class == api) {
            return new ContainerInfo(id);
        }
        if (RecordService.class == api) {
            return new RecordServiceImpl(id, recordBuilderFactoryProvider.apply(id), () -> jsonpBuilderFactory,
                    () -> jsonpProvider,
                    Lazy
                            .lazy(() -> Jsonb.class
                                    .cast(doLookup(id, loader, localConfigLookup, resolver, Jsonb.class, services,
                                            instantiators))));
        }
        return null;
    }

    private JsonbBuilder createPojoJsonbBuilder(final String id, final Supplier<Jsonb> jsonb) {
        final JsonbBuilder jsonbBuilder = JsonbBuilder
                .newBuilder()
                .withProvider(new PreComputedJsonpProvider(id, jsonpProvider, jsonpParserFactory, jsonpWriterFactory,
                        jsonpBuilderFactory,
                        new RecordJsonGenerator.Factory(Lazy.lazy(() -> recordBuilderFactoryProvider.apply(id)), jsonb,
                                emptyMap()),
                        jsonpReaderFactory))
                .withConfig(jsonbConfig);
        try { // to passthrough the writer, otherwise RecoderJsonGenerator is broken
            final Field mapper = jsonbBuilder.getClass().getDeclaredField("builder");
            if (!mapper.isAccessible()) {
                mapper.setAccessible(true);
            }
            MapperBuilder.class.cast(mapper.get(jsonbBuilder)).setDoCloseOnStreams(true);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        return jsonbBuilder;
    }

    private Properties aggregateConfigurations(final Stream<InputStream> localConfigs) {
        final AtomicReference<RuntimeException> re = new AtomicReference<>();
        final Properties result = localConfigs.map(stream -> {
            final Properties properties = new Properties();
            try {
                if (stream != null) {
                    properties.load(stream);
                }
                return properties;
            } catch (final IOException e) {
                log.error(e.getMessage(), e);
                RuntimeException runtimeException = re.get();
                if (runtimeException == null) {
                    runtimeException = new IllegalStateException("Can't read all local configurations");
                    re.set(runtimeException);
                }
                runtimeException.addSuppressed(e);
                return properties;
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (final IOException e) {
                        // no-op
                    }
                }
            }
        })
                .sorted(comparing(it -> Integer.parseInt(it.getProperty("_ordinal", "0"))))
                .reduce(new Properties(), (p1, p2) -> {
                    p1.putAll(p2);
                    return p1;
                });
        final RuntimeException error = re.get();
        if (error != null) {
            throw error;
        }
        return result;
    }

    /**
     * Build executor service
     * used by
     * - Local cache for eviction.
     *
     * @return scheduled executor service.
     */
    private ScheduledExecutorService buildExecutorService() {
        return Executors.newScheduledThreadPool(4, (Runnable r) -> {
            final Thread thread = new Thread(r, DefaultServiceProvider.class.getName() + "-services-" + hashCode());
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        });
    }
}
