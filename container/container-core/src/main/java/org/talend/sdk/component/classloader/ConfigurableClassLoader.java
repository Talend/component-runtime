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
package org.talend.sdk.component.classloader;

import static java.util.Collections.enumeration;
import static java.util.Collections.list;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.jar.JarInputStream;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigurableClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    // note: is there any reason to make it configurable? normally it shouldn't or
    // it
    // breaks some logic.
    public static final String NESTED_MAVEN_REPOSITORY = "MAVEN-INF/repository/";

    private static final ClassLoader SYSTEM_CLASS_LOADER = getSystemClassLoader();

    private final Predicate<String> parentFilter;

    private final Predicate<String> childFirstFilter;

    private final Map<String, Collection<Resource>> resources = new HashMap<>();

    public ConfigurableClassLoader(final URL[] urls, final ClassLoader parent, final Predicate<String> parentFilter,
            final Predicate<String> childFirstFilter, final String[] nestedDependencies) {
        super(urls, parent);
        this.parentFilter = parentFilter;
        this.childFirstFilter = childFirstFilter;
        if (nestedDependencies != null) { // load all in memory to avoid perf issues - should we try offheap?
            final byte[] buffer = new byte[8192]; // should be good for most cases
            final ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.length);
            Stream.of(nestedDependencies).map(d -> NESTED_MAVEN_REPOSITORY + d).forEach(resource -> {
                final URL url = ofNullable(super.findResource(resource)).orElseGet(() -> parent.getResource(resource));
                try (final JarInputStream jarInputStream = new JarInputStream(url.openStream())) {
                    ZipEntry entry;
                    while ((entry = jarInputStream.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            out.reset();

                            int read;
                            while ((read = jarInputStream.read(buffer, 0, buffer.length)) >= 0) {
                                out.write(buffer, 0, read);
                            }

                            resources.computeIfAbsent(entry.getName(), k -> new ArrayList<>()).add(
                                    new Resource(resource, out.toByteArray()));
                        }
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    @Override
    public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        if (name == null) { // some frameworks (hibernate to not cite it) do it
            throw new ClassNotFoundException();
        }

        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz;

            // if in the JVM, never override them
            if (isDirectJvmClass(name)) {
                clazz = loadFromJvm(name, resolve);
                if (clazz != null) {
                    return clazz;
                }
            }

            // already loaded?
            clazz = findLoadedClass(name);
            if (postLoad(resolve, clazz)) {
                return clazz;
            }

            // look for it in this classloader
            final boolean childFirst = childFirstFilter.test(name);
            if (childFirst) {
                clazz = loadInternal(name, resolve);
                if (clazz != null) {
                    return clazz;
                }
            }

            // if parent first or not present in this loader, try the parent chain
            if (parentFilter.test(name)) {
                clazz = loadFromParent(name, resolve);
                if (clazz != null) {
                    return clazz;
                }
            }

            // if this class was a parent first then try to load it now parent loading
            // failed
            if (!childFirst) {
                clazz = loadInternal(name, resolve);
                if (clazz != null) {
                    return clazz;
                }
            }

            if (isJvmJavax(name)) {
                clazz = loadFromJvm(name, resolve);
                if (clazz != null) {
                    return clazz;
                }
            }

            throw new ClassNotFoundException(name);
        }
    }

    @Override
    public URL findResource(final String name) {
        return resources.isEmpty() ? super.findResource(name)
                : ofNullable(super.findResource(name)).orElseGet(() -> ofNullable(resources.get(name))
                        .filter(s -> !s.isEmpty())
                        .map(s -> s.iterator().next())
                        .map(r -> nestedResourceToURL(name, r))
                        .orElse(null));
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        return ofNullable(super.getResourceAsStream(name)).orElseGet(() -> ofNullable(resources.get(name))
                .filter(s -> s.size() > 0)
                .map(s -> s.iterator().next().resource)
                .map(ByteArrayInputStream::new)
                .orElse(null));
    }

    @Override
    public Enumeration<URL> findResources(final String name) throws IOException {
        final Enumeration<URL> delegates = super.findResources(name);
        if (resources.isEmpty()) {
            return delegates;
        }

        final Collection<Resource> nested = ofNullable(resources.get(name)).orElseGet(Collections::emptyList);
        if (nested.isEmpty()) {
            return delegates;
        }
        final Collection<URL> aggregated = new ArrayList<>(list(delegates));
        aggregated.addAll(nested.stream().map(r -> nestedResourceToURL(name, r)).collect(toList()));
        return enumeration(aggregated);
    }

    private URL nestedResourceToURL(final String name, final Resource nestedResource) {
        try {
            return new URL("nested", null, -1, nestedResource.entry + "!/" + name, new Handler(nestedResource));
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isDirectJvmClass(final String name) {
        if (name.startsWith("java.")) {
            return true;
        }
        if (name.startsWith("sun.")) {
            return true;
        }
        if (name.startsWith("jdk.")) {
            return true;
        }
        if (name.startsWith("oracle.")) {
            return true;
        }
        if (name.startsWith("javafx.")) {
            return true;
        }
        if (name.startsWith("netscape.")) {
            return true;
        }
        if (name.startsWith("org.")) {
            final String sub = name.substring("org.".length());
            if (sub.startsWith("w3c.dom.")) {
                return true;
            }
            if (sub.startsWith("omg.")) {
                return true;
            }
            if (sub.startsWith("xml.sax.")) {
                return true;
            }
            if (sub.startsWith("ietf.jgss.")) {
                return true;
            }
            if (sub.startsWith("jcp.xml.dsig.internal.")) {
                return true;
            }
        }
        if (name.startsWith("com.")) {
            final String sub = name.substring("com.".length());
            if (sub.startsWith("oracle.")) {
                return true;
            }
            if (sub.startsWith("sun.")) {
                return true;
            }
        }
        return false;
    }

    // this is a weird way to write it and it could be inlined but it is slower and
    // since loadClass is called a tons of times it is better this way
    private boolean isJvmJavax(final String name) {
        if (name.startsWith("javax.")) {
            final String sub = name.substring("javax.".length());
            if (sub.startsWith("accessibility.")) {
                return true;
            }
            if (sub.startsWith("activation.")) {
                return true;
            }
            if (sub.startsWith("activity.")) {
                return true;
            }
            if (sub.startsWith("annotation.")) {
                return true;
            }
            if (sub.startsWith("imageio.")) {
                return true;
            }
            if (sub.startsWith("jws.")) {
                return true;
            }
            if (sub.startsWith("lang.")) {
                return true;
            }
            if (sub.startsWith("management.")) {
                return true;
            }
            if (sub.startsWith("naming.")) {
                return true;
            }
            if (sub.startsWith("net.")) {
                return true;
            }
            if (sub.startsWith("print.")) {
                return true;
            }
            if (sub.startsWith("rmi.")) {
                return true;
            }
            if (sub.startsWith("script.")) {
                return true;
            }
            if (sub.startsWith("security.")) {
                return true;
            }
            if (sub.startsWith("smartcardio.")) {
                return true;
            }
            if (sub.startsWith("sound.")) {
                return true;
            }
            if (sub.startsWith("sql.")) {
                return true;
            }
            if (sub.startsWith("swing.")) {
                return true;
            }
            if (sub.startsWith("tools.")) {
                return true;
            }
            if (sub.startsWith("transaction.")) {
                return true;
            }
            if (sub.startsWith("xml.")) {
                return true;
            }
            if (sub.startsWith("jnlp.")) {
                return true;
            }
            if (sub.startsWith("crypto.")) {
                return true;
            }
        }
        return false;
    }

    private boolean postLoad(final boolean resolve, final Class<?> clazz) {
        if (clazz != null) {
            if (resolve) {
                resolveClass(clazz);
            }
            return true;
        }
        return false;
    }

    private Class<?> loadFromJvm(final String name, final boolean resolve) {
        Class<?> clazz;
        try {
            clazz = SYSTEM_CLASS_LOADER.loadClass(name);
            if (postLoad(resolve, clazz)) {
                return clazz;
            }
        } catch (final NoClassDefFoundError | ClassNotFoundException ignored) {
            // no-op
        }
        return null;
    }

    private Class<?> loadFromParent(final String name, final boolean resolve) {
        ClassLoader parent = getParent();
        if (parent == null) {
            parent = SYSTEM_CLASS_LOADER;
        }
        try {
            final Class<?> clazz = Class.forName(name, false, parent);
            if (postLoad(resolve, clazz)) {
                return clazz;
            }
        } catch (final ClassNotFoundException ignored) {
            // no-op
        }
        return null;
    }

    private Class<?> loadInternal(final String name, final boolean resolve) {
        Class<?> clazz = null;
        try {
            clazz = findClass(name);
        } catch (final ClassNotFoundException ignored) {
            if (!resources.isEmpty()) {
                final Collection<Resource> resources = this.resources.get(name.replace(".", "/") + ".class");
                if (resources != null && !resources.isEmpty()) {
                    final Resource resource = resources.iterator().next();
                    clazz = defineClass(name, resource.resource, 0, resource.resource.length);
                }
            }
        }
        if (postLoad(resolve, clazz)) {
            return clazz;
        }
        return null;
    }

    @RequiredArgsConstructor
    private static class Resource {

        private final String entry;

        private final byte[] resource;
    }

    @RequiredArgsConstructor
    private static class Handler extends URLStreamHandler {

        private final Resource resource;

        @Override
        protected URLConnection openConnection(final URL url) throws IOException {
            return new Connection(url, resource);
        }
    }

    private static class Connection extends URLConnection {

        private final Resource resource;

        private Connection(final URL url, final Resource resource) {
            super(url);
            this.resource = resource;
        }

        @Override
        public void connect() throws IOException {
            // no-op
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(resource.resource);
        }
    }
}
