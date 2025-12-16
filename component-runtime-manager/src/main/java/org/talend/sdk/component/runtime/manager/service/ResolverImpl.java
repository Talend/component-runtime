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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import org.talend.sdk.component.api.service.dependency.Resolver;
import org.talend.sdk.component.classloader.ConfigurableClassLoader;
import org.talend.sdk.component.dependencies.maven.Artifact;
import org.talend.sdk.component.dependencies.maven.MvnDependencyListLocalRepositoryResolver;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResolverImpl implements Resolver, Serializable {

    private final String plugin;

    private final Function<String, Path> fileResolver;

    @Override
    public ClassLoaderDescriptor mapDescriptorToClassLoader(final InputStream descriptor) {
        final Collection<URL> urls = new ArrayList<>();
        final Collection<String> nested = new ArrayList<>();
        final Collection<String> resolved = new ArrayList<>();
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final ClassLoader loader =
                ofNullable(classLoader).map(ClassLoader::getParent).orElseGet(ClassLoader::getSystemClassLoader);
        try {
            new MvnDependencyListLocalRepositoryResolver(null, fileResolver)
                    .resolveFromDescriptor(descriptor)
                    .forEach(artifact -> {
                        final String path = artifact.toPath();
                        final Path file = fileResolver.apply(path);
                        if (Files.exists(file)) {
                            try {
                                urls.add(file.toUri().toURL());
                                resolved.add(artifact.toCoordinate());
                            } catch (final MalformedURLException e) {
                                throw new IllegalStateException(e);
                            }
                        } else if (loader.getResource(path) != null) {
                            nested.add(path);
                            resolved.add(artifact.toCoordinate());
                        } // else will be missing
                    });
            final ConfigurableClassLoader volatileLoader = new ConfigurableClassLoader(plugin + "#volatile-resolver",
                    urls.toArray(new URL[0]), classLoader, it -> false, it -> true, nested.toArray(new String[0]),
                    ConfigurableClassLoader.class.isInstance(classLoader)
                            ? ConfigurableClassLoader.class.cast(classLoader).getJvmMarkers()
                            : new String[] { "" });
            return new ClassLoaderDescriptorImpl(volatileLoader, resolved);
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Collection<File> resolveFromDescriptor(final InputStream descriptor) {
        try {
            return new MvnDependencyListLocalRepositoryResolver(null, fileResolver)
                    .resolveFromDescriptor(descriptor)
                    .map(Artifact::toPath)
                    .map(fileResolver)
                    .map(Path::toFile)
                    .collect(toList());
        } catch (final IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, Resolver.class.getName());
    }

    @RequiredArgsConstructor
    private static class ClassLoaderDescriptorImpl implements ClassLoaderDescriptor {

        private final ConfigurableClassLoader volatileLoader;

        private final Collection<String> resolved;

        @Override
        public ClassLoader asClassLoader() {
            return volatileLoader;
        }

        @Override
        public Collection<String> resolvedDependencies() {
            return resolved;
        }

        @Override
        public void close() throws Exception {
            volatileLoader.close();
        }
    }
}
