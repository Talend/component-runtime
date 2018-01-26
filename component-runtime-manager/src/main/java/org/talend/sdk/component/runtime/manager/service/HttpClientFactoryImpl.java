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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.of;
import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.transform.stream.StreamSource;

import org.talend.sdk.component.api.service.http.Codec;
import org.talend.sdk.component.api.service.http.Configurer;
import org.talend.sdk.component.api.service.http.ConfigurerOption;
import org.talend.sdk.component.api.service.http.ContentType;
import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.Encoder;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.HttpClientFactory;
import org.talend.sdk.component.api.service.http.HttpException;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.http.Response;
import org.talend.sdk.component.api.service.http.UseConfigurer;
import org.talend.sdk.component.runtime.manager.reflect.Constructors;
import org.talend.sdk.component.runtime.manager.reflect.Copiable;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.reflect.Defaults;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
public class HttpClientFactoryImpl implements HttpClientFactory, Serializable {

    private static final String PATH_RESERVED_CHARACTERS = "=@/:!$&\'(),;~";

    private static final String QUERY_RESERVED_CHARACTERS = "?/,";

    private final String plugin;

    private final ReflectionService reflections;

    private final Map<Class<?>, Object> services;

    public static <T> Collection<String> createErrors(final Class<T> api) {
        final Collection<String> errors = new ArrayList<>();
        final Collection<Method> methods =
                of(api.getMethods()).filter(m -> m.getDeclaringClass() == api && !m.isDefault()).collect(toList());

        if (!HttpClient.class.isAssignableFrom(api)) {
            errors.add(api.getCanonicalName() + " should extends HttpClient");
        }
        errors.addAll(methods
                .stream()
                .filter(m -> !m.isAnnotationPresent(Request.class))
                .map(m -> "No @Request on " + m)
                .collect(toList()));
        errors.addAll(methods.stream().filter(m -> {
            final Codec codec = m.getAnnotation(Codec.class);
            return (codec == null || codec.encoder().length == 0) && of(m.getParameters())
                    .filter(p -> of(Path.class, Query.class, Header.class).noneMatch(p::isAnnotationPresent))
                    .anyMatch(p -> isNotSupportedByDefaultCodec(p.getParameterizedType()));
        }).map(m -> m + " defines a request payload without an adapted coder").collect(toList()));
        errors.addAll(methods.stream().filter(m -> {
            final Codec codec = m.getAnnotation(Codec.class);
            return (codec == null || codec.decoder().length == 0)
                    && isNotSupportedByDefaultCodec(m.getGenericReturnType());
        }).map(m -> m + " defines a response payload without an adapted coder").collect(toList()));
        return errors;
    }

    private static boolean isNotSupportedByDefaultCodec(final Type type) {
        final Class<?> cType = toClassType(type);
        return cType != null && cType != String.class && cType != Void.class
                && !cType.isAnnotationPresent(XmlRootElement.class);
    }

    private static Class<?> toClassType(final Type type) {
        Class<?> cType = null;
        if (Class.class.isInstance(type)) {
            cType = Class.class.cast(type);
        } else if (ParameterizedType.class.isInstance(type)) {
            final ParameterizedType pt = ParameterizedType.class.cast(type);
            if (pt.getRawType() == Response.class && pt.getActualTypeArguments().length == 1
                    && Class.class.isInstance(pt.getActualTypeArguments()[0])) {
                cType = Class.class.cast(pt.getActualTypeArguments()[0]);
            }
        }

        return cType;
    }

    private static byte[] slurp(final InputStream responseStream) {
        final byte[] buffer = new byte[8192];
        final ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream(buffer.length);
        try (final InputStream inputStream = responseStream) {
            int count;
            while ((count = inputStream.read(buffer)) >= 0) {
                responseBuffer.write(buffer, 0, count);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return responseBuffer.toByteArray();
    }

    private static String queryEncode(final String value) {
        return componentEncode(QUERY_RESERVED_CHARACTERS, value);
    }

    private static String pathEncode(final String value) {
        String result = componentEncode(PATH_RESERVED_CHARACTERS, value);
        // URLEncoder will encode '+' to %2B but will turn ' ' into '+'
        // We need to retain '+' and encode ' ' as %20
        if (result.indexOf('+') != -1) {
            result = result.replace("+", "%20");
        }
        if (result.contains("%2B")) {
            result = result.replace("%2B", "+");
        }
        return result;
    }

    private static String componentEncode(final String reservedChars, final String value) {
        final StringBuilder buffer = new StringBuilder();
        final StringBuilder bufferToEncode = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            final char currentChar = value.charAt(i);
            if (reservedChars.indexOf(currentChar) != -1) {
                if (bufferToEncode.length() > 0) {
                    buffer.append(urlEncode(bufferToEncode.toString()));
                    bufferToEncode.setLength(0);
                }
                buffer.append(currentChar);
            } else {
                bufferToEncode.append(currentChar);
            }
        }
        if (bufferToEncode.length() > 0) {
            buffer.append(urlEncode(bufferToEncode.toString()));
        }
        return buffer.toString();
    }

    private static String urlEncode(final String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public <T> T create(final Class<T> api, final String base) {
        if (!api.isInterface()) {
            throw new IllegalArgumentException(api + " is not an interface");
        }
        validate(api);
        final HttpHandler handler = new HttpHandler(plugin, reflections, services);
        final T instance = api.cast(Proxy.newProxyInstance(api.getClassLoader(),
                new Class<?>[] { api, HttpClient.class, Serializable.class, Copiable.class }, handler));
        HttpClient.class.cast(instance).base(base);
        return instance;
    }

    private <T> void validate(final Class<T> api) {
        final Collection<String> errors = createErrors(api);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid Http Proxy specification:\n" + errors.stream().collect(joining("\n- ", "- ", "\n")));
        }
    }

    Object writeReplace() throws ObjectStreamException {
        return new SerializableService(plugin, HttpClientFactory.class.getName());
    }

    @ToString
    @RequiredArgsConstructor
    private static class HttpHandler implements InvocationHandler, Serializable {

        private static final Object[] EMPTY_ARRAY = new Object[0];

        private static final Configurer.ConfigurerConfiguration EMPTY_CONFIGURER_OPTIONS =
                new Configurer.ConfigurerConfiguration() {

                    @Override
                    public Object[] configuration() {
                        return EMPTY_ARRAY;
                    }

                    @Override
                    public <T> T get(final String name, final Class<T> type) {
                        return null;
                    }
                };

        private final String plugin;

        private final ReflectionService reflections;

        private final Map<Class<?>, Object> services;

        private String base;

        private volatile Map<Class<?>, JAXBContext> jaxbContexts = new HashMap<>();

        private volatile ConcurrentMap<Method, Function<Object[], Object>> invokers;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (Copiable.class == method.getDeclaringClass()) {
                final HttpHandler httpHandler = new HttpHandler(plugin, reflections, services);
                httpHandler.base = base;
                return Proxy.newProxyInstance(proxy.getClass().getClassLoader(), proxy.getClass().getInterfaces(),
                        httpHandler);
            }
            if (method.isDefault()) {
                final Class<?> declaringClass = method.getDeclaringClass();
                return Defaults
                        .of(declaringClass)
                        .unreflectSpecial(method, declaringClass)
                        .bindTo(proxy)
                        .invokeWithArguments(args);
            }

            final String methodName = method.getName();
            if (Object.class == method.getDeclaringClass()) {
                switch (methodName) {
                case "equals":
                    return args[0] != null && Proxy.isProxyClass(args[0].getClass())
                            && equals(Proxy.getInvocationHandler(args[0]));
                case "toString":
                    return "@Request " + base;
                default:
                    return delegate(method, args);
                }
            } else if (HttpClient.class == method.getDeclaringClass()) {
                switch (methodName) {
                case "base":
                    final String rawBase = String.valueOf(args[0]);
                    this.base = rawBase.endsWith("/") ? rawBase : (rawBase + '/');
                    return null;
                default:
                    throw new UnsupportedOperationException("HttpClient." + methodName);
                }
            }
            if (invokers == null) {
                synchronized (this) {
                    if (invokers == null) {
                        invokers = new ConcurrentHashMap<>();
                        jaxbContexts = new ConcurrentHashMap<>();
                    }
                }
            }
            return invokers.computeIfAbsent(method,
                    m -> createInvoker(m, args).orElseGet(() -> params -> delegate(method, args))).apply(args);
        }

        private Optional<Function<Object[], Object>> createInvoker(final Method m, final Object[] args) {
            return ofNullable(m.getAnnotation(Request.class)).map(request -> {

                final UseConfigurer configurer = ofNullable(m.getAnnotation(UseConfigurer.class))
                        .orElseGet(() -> m.getDeclaringClass().getAnnotation(UseConfigurer.class));
                final Configurer configurerInstance;
                try {
                    configurerInstance = configurer == null ? null : configurer.value().getConstructor().newInstance();
                } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                } catch (final InvocationTargetException e) {
                    throw toRuntimeException(e);
                }

                final Codec codec = ofNullable(m.getAnnotation(Codec.class))
                        .orElseGet(() -> m.getDeclaringClass().getAnnotation(Codec.class));

                // create codec instances
                Stream.concat(of(m.getGenericReturnType()),
                        of(m.getParameters()).filter(p -> of(Path.class, Query.class, Header.class)
                                .noneMatch(p::isAnnotationPresent)).map(Parameter::getParameterizedType))
                        .map(HttpClientFactoryImpl::toClassType)
                        .filter(Objects::nonNull)
                        .filter(cType -> cType.isAnnotationPresent(XmlRootElement.class) || cType.isAnnotationPresent(
                                XmlType.class))
                        .forEach(rootElemType -> {
                            jaxbContexts.computeIfAbsent(rootElemType, k -> {
                                try {
                                    return JAXBContext.newInstance(k);
                                } catch (final JAXBException e) {
                                    throw new IllegalStateException(e);
                                }
                            });
                        })
                ;

                final Map<String, Encoder> encoders = createEncoder(codec);
                final Map<String, Decoder> decoders = createDecoder(codec);

                // preCompute the execution (kind of compile phase)
                String path = request.path();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                final Collection<BiFunction<String, Object[], String>> pathReplacements = new ArrayList<>();
                final Collection<Function<Object[], String>> queryBuilder = new ArrayList<>();
                final Collection<BiConsumer<Object[], HttpURLConnection>> headerProvider = new ArrayList<>();
                final Map<String, Function<Object[], Object>> configurerOptions = new HashMap<>();
                BiFunction<HttpURLConnection, Object[], byte[]> payloadProvider = null;
                final Parameter[] parameters = m.getParameters();
                final CodecMatcher<Encoder> encoderMatcher = new CodecMatcher<>();

                for (int i = 0; i < parameters.length; i++) {
                    final int index = i;
                    if (parameters[i].isAnnotationPresent(Path.class)) {
                        final Path annotation = parameters[i].getAnnotation(Path.class);
                        pathReplacements.add((p, params) -> replacePlaceholder(p, '{' + annotation.value() + '}',
                                String.valueOf(args[index]), annotation.encode()));
                    } else if (parameters[i].isAnnotationPresent(Query.class)) {
                        final Query annotation = parameters[i].getAnnotation(Query.class);
                        final String queryName = annotation.value();
                        final boolean encode = annotation.encode();
                        queryBuilder.add(params -> ofNullable(params[index]).map(v -> {
                            String value = String.valueOf(v);
                            if (encode) {
                                value = queryEncode(value);
                            }
                            return queryName + '=' + value;
                        }).orElse(""));
                    } else if (parameters[i].isAnnotationPresent(Header.class)) {
                        final String headerName = parameters[i].getAnnotation(Header.class).value();
                        headerProvider
                                .add((params, connection) -> ofNullable(params[index]).map(String::valueOf).ifPresent(
                                        v -> connection.setRequestProperty(headerName, v)));
                    } else if (parameters[i].isAnnotationPresent(ConfigurerOption.class)) {
                        final String name = parameters[i].getAnnotation(ConfigurerOption.class).value();
                        configurerOptions.putIfAbsent(name, params -> params[index]);
                    } else { // payload
                        if (payloadProvider != null) {
                            throw new IllegalArgumentException(m + " has two payload parameters");
                        } else {
                            if (encoders.size() == 1) {
                                final Encoder encoder = encoders.values().iterator().next();
                                payloadProvider = (u, params) -> encoder.encode(params[index]);
                            } else {
                                payloadProvider = (connection, params) -> encoderMatcher
                                        .select(encoders, connection.getRequestProperty("content-type"))
                                        .encode(params[index]);
                            }
                        }
                    }
                }

                final String httpMethod = request.method();
                final String startingPath = path;
                final BiFunction<HttpURLConnection, Object[], byte[]> payloadProviderRef = payloadProvider;
                final boolean isResponse = m.getReturnType() == Response.class;
                final Type responseType =
                        isResponse ? ParameterizedType.class.cast(m.getGenericReturnType()).getActualTypeArguments()[0]
                                : m.getReturnType();
                return (Function<Object[], Object>) params -> {
                    String currentPath = startingPath;
                    for (final BiFunction<String, Object[], String> fn : pathReplacements) {
                        currentPath = fn.apply(currentPath, params);
                    }
                    if (!queryBuilder.isEmpty()) {
                        currentPath += "?" + queryBuilder.stream().map(b -> b.apply(params)).collect(joining("&"));
                    }
                    HttpURLConnection urlConnection = null;
                    try {
                        final URL url = new URL(base + currentPath);
                        urlConnection = HttpURLConnection.class.cast(url.openConnection());
                        urlConnection.setRequestMethod(httpMethod);
                        for (final BiConsumer<Object[], HttpURLConnection> provider : headerProvider) {
                            provider.accept(params, urlConnection);
                        }

                        if (configurerInstance != null) {
                            final Map<String, Object> options = configurerOptions.entrySet().stream().collect(
                                    toMap(Map.Entry::getKey, e -> e.getValue().apply(params)));
                            configurerInstance.configure(urlConnection,
                                    configurerOptions.isEmpty() ? EMPTY_CONFIGURER_OPTIONS
                                            : new Configurer.ConfigurerConfiguration() {

                                        @Override
                                        public Object[] configuration() {
                                            return options.values().toArray(new Object[options.size()]);
                                        }

                                        @Override
                                        public <T> T get(final String name, final Class<T> type) {
                                            return type.cast(options.get(name));
                                        }
                                    });
                        }

                        if (payloadProviderRef != null) {
                            urlConnection.setDoOutput(true);
                            try (final BufferedOutputStream outputStream =
                                    new BufferedOutputStream(urlConnection.getOutputStream())) {
                                outputStream.write(payloadProviderRef.apply(urlConnection, params));
                                outputStream.flush();
                            }
                        }

                        final int responseCode = urlConnection.getResponseCode();
                        final CodecMatcher<Decoder> decoderMatcher = new CodecMatcher<>();
                        final String contentType = urlConnection.getHeaderField("content-type");
                        final byte[] error;
                        final byte[] response;
                        try {
                            response = slurp(urlConnection.getInputStream());
                            if (!isResponse) {
                                return decoderMatcher.select(decoders, contentType).decode(response, responseType);
                            }
                            return new ResponseImpl(responseCode, decoderMatcher.select(decoders, contentType),
                                    responseType, headers(urlConnection), response, null);
                        } catch (final IOException e) {
                            error = ofNullable(urlConnection.getErrorStream())
                                    .map(HttpClientFactoryImpl::slurp)
                                    .orElseGet(() -> ofNullable(e.getMessage())
                                            .map(s -> s.getBytes(StandardCharsets.UTF_8))
                                            .orElse(null));
                            final Response<Object> errorResponse =
                                    new ResponseImpl(responseCode, decoderMatcher.select(decoders, contentType),
                                            responseType, headers(urlConnection), null, error);

                            if (isResponse) {
                                return errorResponse;
                            }

                            throw new HttpException(errorResponse);
                        }

                    } catch (final IOException e) {
                        if (urlConnection != null) { // it fails, release the resources, otherwise we want to be pooled
                            urlConnection.disconnect();
                        }
                        throw new IllegalStateException(e);
                    }
                };
            });
        }

        /**
         * @param original    : string with placeholders
         * @param placeholder the placeholder to be replaced
         * @param value       the replacement value
         * @param encode      true if the value need to be encoded
         * @return a new string with the placeholder replaced by value
         */
        private String replacePlaceholder(final String original, final String placeholder, final String value,
                final boolean encode) {
            String out = original;
            int start;
            do {
                start = out.indexOf(placeholder);
                if (start >= 0) {
                    String replacement = value;
                    if (encode) {
                        replacement = pathEncode(replacement);
                    }
                    out = out.substring(0, start) + replacement + out.substring(start + placeholder.length());
                }
            } while (start >= 0);
            return out;
        }

        private Map<String, List<String>> headers(final HttpURLConnection urlConnection) {
            return urlConnection.getHeaderFields().keySet().stream().filter(Objects::nonNull).collect(
                    toMap(identity(), urlConnection.getHeaderFields()::get, (k, v) -> {
                        throw new IllegalArgumentException("Ambiguous key for: '" + k + "'");
                    }, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
        }

        private Object delegate(final Method method, final Object[] args) {
            try {
                return method.invoke(this, args);
            } catch (final InvocationTargetException ite) {
                throw toRuntimeException(ite);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private Map<String, Encoder> createEncoder(final Codec codec) {
            if (codec == null || codec.decoder().length == 0) {

                return new HashMap<String, Encoder>() {{
                    //keep the put order
                    if (!jaxbContexts.isEmpty()) {
                        final JAXBEncoder jaxbEncoder = new JAXBEncoder(jaxbContexts);
                        put("*/xml", jaxbEncoder);
                        put("*/*+xml", jaxbEncoder);
                    }
                    put("*/*", value -> value == null ?
                            new byte[0] :
                            String.valueOf(value).getBytes(StandardCharsets.UTF_8));
                }};
            }

            return stream(codec.encoder())
                    .filter(Objects::nonNull)
                    .collect(toMap(encoder -> encoder.getAnnotation(ContentType.class) != null
                            ? encoder.getAnnotation(ContentType.class).value()
                            : "*/*", encoder -> {
                        try {
                            final Constructor<?> constructor = Constructors.findConstructor(encoder);
                            final Function<Map<String, String>, Object[]> paramFactory =
                                    reflections.parameterFactory(constructor, services);
                            return Encoder.class.cast(constructor.newInstance(paramFactory.apply(emptyMap())));
                        } catch (final InstantiationException | IllegalAccessException e) {
                            throw new IllegalArgumentException(e);
                        } catch (final InvocationTargetException e) {
                            throw toRuntimeException(e);
                        }
                    }, (k, v) -> {
                        throw new IllegalArgumentException("Ambiguous key for: '" + k + "'");
                    }, LinkedHashMap::new));

        }

        private Map<String, Decoder> createDecoder(final Codec codec) {
            if (codec == null || codec.decoder().length == 0) {
                return new HashMap<String, Decoder>() {{
                    //keep the put order
                    if (!jaxbContexts.isEmpty()) {
                        final JAXBDecoder jaxbDecoder = new JAXBDecoder(jaxbContexts);
                        put("*/xml", jaxbDecoder);
                        put("*/*+xml", jaxbDecoder);
                    }
                    put("*/*", (value, expectedType) -> new String(value));
                }};
            }

            return stream(codec.decoder())
                    .filter(Objects::nonNull)
                    .collect(toMap(decoder -> decoder.getAnnotation(ContentType.class) != null
                            ? decoder.getAnnotation(ContentType.class).value()
                            : "*/*", decoder -> {
                        try {
                            final Constructor<?> constructor = Constructors.findConstructor(decoder);
                            final Function<Map<String, String>, Object[]> paramFactory =
                                    reflections.parameterFactory(constructor, services);
                            return Decoder.class.cast(constructor.newInstance(paramFactory.apply(emptyMap())));
                        } catch (final InstantiationException | IllegalAccessException e) {
                            throw new IllegalArgumentException(e);
                        } catch (final InvocationTargetException e) {
                            throw toRuntimeException(e);
                        }
                    }, (k, v) -> {
                        throw new IllegalArgumentException("Ambiguous key for: '" + k + "'");
                    }, LinkedHashMap::new));
        }

        @AllArgsConstructor
        private static class ResponseImpl<T> implements Response<T> {

            private final int status;

            private final Decoder decoder;

            private final Type responseType;

            private Map<String, List<String>> headers;

            private byte[] responseBody;

            private byte[] error;

            @Override
            public int status() {
                return status;
            }

            @Override
            public Map<String, List<String>> headers() {
                return headers;
            }

            @Override
            public T body() {
                if (responseBody == null) {
                    return null;
                }

                return (T) decoder.decode(responseBody, responseType);
            }

            @Override
            public <E> E error(final Class<E> type) {
                if (error == null) {
                    return null;
                }
                if (String.class == type) {
                    return type.cast(new String(error));
                }

                return type.cast(decoder.decode(error, type));
            }
        }

        @AllArgsConstructor
        private static class JAXBDecoder implements Decoder {

            private final Map<Class<?>, JAXBContext> jaxbContexts;

            @Override
            public Object decode(final byte[] value, final Type expectedType) {
                try {
                    final Class key = Class.class.cast(expectedType);
                    return jaxbContexts.get(key).createUnmarshaller()
                            .unmarshal(new StreamSource(new ByteArrayInputStream(value)), key).getValue();
                } catch (final JAXBException e) {
                    throw new IllegalArgumentException(e);
                }

            }
        }

        @AllArgsConstructor
        private static class JAXBEncoder implements Encoder {

            private final Map<Class<?>, JAXBContext> jaxbContexts;

            @Override public byte[] encode(final Object value) {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    jaxbContexts.get(value.getClass()).createMarshaller().marshal(value, os);
                } catch (final JAXBException e) {
                    throw new IllegalArgumentException(e);
                }
                return os.toByteArray();
            }
        }
    }
}
