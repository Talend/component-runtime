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
package org.talend.sdk.component.container;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.lang.UnsafeSupplier;
import org.talend.sdk.component.lifecycle.Lifecycle;
import org.talend.sdk.component.lifecycle.LifecycleSupport;
import org.talend.sdk.component.proxy.ApiHandler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Container implements Lifecycle {

    private final AtomicReference<ConfigurableClassLoader> loaderRef = new AtomicReference<>();

    @Getter
    private final String id;

    @Getter
    private final String rootModule;

    @Getter
    private final Artifact[] dependencies;

    private final AtomicReference<Date> created = new AtomicReference<>();

    private final Supplier<ConfigurableClassLoader> classloaderProvider;

    @Getter
    private final Function<String, File> localDependencyRelativeResolver;

    private final LifecycleSupport lifecycle = new LifecycleSupport();

    private final ConcurrentMap<Class<?>, Object> data = new ConcurrentHashMap<>();

    public Container(final String id, final String rootModule, final Artifact[] dependencies,
            final ContainerManager.ClassLoaderConfiguration configuration,
            final Function<String, File> localDependencyRelativeResolver) {
        this.id = id;
        this.rootModule = rootModule;
        this.dependencies = dependencies;
        this.localDependencyRelativeResolver = localDependencyRelativeResolver;
        this.classloaderProvider = () -> {
            final URL[] urls = findExistingClasspathFiles().map(f -> {
                try {
                    return f.toURI().toURL();
                } catch (final MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
            }).toArray(URL[]::new);

            // for the jar module we test in order:
            // - if the file exists we use it
            // - if the nested file exists using the module as path in nested maven repo,
            // we use it
            // - if the nested path is in the global plugin.properties index, we use it
            return new ConfigurableClassLoader(urls, configuration.getParent(), configuration.getClassesFilter(),
                    configuration.getParentClassesFilter(),
                    configuration.isSupportsResourceDependencies()
                            ? Stream
                                    .concat(Stream.of(dependencies).map(Artifact::toPath),
                                            of(rootModule)
                                                    .filter(m -> !new File(m).exists()
                                                            && !localDependencyRelativeResolver.apply(m).exists())
                                                    .map(Stream::of)
                                                    .orElseGet(Stream::empty))
                                    .toArray(String[]::new)
                            : null);
        };
        reload();
    }

    public <T> T set(final Class<T> key, final T instance) {
        return (T) data.put(key, instance);
    }

    public <T> T get(final Class<T> key) {
        return (T) data.get(key);
    }

    public <T> T remove(final Class<T> key) {
        return (T) data.remove(key);
    }

    public Stream<File> findExistingClasspathFiles() {
        return Stream
                .concat(getContainerFile().map(Stream::of).orElseGet(Stream::empty),
                        Stream.of(dependencies).map(Artifact::toPath).map(localDependencyRelativeResolver))
                .filter(File::exists);
    }

    public Optional<File> getContainerFile() {
        return Optional.of(rootModule).map(
                m -> of(new File(m)).filter(File::exists).orElseGet(() -> localDependencyRelativeResolver.apply(m)));
    }

    public Stream<Artifact> findDependencies() {
        return Stream.of(dependencies);
    }

    public <S, T> T executeAndContextualize(final Supplier<S> supplier, final Class<T> api) {
        checkState();
        if (!api.isInterface()) {
            throw new IllegalArgumentException("Only interfaces are supported for now: " + api);
        }
        try { // ensure we don't leak a specific class, no need of any proxy for the class
              // failing here
            loaderRef.get().getParent().loadClass(api.getName());
        } catch (final NoClassDefFoundError | ClassNotFoundException e) {
            throw new IllegalArgumentException("executeAndContextualize only usable with parent API");
        }

        final S result = execute(supplier);
        return api.isInstance(result) ? api.cast(result)
                : api.cast(newProxyInstance(loaderRef.get(), new Class<?>[] { api },
                        new ApiHandler(result, api, this::withTccl)));
    }

    public <T> T execute(final Supplier<T> supplier) {
        try {
            return withTccl(supplier::get);
        } catch (final RuntimeException | Error re) {
            throw re;
        } catch (final Throwable throwable) { // unlikely
            throw new IllegalStateException(throwable);
        }
    }

    public ConfigurableClassLoader getLoader() {
        return loaderRef.get();
    }

    @Override
    public synchronized void close() {
        lifecycle.closeIfNeeded(() -> {
            doClose();
            loaderRef.set(null);
        });
    }

    @Override
    public boolean isClosed() {
        return lifecycle.isClosed();
    }

    public synchronized void reload() {
        checkState();
        doClose();
        loaderRef.set(classloaderProvider.get());
        created.set(new Date());
    }

    public Date getCreated() {
        return created.get();
    }

    private void checkState() {
        if (lifecycle.isClosed()) {
            throw new IllegalStateException("Container '" + id + "' is already closed");
        }
    }

    private void doClose() {
        ofNullable(loaderRef.get()).filter(Closeable.class::isInstance).map(Closeable.class::cast).ifPresent(c -> {
            try {
                c.close();
            } catch (final IOException e) {
                log.debug(e.getMessage(), e);
            }
        });
    }

    private <T> T withTccl(final UnsafeSupplier<T> supplier) throws Throwable {
        checkState();
        final Thread thread = Thread.currentThread();
        final ClassLoader old = thread.getContextClassLoader();
        final ClassLoader contextualLoader = loaderRef.get();
        thread.setContextClassLoader(contextualLoader);
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(old);
        }
    }
}
