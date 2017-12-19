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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.talend.sdk.component.api.service.cache.LocalCache;
import org.talend.sdk.component.api.service.http.Codec;
import org.talend.sdk.component.api.service.http.Configurer;
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
import org.talend.sdk.component.runtime.manager.reflect.Copiable;
import org.talend.sdk.component.runtime.reflect.Defaults;
import org.talend.sdk.component.runtime.serialization.SerializableService;

import lombok.AllArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
public class HttpClientFactoryImpl implements HttpClientFactory, Serializable {

    private static final String PATH_RESERVED_CHARACTERS = "=@/:!$&\'(),;~";

    private static final String QUERY_RESERVED_CHARACTERS = "?/,";

    private final String plugin;

    public static <T> Collection<String> createErrors(final Class<T> api) {
        final Collection<String> errors = new ArrayList<>();
        final Collection<Method> methods =
                Stream.of(api.getMethods()).filter(m -> m.getDeclaringClass() == api && !m.isDefault()).collect(
                        toList());

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
            return (codec == null || codec.encoder() == Encoder.class) && Stream
                    .of(m.getParameters())
                    .filter(p -> Stream.of(Path.class, Query.class, Header.class).noneMatch(p::isAnnotationPresent))
                    .anyMatch(p -> p.getType() != String.class);
        }).map(m -> m + " defines a payload without an adapted coder").collect(toList()));
        errors.addAll(methods.stream().filter(m -> {
            final Codec codec = m.getAnnotation(Codec.class);
            return (codec == null || codec.decoder() == Decoder.class) && String.class != m.getReturnType();
        }).map(m -> m + " defines a payload without an adapted coder").collect(toList()));
        return errors;
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
        final HttpHandler handler = new HttpHandler();
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
        return new SerializableService(plugin, LocalCache.class.getName());
    }

    @ToString
    private static class HttpHandler implements InvocationHandler, Serializable {

        private String base;

        private volatile ConcurrentMap<Method, Function<Object[], Object>> invokers;

        @Override
        public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
            if (Copiable.class == method.getDeclaringClass()) {
                final HttpHandler httpHandler = new HttpHandler();
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
                    }
                }
            }

            return invokers.computeIfAbsent(method, m -> ofNullable(m.getAnnotation(Request.class)).map(request -> {
                final Codec codec = ofNullable(m.getAnnotation(Codec.class))
                        .orElseGet(() -> m.getDeclaringClass().getAnnotation(Codec.class));
                final UseConfigurer configurer = ofNullable(m.getAnnotation(UseConfigurer.class))
                        .orElseGet(() -> m.getDeclaringClass().getAnnotation(UseConfigurer.class));
                final Configurer configurerInstance;
                try {
                    configurerInstance = configurer == null ? c -> {
                    } : configurer.value().getConstructor().newInstance();
                } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalArgumentException(e.getTargetException());
                }
                final Encoder encoder = createEncoder(codec);
                final Decoder decoder = createDecoder(codec);

                // precompute the execution (kind of compile phase)
                String path = request.path();
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }

                final Collection<BiFunction<String, Object[], String>> pathReplacements = new ArrayList<>();
                final Collection<Function<Object[], String>> queryBuilder = new ArrayList<>();
                final Collection<BiConsumer<Object[], HttpURLConnection>> headerProvider = new ArrayList<>();
                Function<Object[], byte[]> payloadProvider = null;
                final Parameter[] parameters = m.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    final int index = i;
                    if (parameters[i].isAnnotationPresent(Path.class)) {
                        final Path annotation = parameters[i].getAnnotation(Path.class);
                        final String placeholder = "{" + annotation.value() + '}';
                        final boolean encode = annotation.encode();
                        pathReplacements.add((p, params) -> {
                            String out = p;
                            int start;
                            do {
                                start = out.indexOf(placeholder);
                                if (start >= 0) {
                                    String value = String.valueOf(args[index]);
                                    if (encode) {
                                        value = pathEncode(value);
                                    }
                                    out = out.substring(0, start) + value + out.substring(start + placeholder.length());
                                }
                            } while (start >= 0);
                            return out;
                        });
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
                    } else { // payload
                        if (payloadProvider != null) {
                            throw new IllegalArgumentException(m + " has two payload parameters");
                        } else {
                            payloadProvider = params -> encoder.encode(params[index]);
                        }
                    }
                }

                final String httpMethod = request.method();
                final String startingPath = path;
                final Function<Object[], byte[]> payloadProviderRef = payloadProvider;
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
                        configurerInstance.configure(urlConnection);

                        if (payloadProviderRef != null) {
                            urlConnection.setDoOutput(true);
                            try (final BufferedOutputStream outputStream =
                                    new BufferedOutputStream(urlConnection.getOutputStream())) {
                                outputStream.write(payloadProviderRef.apply(params));
                                outputStream.flush();
                            }
                        }

                        final int responseCode = urlConnection.getResponseCode();
                        byte[] error = null;
                        byte[] response = null;
                        try {
                            response = slurp(urlConnection.getInputStream());
                            if (!isResponse) {
                                return decoder.decode(response, responseType);
                            }
                            return new ResponseImpl(responseCode, decoder, responseType,
                                    urlConnection.getHeaderFields(), response, null);
                        } catch (final IOException e) {
                            error = slurp(urlConnection.getErrorStream());
                            final Response<Object> errorResponse = new ResponseImpl(responseCode, decoder, responseType,
                                    urlConnection.getHeaderFields(), null, error);

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
            }).orElseGet(() -> params -> delegate(method, args))).apply(args);
        }

        private Object delegate(final Method method, final Object[] args) {
            try {
                return method.invoke(this, args);
            } catch (final InvocationTargetException ite) {
                final Throwable targetException = ite.getTargetException();
                if (RuntimeException.class.isInstance(targetException)) {
                    throw RuntimeException.class.cast(targetException);
                }
                throw new IllegalStateException(targetException);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private Encoder createEncoder(final Codec codec) {
            if (codec != null && codec.encoder() != Encoder.class) {
                try {
                    return codec.encoder().getConstructor().newInstance();
                } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalArgumentException(e.getTargetException());
                }
            }
            return value -> value == null ? new byte[0] : String.valueOf(value).getBytes(StandardCharsets.UTF_8);
        }

        private Decoder createDecoder(final Codec codec) {
            if (codec != null && codec.decoder() != Decoder.class) {
                try {
                    return codec.decoder().getConstructor().newInstance();
                } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e) {
                    throw new IllegalArgumentException(e);
                } catch (final InvocationTargetException e) {
                    throw new IllegalArgumentException(e.getTargetException());
                }
            }
            return (value, expectedType) -> new String(value);
        }
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
}
