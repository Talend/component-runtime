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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.enumeration;
import static java.util.Collections.list;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import lombok.Getter;
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

    @Getter
    private final String id;

    private final URL[] creationUrls;

    private final Predicate<String> parentFilter;

    private final Predicate<String> childFirstFilter;

    private final Map<String, Collection<Resource>> resources = new HashMap<>();

    private final Collection<ClassFileTransformer> transformers = new ArrayList<>();

    private final WeakHashMap<Closeable, Void> closeables = new WeakHashMap<>();

    private volatile URLClassLoader temporaryCopy;

    public ConfigurableClassLoader(final String id, final URL[] urls, final ClassLoader parent,
            final Predicate<String> parentFilter, final Predicate<String> childFirstFilter,
            final String[] nestedDependencies) {
        this(id, urls, parent, parentFilter, childFirstFilter, emptyMap());
        if (nestedDependencies != null) { // load all in memory to avoid perf issues - should we try offheap?
            final byte[] buffer = new byte[8192]; // should be good for most cases
            final ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.length);
            Stream.of(nestedDependencies).map(d -> NESTED_MAVEN_REPOSITORY + d).forEach(resource -> {
                final URL url = ofNullable(super.findResource(resource)).orElseGet(() -> parent.getResource(resource));
                if (url == null) {
                    throw new IllegalArgumentException("Didn't find " + resource + " in " + asList(nestedDependencies));
                }
                try (final JarInputStream jarInputStream = new JarInputStream(url.openStream())) {
                    ZipEntry entry;
                    while ((entry = jarInputStream.getNextEntry()) != null) {
                        if (!entry.isDirectory()) {
                            out.reset();

                            int read;
                            while ((read = jarInputStream.read(buffer, 0, buffer.length)) >= 0) {
                                out.write(buffer, 0, read);
                            }

                            resources
                                    .computeIfAbsent(entry.getName(), k -> new ArrayList<>())
                                    .add(new Resource(resource, out.toByteArray()));
                        }
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }

    private ConfigurableClassLoader(final String id, final URL[] urls, final ClassLoader parent,
            final Predicate<String> parentFilter, final Predicate<String> childFirstFilter,
            final Map<String, Collection<Resource>> resources) {
        super(urls, parent);
        this.id = id;
        this.creationUrls = urls;
        this.parentFilter = parentFilter;
        this.childFirstFilter = childFirstFilter;
        this.resources.putAll(resources);
    }

    public void registerTransformer(final ClassFileTransformer transformer) {
        transformers.add(transformer);
    }

    public synchronized URLClassLoader createTemporaryCopy() {
        final ConfigurableClassLoader self = this;
        return temporaryCopy == null ? temporaryCopy =
                new ConfigurableClassLoader(id, creationUrls, getParent(), parentFilter, childFirstFilter, resources) {

                    @Override
                    public synchronized void close() throws IOException {
                        super.close();
                        synchronized (self) {
                            self.temporaryCopy = null;
                        }
                    }
                } : temporaryCopy;
    }

    @Override
    public synchronized void close() throws IOException {
        resources.clear();
        if (temporaryCopy != null) {
            try {
                temporaryCopy.close();
            } catch (final RuntimeException re) {
                log.warn(re.getMessage(), re);
            }
        }
        synchronized (closeables) {
            closeables.keySet().forEach(c -> {
                try {
                    c.close();
                } catch (final IOException ex) {
                    log.warn(ex.getMessage(), ex);
                }
            });
            closeables.clear();
        }
        super.close();
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

            if (shouldFallbackOnJvmLoading(name)) {
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
                : ofNullable(super.findResource(name))
                        .orElseGet(() -> ofNullable(resources.get(name))
                                .filter(s -> !s.isEmpty())
                                .map(s -> s.iterator().next())
                                .map(r -> nestedResourceToURL(name, r))
                                .orElse(null));
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        return ofNullable(doGetResourceAsStream(name))
                .orElseGet(() -> ofNullable(resources.get(name))
                        .filter(s -> s.size() > 0)
                        .map(s -> s.iterator().next().resource)
                        .map(ByteArrayInputStream::new)
                        .orElse(null));
    }

    private InputStream doGetResourceAsStream(final String name) {
        final URL resource = super.findResource(name);
        try {
            if (resource == null) {
                return super.getResourceAsStream(name);
            }
            final URLConnection urlc = resource.openConnection();
            final InputStream is = urlc.getInputStream();
            if (JarURLConnection.class.isInstance(urlc)) {
                final JarURLConnection juc = JarURLConnection.class.cast(urlc);
                final JarFile jar = juc.getJarFile();
                synchronized (closeables) {
                    if (!closeables.containsKey(jar)) {
                        closeables.put(jar, null);
                    }
                }
            } else if (urlc.getClass().getName().equals("sun.net.www.protocol.file.FileURLConnection")) {
                synchronized (closeables) {
                    closeables.put(is, null);
                }
            }
            return is;
        } catch (final IOException e) {
            return null;
        }
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

    public InputStream findContainedResource(final String name) {
        return ofNullable(super.findResource(name)).map(u -> {
            try {
                return u.openStream();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        })
                .orElseGet(() -> ofNullable(resources.get(name))
                        .filter(s -> s.size() > 0)
                        .map(s -> s.iterator().next().resource)
                        .map(ByteArrayInputStream::new)
                        .orElse(null));
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
                final String subSun = sub.substring("sun.".length());
                if (subSun.startsWith("accessibility.")) {
                    return true;
                }
                if (subSun.startsWith("activation.")) {
                    return true;
                }
                if (subSun.startsWith("awt.")) {
                    return true;
                }
                if (subSun.startsWith("beans.")) {
                    return true;
                }
                if (subSun.startsWith("corba.se.")) {
                    return true;
                }
                if (subSun.startsWith("demo.jvmti.")) {
                    return true;
                }
                if (subSun.startsWith("image.codec.jpeg.")) {
                    return true;
                }
                if (subSun.startsWith("imageio.")) {
                    return true;
                }
                if (subSun.startsWith("istack.internal.")) {
                    return true;
                }
                if (subSun.startsWith("java.")) {
                    return true;
                }
                if (subSun.startsWith("java_cup.")) {
                    return true;
                }
                if (subSun.startsWith("jmx.")) {
                    return true;
                }
                if (subSun.startsWith("jndi.")) {
                    return true;
                }
                if (subSun.startsWith("management.")) {
                    return true;
                }
                if (subSun.startsWith("media.sound.")) {
                    return true;
                }
                if (subSun.startsWith("naming.internal.")) {
                    return true;
                }
                if (subSun.startsWith("net.")) {
                    return true;
                }
                if (subSun.startsWith("nio.")) {
                    return true;
                }
                if (subSun.startsWith("org.")) {
                    return true;
                }
                if (subSun.startsWith("rmi.rmid.")) {
                    return true;
                }
                if (subSun.startsWith("rowset.")) {
                    return true;
                }
                if (subSun.startsWith("security.")) {
                    return true;
                }
                if (subSun.startsWith("swing.")) {
                    return true;
                }
                if (subSun.startsWith("tracing.")) {
                    return true;
                }
                if (subSun.startsWith("xml.internal.")) {
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    // this is a weird way to write it and it could be inlined but it is slower and
    // since loadClass is called a tons of times it is better this way
    private boolean shouldFallbackOnJvmLoading(final String name) {
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
        if (name.startsWith("scala.")) {
            return true;
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
        final String resourceName = name.replace('.', '/');
        final String path = resourceName.concat(".class");
        final URL url = super.findResource(path);
        if (url != null) {
            try {
                final URLConnection connection = url.openConnection();
                connection.setUseCaches(false);

                // package
                final int i = name.lastIndexOf('.');
                if (i != -1) {
                    final String pckName = name.substring(0, i);
                    final Package pck = super.getPackage(pckName);
                    if (pck == null) {
                        final Manifest manifest = JarURLConnection.class.isInstance(connection)
                                ? JarURLConnection.class.cast(connection).getManifest()
                                : null;
                        if (manifest == null) {
                            definePackage(pckName, null, null, null, null, null, null, null);
                        } else {
                            definePackage(pckName, manifest, JarURLConnection.class.cast(connection).getJarFileURL());
                        }
                    }
                }

                // read the class and transform it
                byte[] bytes;
                try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int read;
                    try (final InputStream stream = connection.getInputStream()) {
                        while ((read = stream.read(buffer)) >= 0) {
                            if (read == 0) {
                                continue;
                            }
                            outputStream.write(buffer, 0, read);
                        }
                    }
                    bytes = outputStream.toByteArray();
                }
                final Certificate[] certificates = JarURLConnection.class.isInstance(connection)
                        ? JarURLConnection.class.cast(connection).getCertificates()
                        : new Certificate[0];
                if (!transformers.isEmpty()) {
                    for (final ClassFileTransformer transformer : transformers) {
                        try {
                            bytes = transformer.transform(this, resourceName, null, null, bytes);
                        } catch (final IllegalClassFormatException e) {
                            log.error(e.getMessage() + ", will ignore the transformers", e);
                            break;
                        }
                    }
                }
                clazz = super.defineClass(name, bytes, 0, bytes.length, new CodeSource(url, certificates));
            } catch (final IOException e) {
                log.warn(e.getMessage(), e);
                return null;
            }
        } else if (!resources.isEmpty()) {
            final Collection<Resource> resources = this.resources.get(name.replace(".", "/") + ".class");
            if (resources != null && !resources.isEmpty()) {
                final Resource resource = resources.iterator().next();
                clazz = defineClass(name, resource.resource, 0, resource.resource.length);
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
