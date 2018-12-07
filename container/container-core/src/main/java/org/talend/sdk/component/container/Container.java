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
package org.talend.sdk.component.container;

import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Collections.list;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.talend.sdk.component.container.Container.State.CREATED;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
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

    private final AtomicReference<Date> lastModifiedTimestamp = new AtomicReference<>();

    private final Supplier<ConfigurableClassLoader> classloaderProvider;

    @Getter
    private final Function<String, File> localDependencyRelativeResolver;

    private final LifecycleSupport lifecycle = new LifecycleSupport();

    private final ConcurrentMap<Class<?>, Object> data = new ConcurrentHashMap<>();

    private final AtomicReference<State> state = new AtomicReference<>(CREATED);

    private final Collection<ClassFileTransformer> transformers = new ArrayList<>();

    public Container(final String id, final String rootModule, final Artifact[] dependencies,
            final ContainerManager.ClassLoaderConfiguration configuration,
            final Function<String, File> localDependencyRelativeResolver, final Consumer<Container> initializer) {
        this.id = id;
        this.rootModule = rootModule;
        this.dependencies = dependencies;
        this.localDependencyRelativeResolver = localDependencyRelativeResolver;
        this.lastModifiedTimestamp.set(new Date(0));
        ofNullable(initializer).ifPresent(i -> i.accept(this));

        this.classloaderProvider = () -> {
            final URL[] urls = findExistingClasspathFiles().peek(this::visitLastModified).map(f -> {
                try {
                    return f.toURI().toURL();
                } catch (final MalformedURLException e) {
                    throw new IllegalStateException(e);
                }
            }).toArray(URL[]::new);

            final ContainerManager.ClassLoaderConfiguration overrideClassLoaderConfig =
                    ofNullable(get(ContainerManager.ClassLoaderConfiguration.class)).orElse(configuration);

            // for the jar module we test in order:
            // - if the file exists we use it
            // - if the nested file exists using the module as path in nested maven repo,
            // we use it
            // - if the nested path is in the global plugin.properties index, we use it
            final File rootFile = of(rootModule)
                    .map(File::new)
                    .filter(File::exists)
                    .orElseGet(() -> localDependencyRelativeResolver.apply(rootModule));
            final Predicate<String> resourceExists = of(rootFile)
                    .filter(File::exists)
                    .filter(it -> it.getName().endsWith(".jar"))
                    .map(this::jarIndex)
                    .orElseGet(() -> s -> of(new File(rootFile, ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY + s))
                            .map(File::exists)
                            .filter(it -> it)
                            .orElseGet(() -> findNestedDependency(overrideClassLoaderConfig, s)));
            final String[] rawNestedDependencies =
                    overrideClassLoaderConfig.isSupportsResourceDependencies()
                            ? Stream
                                    .concat(Stream.of(rootModule), Stream.of(dependencies).map(Artifact::toPath))
                                    .filter(resourceExists)
                                    .distinct()
                                    .toArray(String[]::new)
                            : null;
            final ConfigurableClassLoader loader = new ConfigurableClassLoader(id, urls,
                    overrideClassLoaderConfig.getParent(), overrideClassLoaderConfig.getParentClassesFilter(),
                    overrideClassLoaderConfig.getClassesFilter(), rawNestedDependencies);
            transformers.forEach(loader::registerTransformer);
            return loader;
        };
        reload();
    }

    private boolean findNestedDependency(final ContainerManager.ClassLoaderConfiguration overrideClassLoaderConfig,
            final String depPath) {
        final URL url = overrideClassLoaderConfig
                .getParent()
                .getResource(ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY + depPath);
        return url != null;
    }

    private void visitLastModified(final File f) {
        long lastModified = f.lastModified();
        if (lastModified <= 0) { // generally does the same but some OS don't
            try {
                final FileTime lastModifiedTime = Files.getLastModifiedTime(f.toPath());
                lastModified = lastModifiedTime.toMillis();
            } catch (final IOException e) {
                // no-op
            }
        }
        if (lastModified > 0 && new Date(lastModified).compareTo(lastModifiedTimestamp.get()) > 0) {
            lastModifiedTimestamp.set(new Date(lastModified));
        }
    }

    // we use that to prefilter the dependencies we keep, in some env we don't nest them
    // so we don't care much testing the nested jars
    private Predicate<String> jarIndex(final File rootFile) {
        try (final JarFile jarFile = new JarFile(rootFile)) {
            final Set<String> entries = list(jarFile.entries())
                    .stream()
                    .map(JarEntry::getName)
                    .filter(n -> n.startsWith(ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY))
                    .map(n -> n.substring(ConfigurableClassLoader.NESTED_MAVEN_REPOSITORY.length()))
                    .collect(toSet());
            return entries::contains;
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
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
        return Optional
                .of(rootModule)
                .map(m -> of(new File(m))
                        .filter(File::exists)
                        .orElseGet(() -> localDependencyRelativeResolver.apply(m)));
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
                : api
                        .cast(newProxyInstance(loaderRef.get(), new Class<?>[] { api },
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

    public State getState() {
        return state.get();
    }

    public void setState(final State newState) {
        state.set(newState);
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
        this.created.set(new Date());
    }

    public Date getLastModifiedTimestamp() {
        return lastModifiedTimestamp.get();
    }

    public Date getCreated() {
        return created.get();
    }

    public void registerTransformer(final ClassFileTransformer transformer) {
        transformers.add(transformer);
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

    public enum State {
        CREATED,
        DEPLOYED,
        ON_ERROR,
        UNDEPLOYING,
        UNDEPLOYED
    }
}
