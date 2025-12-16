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
package org.talend.sdk.component.classloader;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.enumeration;
import static java.util.Collections.list;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static org.talend.sdk.component.jar.Jars.toPath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
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

    private static final Certificate[] NO_CERTIFICATES = new Certificate[0];

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

    @Getter
    private final Predicate<String> parentFilter;

    @Getter
    private final Predicate<String> childFirstFilter;

    @Getter
    private final Predicate<String> resourcesFilter;

    private final Map<String, Collection<Resource>> resources = new HashMap<>();

    private final Collection<ClassFileTransformer> transformers = new ArrayList<>();

    private final WeakHashMap<Closeable, Void> closeables = new WeakHashMap<>();

    private final String[] fullPathJvmPrefixes;

    private final String[] nameJvmPrefixes;

    private final ConcurrentMap<String, Class<?>> proxies = new ConcurrentHashMap<>();

    private volatile URLClassLoader temporaryCopy;

    private final URLClassLoader classLoaderFromClasspath;

    @Getter
    private final List<String> cacheableClasses;

    public ConfigurableClassLoader(final String id, final URL[] urls, final ClassLoader parent,
            final Predicate<String> parentFilter, final Predicate<String> childFirstFilter,
            final String[] nestedDependencies, final String[] jvmPrefixes) {
        this(id, urls, parent, parentFilter, childFirstFilter, emptyMap(), jvmPrefixes, (name) -> false);
        if (nestedDependencies != null) {
            loadNestedDependencies(parent, nestedDependencies);
        }
    }

    public ConfigurableClassLoader(final String id, final URL[] urls, final ClassLoader parent,
            final Predicate<String> parentFilter, final Predicate<String> childFirstFilter,
            final String[] nestedDependencies, final String[] jvmPrefixes, final Predicate<String> resourcesFilter) {
        this(id, urls, parent, parentFilter, childFirstFilter, emptyMap(), jvmPrefixes, resourcesFilter);
        if (nestedDependencies != null) {
            loadNestedDependencies(parent, nestedDependencies);
        }
    }

    private ConfigurableClassLoader(final String id, final URL[] urls, final ClassLoader parent,
            final Predicate<String> parentFilter, final Predicate<String> childFirstFilter,
            final Map<String, Collection<Resource>> resources, final String[] jvmPrefixes,
            final Predicate<String> resourcesFilter) {
        super(urls, parent);
        this.id = id;
        this.creationUrls = urls;
        this.parentFilter = parentFilter;
        this.childFirstFilter = childFirstFilter;
        this.resourcesFilter = resourcesFilter;
        this.resources.putAll(resources);

        this.fullPathJvmPrefixes =
                Stream.of(jvmPrefixes).filter(it -> it.contains(File.separator)).toArray(String[]::new);
        this.nameJvmPrefixes = Stream
                .of(jvmPrefixes)
                .filter(it -> Stream.of(this.fullPathJvmPrefixes).noneMatch(it::equals))
                .toArray(String[]::new);
        classLoaderFromClasspath = createClassLoaderFromClasspath();

        final String useURLConnectionCacheClassList = System.getProperty("talend.tccl.cacheable.classes");
        if (useURLConnectionCacheClassList != null) {
            cacheableClasses = asList(useURLConnectionCacheClassList.replace('.', '/').split(","));
        } else {
            cacheableClasses = Collections.emptyList();
        }
    }

    // load all in memory to avoid perf issues - should we try offheap?
    private void loadNestedDependencies(final ClassLoader parent, final String[] nestedDependencies) {
        final byte[] buffer = new byte[8192]; // should be good for most cases
        final ByteArrayOutputStream out = new ByteArrayOutputStream(buffer.length);
        Stream.of(nestedDependencies).forEach(resource -> {
            final URL url = ofNullable(super.findResource(resource)).orElseGet(() -> parent.getResource(resource));
            if (url == null) {
                throw new IllegalArgumentException("Didn't find " + resource + " in " + asList(nestedDependencies));
            }
            final Map<String, Resource> resources = new HashMap<>();
            final URLConnection urlConnection;
            final Manifest manifest;
            final CodeSource codeSource;
            try {
                urlConnection = url.openConnection();
                if (JarURLConnection.class.isInstance(urlConnection)) {
                    final JarURLConnection juc = JarURLConnection.class.cast(urlConnection);
                    manifest = juc.getManifest();

                    final Certificate[] certificates = juc.getCertificates();
                    codeSource = new CodeSource(url, certificates);
                } else { // unlikely
                    manifest = null;
                    codeSource = null;
                }
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            try (final JarInputStream jarInputStream = new JarInputStream(urlConnection.getInputStream())) {
                ZipEntry entry;
                while ((entry = jarInputStream.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        if (isBlacklisted(entry.getName())) {
                            logUnexpectedDependency(url, entry.getName());
                            continue;
                        }
                        out.reset();

                        int read;
                        while ((read = jarInputStream.read(buffer, 0, buffer.length)) >= 0) {
                            out.write(buffer, 0, read);
                        }

                        resources.put(entry.getName(), new Resource(resource, out.toByteArray(), manifest, codeSource));
                    }
                }
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            resources.forEach((k, v) -> this.resources.computeIfAbsent(k, i -> new ArrayList<>()).add(v));
        });
    }

    public Class<?> registerBytecode(final String name, final byte[] bytes) {
        final Class<?> value = super.defineClass(name, bytes, 0, bytes.length);
        resolveClass(value);
        proxies.put(name, value);
        return value;
    }

    private void logUnexpectedDependency(final URL url, final String entry) {
        log.warn("{} shouldn't be provided outside the JVM itself, origin={}", entry, url);
    }

    private boolean isBlacklisted(final String name) {
        return "META-INF/services/org.xml.sax.driver".equals(name);
    }

    private boolean isBlacklistedFromParent(final String name) {
        return name != null && name.startsWith("TALEND-INF/");
    }

    public String[] getJvmMarkers() {
        return Stream.of(nameJvmPrefixes, fullPathJvmPrefixes).flatMap(Stream::of).toArray(String[]::new);
    }

    public void registerTransformer(final ClassFileTransformer transformer) {
        transformers.add(transformer);
    }

    public synchronized URLClassLoader createTemporaryCopy() {
        final ConfigurableClassLoader self = this;
        return temporaryCopy == null ? temporaryCopy = new ConfigurableClassLoader(id, creationUrls, getParent(),
                parentFilter, childFirstFilter, resources, fullPathJvmPrefixes, resourcesFilter) {

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
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            // for others be as it's in parent
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            if (name.endsWith("package-info")) {
                // need to handle package-info, because it loaded in a different way
                // ucp is empty, and we can't load the resource
                // not sure about the parameter resolve, didn't see that the parent does the resolve
                return loadClass(name, false);
            }

            // rethrow the problem otherwise
            throw e;
        }
    }

    @Override
    public Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
        if (name == null) { // some frameworks (hibernate to not cite it) do it
            throw new ClassNotFoundException();
        }

        final Class<?> aClass = proxies.get(name);
        if (aClass != null) {
            return aClass;
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

            // last chance using java defined classpath
            clazz = classLoaderFromClasspath.loadClass(name);
            if (clazz != null) {
                return clazz;
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
    public Enumeration<URL> getResources(final String name) throws IOException {
        final Enumeration<URL> selfResources = findResources(name);
        final Enumeration<URL> parentResources = getParent().getResources(name);
        if (!parentResources.hasMoreElements()) {
            return selfResources;
        }
        if (!selfResources.hasMoreElements()) {
            return new FilteringUrlEnum(parentResources, this::isInJvm);
        }
        return new ChainedEnumerations(
                asList(selfResources, new FilteringUrlEnum(parentResources, this::isInJvm)).iterator());
    }

    @Override
    public URL getResource(final String name) {
        URL resource = findResource(name);
        if (resource != null) {
            return resource;
        }
        if (!isBlacklistedFromParent(name)) {
            resource = getParent().getResource(name);
            if (resource != null && (isNestedDependencyResource(name) || isInJvm(resource))) {
                return resource;
            }
        }
        return null;
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
        if (isBlacklisted(name)) {
            logUnexpectedDependency(resource, name);
            return null;
        }
        try {
            if (resource == null && !isBlacklistedFromParent(name)) {
                final URL url = getParent().getResource(name);
                return url != null ? getInputStream(url) : null;
            }
            if (resource == null) {
                return null;
            }
            return getInputStream(resource);
        } catch (final IOException e) {
            return null;
        }
    }

    private InputStream getInputStream(final URL resource) throws IOException {
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

    private boolean isNestedDependencyResource(final String name) {
        return name.startsWith(NESTED_MAVEN_REPOSITORY) || name.endsWith(".jar"); // TODO: improve coz not at all
                                                                                  // precise
    }

    private boolean isInJvm(final URL resource) {
        // Services and parent allowed resources that should always be found by top level classloader.
        // By default, META-INF/services/ is always allowed otherwise SPI won't work properly in nested environments.
        // Warning: selection shouldn't be too generic! Use very specific paths only like jndi.properties.
        if (resourcesFilter.test(resource.getFile())) {
            return true;
        }
        final Path path = toPath(resource);
        if (path == null) {
            return false;
        }
        if (nameJvmPrefixes.length > 0) {
            final String name = path.getFileName().toString();
            if (Stream.of(nameJvmPrefixes).anyMatch(it -> it.startsWith(name))) {
                return true;
            }
        }
        if (fullPathJvmPrefixes.length > 0) {
            final String fullPath = path.toAbsolutePath().toString();
            return Stream.of(fullPathJvmPrefixes).anyMatch(fullPath::startsWith);
        }
        return false;
    }

    public List<InputStream> findContainedResources(final String name) {
        try {
            return Stream.concat(ofNullable(super.findResources(name)).map(urls -> list(urls).stream().map(url -> {
                try {
                    return url.openStream();
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            })).orElseGet(Stream::empty),
                    ofNullable(resources.get(name))
                            .map(s -> s
                                    .stream()
                                    .map(it -> it.resource)
                                    .map(ByteArrayInputStream::new)
                                    .map(InputStream.class::cast))
                            .orElseGet(Stream::empty))
                    .collect(toList());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Opens a stream to a resource located in a nested JAR.
     *
     * @param nestedUrl The full "nested:" URL, e.g.
     * nested:/path/to/outer.jar/!path/inner.jar!/file.txt
     * @return InputStream to the resource (must be closed by caller)
     */
    public InputStream getNestedResource(final String nestedUrl) throws IOException {
        if (nestedUrl == null || !nestedUrl.startsWith("nested:")) {
            throw new IllegalArgumentException("Invalid nested URL: " + nestedUrl);
        }
        final String path = nestedUrl.substring("nested:".length());
        // Find the "/!" and "!/" separators
        final int firstBang = path.indexOf("/!");
        final int secondBang = path.indexOf("!/", firstBang + 2);
        if (firstBang < 0 || secondBang < 0) {
            throw new IllegalArgumentException("Malformed nested URL: " + nestedUrl);
        }
        final Path outerJarPath = Paths.get(path.substring(0, firstBang)); // before /!
        final String innerJarPath = path.substring(firstBang + 2, secondBang); // between /! and !/
        final String resourcePath = path.substring(secondBang + 2); // after the last !/
        // Open the outer JAR file
        try (JarFile outerJar = new JarFile(outerJarPath.toFile())) {
            JarEntry innerEntry = outerJar.getJarEntry(innerJarPath);
            if (innerEntry == null) {
                throw new FileNotFoundException("Inner JAR not found: " + innerJarPath);
            }
            // Copy the inner JAR to a temporary file
            final Path tempInnerJar = Files.createTempFile("nested-inner-", ".jar");
            try (InputStream innerStream = outerJar.getInputStream(innerEntry)) {
                Files.copy(innerStream, tempInnerJar, StandardCopyOption.REPLACE_EXISTING);

                JarFile innerJar = new JarFile(tempInnerJar.toFile());
                final JarEntry resourceEntry = innerJar.getJarEntry(resourcePath);
                if (resourceEntry == null) {
                    throw new FileNotFoundException("Resource not found: " + resourcePath);
                }

                // Return a stream that cleans up automatically when closed
                return new FilterInputStream(innerJar.getInputStream(resourceEntry)) {

                    @Override
                    public void close() throws IOException {
                        try {
                            super.close();
                        } finally {
                            Files.deleteIfExists(tempInnerJar);
                        }
                    }
                };
            } catch (IOException e) {
                Files.deleteIfExists(tempInnerJar);
                throw e;
            }
        }
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
                if (cacheableClasses.stream().anyMatch(s -> path.startsWith(s))) {
                    connection.setUseCaches(true);
                }
                // package
                final int i = name.lastIndexOf('.');
                if (i != -1) {
                    final String pckName = name.substring(0, i);
                    final Package pck = super.getPackage(pckName);
                    if (pck == null) {
                        if (!JarURLConnection.class.isInstance(connection)) {
                            doDefinePackage(null, null, pckName);
                        } else {
                            final JarURLConnection urlConnection = JarURLConnection.class.cast(connection);
                            doDefinePackage(urlConnection.getManifest(), urlConnection.getJarFileURL(), pckName);
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
                        : NO_CERTIFICATES;
                bytes = doTransform(resourceName, bytes);
                clazz = super.defineClass(name, bytes, 0, bytes.length, new CodeSource(url, certificates));
            } catch (final IOException e) {
                log.warn(e.getMessage(), e);
                return null;
            }
        } else if (!resources.isEmpty()) {
            final Collection<Resource> resources = this.resources.get(name.replace(".", "/") + ".class");
            if (resources != null && !resources.isEmpty()) {
                final Resource resource = resources.iterator().next();

                final int i = name.lastIndexOf('.');
                if (i != -1) {
                    doDefinePackage(resource.manifest, null, name.substring(0, i));
                }

                final byte[] bytes = doTransform(resourceName, resource.resource);
                clazz = defineClass(name, bytes, 0, bytes.length, resource.codeSource);
            }
        }
        if (postLoad(resolve, clazz)) {
            return clazz;
        }
        return null;
    }

    private byte[] doTransform(final String resourceName, final byte[] inBytes) {
        if (transformers.isEmpty()) {
            return inBytes;
        }
        byte[] bytes = inBytes;
        for (final ClassFileTransformer transformer : transformers) {
            try {
                bytes = transformer.transform(this, resourceName, null, null, bytes);
            } catch (final IllegalClassFormatException e) {
                log.error(e.getMessage() + ", will ignore the transformers", e);
                break;
            }
        }
        return bytes;
    }

    private void doDefinePackage(final Manifest manifest, final URL url, final String pckName) {
        IllegalArgumentException iae = null;
        for (int i = 0; i < 3; i++) {
            try {
                final Package pck = super.getPackage(pckName);
                if (pck == null) {
                    if (manifest == null) {
                        definePackage(pckName, null, null, null, null, null, null, null);
                    } else {
                        definePackage(pckName, manifest, url);
                    }
                }
                return;
            } catch (final IllegalArgumentException e) { // concurrent access on some JVM, quite unexpected
                iae = e;
            }
        }
        throw iae;
    }

    private URLClassLoader createClassLoaderFromClasspath() {
        return new java.net.URLClassLoader(Arrays
                .stream(System.getProperty("java.class.path", "").split(File.pathSeparator))
                .filter(cp -> !".".equals(cp))
                .map(File::new)
                .filter(File::exists)
                .map(f -> {
                    try {
                        return f.toURL();
                    } catch (MalformedURLException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toArray(URL[]::new));
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class Resource {

        private final String entry;

        private final byte[] resource;

        private final Manifest manifest;

        private final CodeSource codeSource;
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class Handler extends URLStreamHandler {

        private final Resource resource;

        @Override
        protected URLConnection openConnection(final URL url) {
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
        public void connect() {
            // no-op
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(resource.resource);
        }
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class FilteringUrlEnum implements Enumeration<URL> {

        private final Enumeration<URL> delegate;

        private final Predicate<URL> filter;

        private URL next;

        @Override
        public boolean hasMoreElements() {
            while (delegate.hasMoreElements()) {
                next = delegate.nextElement();
                if (filter.test(next)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public URL nextElement() {
            return next;
        }
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class ChainedEnumerations implements Enumeration<URL> {

        private final Iterator<Enumeration<URL>> enumerations;

        private Enumeration<URL> current;

        @Override
        public boolean hasMoreElements() {
            if (current == null || !current.hasMoreElements()) {
                if (!enumerations.hasNext()) {
                    return false;
                }
                current = enumerations.next();
            }
            return current.hasMoreElements();
        }

        @Override
        public URL nextElement() {
            return current.nextElement();
        }
    }
}
