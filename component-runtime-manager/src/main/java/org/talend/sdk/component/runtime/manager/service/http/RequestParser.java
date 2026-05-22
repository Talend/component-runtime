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
package org.talend.sdk.component.runtime.manager.service.http;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.talend.sdk.component.runtime.base.lang.exception.InvocationExceptionWrapper.toRuntimeException;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;

import org.talend.sdk.component.api.service.http.Base;
import org.talend.sdk.component.api.service.http.Codec;
import org.talend.sdk.component.api.service.http.Configurer;
import org.talend.sdk.component.api.service.http.ConfigurerOption;
import org.talend.sdk.component.api.service.http.ContentType;
import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.Encoder;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.Headers;
import org.talend.sdk.component.api.service.http.HttpMethod;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.QueryFormat;
import org.talend.sdk.component.api.service.http.QueryParams;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.http.Response;
import org.talend.sdk.component.api.service.http.Url;
import org.talend.sdk.component.api.service.http.UseConfigurer;
import org.talend.sdk.component.runtime.manager.reflect.Constructors;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.service.MediaTypeComparator;
import org.talend.sdk.component.runtime.manager.service.http.codec.CodecMatcher;
import org.talend.sdk.component.runtime.manager.service.http.codec.JsonpDecoder;
import org.talend.sdk.component.runtime.manager.service.http.codec.JsonpEncoder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

public class RequestParser {

    private static final String PATH_RESERVED_CHARACTERS = "=@/:!$&\'(),;~";

    private static final String QUERY_RESERVED_CHARACTERS = "?/,";

    private final InstanceCreator instanceCreator;

    interface InstanceCreator {

        <T> T buildNew(Class<? extends T> realClass);
    }

    @AllArgsConstructor
    final static class ReflectionInstanceCreator implements InstanceCreator {

        private final ReflectionService reflections;

        private final Map<Class<?>, Object> services;

        @Override
        public <T> T buildNew(final Class<? extends T> realClass) {
            try {
                final Constructor<?> constructor = Constructors.findConstructor(realClass);
                final Function<Map<String, String>, Object[]> paramFactory =
                        reflections.parameterFactory(constructor, services, null);
                return (T) constructor.newInstance(paramFactory.apply(emptyMap()));
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            } catch (final InvocationTargetException e) {
                throw toRuntimeException(e);
            }
        }
    }

    private final Encoder jsonpEncoder;

    private final Decoder jsonpDecoder;

    private final JAXBManager jaxb = JAXB.ACTIVE ? new JAXBManager() : null;

    private volatile CodecMatcher<Encoder> codecMatcher = new CodecMatcher<>();

    public RequestParser(final ReflectionService reflections, final Jsonb jsonb, final Map<Class<?>, Object> services) {
        this(new ReflectionInstanceCreator(reflections, services), jsonb);
    }

    public RequestParser(final InstanceCreator instanceCreator, final Jsonb jsonb) {
        this.instanceCreator = instanceCreator;
        this.jsonpEncoder = new JsonpEncoder(jsonb);
        this.jsonpDecoder = new JsonpDecoder(jsonb);
    }

    /**
     * Parse method annotated with @{@link Request} and construct an {@link ExecutionContext}
     *
     * @param method method annotated with @Request
     * @return an http request execution context
     */
    public ExecutionContext parse(final Method method) {
        if (!method.isAnnotationPresent(Request.class)) {
            throw new IllegalStateException("Method '" + method.getName() + "' need to be annotated with @Request");
        }

        final Request request = method.getAnnotation(Request.class);
        Configurer configurerInstance = findConfigurerInstance(method);
        if (jaxb != null) {
            jaxb.initJaxbContext(method);
        }
        final Codec codec = ofNullable(method.getAnnotation(Codec.class))
                .orElseGet(() -> method.getDeclaringClass().getAnnotation(Codec.class));
        final Map<String, Encoder> encoders = createEncoder(codec);
        final Map<String, Decoder> decoders = createDecoder(codec);
        final PathProvider pathProvider = new PathProvider();
        final QueryParamsProvider queryParamsProvider = new QueryParamsProvider();
        final HeadersProvider headersProvider = new HeadersProvider();
        final Map<String, Function<Object[], Object>> configurerOptionsProvider = new HashMap<>();
        Integer httpMethod = null;
        Function<Object[], String> urlProvider = null;
        Function<Object[], String> baseProvider = null;
        BiFunction<String, Object[], Optional<byte[]>> payloadProvider = null;
        // preCompute the execution (kind of compile phase)
        final Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            final int index = i;
            if (parameters[i].isAnnotationPresent(HttpMethod.class)) {
                if (httpMethod != null) {
                    throw new IllegalStateException(method + "has two HttpMethod parameters");
                }
                httpMethod = index;
            } else if (parameters[i].isAnnotationPresent(Path.class)) {
                final Path path = parameters[i].getAnnotation(Path.class);
                pathProvider.pathParams.put(i, new Encodable(path.value(), path.encode()));
            } else if (parameters[i].isAnnotationPresent(Url.class)) {
                if (urlProvider != null) {
                    throw new IllegalStateException(method + "has two Url parameters");
                }
                urlProvider = params -> String.valueOf(params[index]);
            } else if (parameters[i].isAnnotationPresent(QueryParams.class)) {
                final QueryParams params = parameters[i].getAnnotation(QueryParams.class);
                queryParamsProvider.queries.put(i, new QueryEncodable("", params.encode(), params.format()));
            } else if (parameters[i].isAnnotationPresent(Query.class)) {
                final Query query = parameters[i].getAnnotation(Query.class);
                queryParamsProvider.queries.put(i, new QueryEncodable(query.value(), query.encode(), query.format()));
            } else if (parameters[i].isAnnotationPresent(Headers.class)) {
                headersProvider.headers.put(i, "");
            } else if (parameters[i].isAnnotationPresent(Header.class)) {
                headersProvider.headers.put(i, parameters[i].getAnnotation(Header.class).value());
            } else if (parameters[i].isAnnotationPresent(ConfigurerOption.class)) {
                configurerOptionsProvider
                        .putIfAbsent(parameters[i].getAnnotation(ConfigurerOption.class).value(),
                                params -> params[index]);
            } else if (parameters[i].isAnnotationPresent(Base.class)) {
                if (baseProvider != null) {
                    throw new IllegalStateException(method + "has two Base parameters");
                }
                baseProvider = params -> String.valueOf(params[index]);
            } else { // payload
                if (payloadProvider != null) {
                    throw new IllegalArgumentException(method + " has two payload parameters");
                }
                payloadProvider = buildPayloadProvider(encoders, i);
            }
        }

        final boolean isResponse = method.getReturnType() == Response.class;
        final Type responseType =
                isResponse ? ParameterizedType.class.cast(method.getGenericReturnType()).getActualTypeArguments()[0]
                        : method.getReturnType();
        final Integer httpMethodIndex = httpMethod;
        final Function<Object[], String> httpMethodProvider = params -> httpMethodIndex == null ? request.method()
                : ofNullable(params[httpMethodIndex]).map(String::valueOf).orElse(request.method());

        String pathTemplate = request.path();
        if (pathTemplate.startsWith("/")) {
            pathTemplate = pathTemplate.substring(1, pathTemplate.length());
        }
        if (pathTemplate.endsWith("/")) {
            pathTemplate = pathTemplate.substring(0, pathTemplate.length() - 1);
        }

        return new ExecutionContext(new HttpRequestCreator(httpMethodProvider, urlProvider, baseProvider, pathTemplate,
                pathProvider, queryParamsProvider, headersProvider, payloadProvider, configurerInstance,
                configurerOptionsProvider), responseType, isResponse, decoders);
    }

    private BiFunction<String, Object[], Optional<byte[]>> buildPayloadProvider(final Map<String, Encoder> encoders,
            final int index) {

        return (contentType, params) -> {
            final Object payload = params[index];
            if (payload == null) {
                return Optional.empty();
            }
            if (byte[].class.isInstance(payload)) {
                return Optional.of(byte[].class.cast(payload));
            }
            if (encoders.size() == 1) {
                return Optional.of(encoders.values().iterator().next().encode(payload));
            }
            return Optional.of(codecMatcher.select(encoders, contentType).encode(payload));
        };
    }

    private Configurer findConfigurerInstance(final Method m) {
        final UseConfigurer configurer = ofNullable(m.getAnnotation(UseConfigurer.class))
                .orElseGet(() -> m.getDeclaringClass().getAnnotation(UseConfigurer.class));
        try {
            return configurer == null ? null : configurer.value().getConstructor().newInstance();
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (final InvocationTargetException e) {
            throw toRuntimeException(e);
        }
    }

    static Class<?> toClassType(final Type type) {
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

    private static String queryEncode(final String value) {
        return componentEncode(QUERY_RESERVED_CHARACTERS, value);
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

    private Map<String, Encoder> createEncoder(final Codec codec) {
        final Map<String, Encoder> encoders = new HashMap<>();
        if (codec != null && codec.encoder().length != 0) {
            encoders
                    .putAll(stream(codec.encoder())
                            .filter(Objects::nonNull)
                            .collect(toMap((Class<? extends Encoder> encoder) -> Optional
                                    .ofNullable(encoder.getAnnotation(ContentType.class))
                                    .map(ContentType::value)
                                    .orElse("*/*"), this.instanceCreator::buildNew)));
        }

        // keep the put order
        if (jaxb != null && !jaxb.isEmpty()) {
            final Encoder jaxbEncoder = jaxb.newEncoder();
            encoders.putIfAbsent("*/xml", jaxbEncoder);
            encoders.putIfAbsent("*/*+xml", jaxbEncoder);
        }

        encoders.putIfAbsent("*/json", jsonpEncoder);
        encoders.putIfAbsent("*/*+json", jsonpEncoder);
        encoders
                .putIfAbsent("*/*",
                        value -> value == null ? new byte[0] : String.valueOf(value).getBytes(StandardCharsets.UTF_8));

        return sortMap(encoders);

    }

    private Map<String, Decoder> createDecoder(final Codec codec) {
        final Map<String, Decoder> decoders = new HashMap<>();
        if (codec != null && codec.decoder().length != 0) {
            decoders
                    .putAll(stream(codec.decoder())
                            .filter(Objects::nonNull)
                            .collect(toMap((Class<? extends Decoder> decoder) -> Optional
                                    .ofNullable(decoder.getAnnotation(ContentType.class))
                                    .map(ContentType::value)
                                    .orElse("*/*"), this.instanceCreator::buildNew)));
        }
        // add default decoders if not override by the user
        // keep the put order
        if (jaxb != null && !jaxb.isEmpty()) {
            final Decoder jaxbDecoder = jaxb.newDecoder();
            decoders.putIfAbsent("*/xml", jaxbDecoder);
            decoders.putIfAbsent("*/*+xml", jaxbDecoder);
        }
        decoders.putIfAbsent("*/json", jsonpDecoder);
        decoders.putIfAbsent("*/*+json", jsonpDecoder);
        decoders.putIfAbsent("*/*", (value, expectedType) -> new String(value));

        return sortMap(decoders);
    }

    private <T> Map<String, T> sortMap(final Map<String, T> entries) {
        final List<String> keys = new ArrayList<>(entries.keySet());
        keys.sort(new MediaTypeComparator(new ArrayList<>(keys)));
        return keys.stream().collect(toMap(k -> k.toLowerCase(ROOT), entries::get, (a, b) -> {
            throw new IllegalArgumentException(a + "/" + b);
        }, LinkedHashMap::new));
    }

    @Data
    private static class Encodable {

        private final String name;

        private final boolean encode;
    }

    @EqualsAndHashCode(callSuper = true)
    private static class QueryEncodable extends Encodable {

        private final QueryFormat format;

        private QueryEncodable(final String name, final boolean encode, final QueryFormat format) {
            super(name, encode);
            this.format = format;
        }
    }

    private static class QueryParamsProvider implements Function<Object[], Collection<String>> {

        private final Map<Integer, QueryEncodable> queries = new LinkedHashMap<>();

        @Override
        public Collection<String> apply(final Object[] args) {

            return queries.entrySet().stream().flatMap(entry -> {
                if (entry.getValue().getName().isEmpty()) {
                    final Map<String, ?> queryParams =
                            args[entry.getKey()] == null ? emptyMap() : Map.class.cast(args[entry.getKey()]);
                    if (entry.getValue().isEncode()) {
                        return queryParams
                                .entrySet()
                                .stream()
                                .filter(q -> q.getValue() != null)
                                .flatMap(it -> mapValues(entry.getValue(), it.getKey(), it.getValue()));
                    }
                    return queryParams.entrySet().stream();
                }
                return ofNullable(args[entry.getKey()])
                        .map(v -> mapValues(entry.getValue(), entry.getValue().getName(), v))
                        .orElse(null);
            }).filter(Objects::nonNull).map(kv -> kv.getKey() + "=" + kv.getValue()).collect(toList());
        }

        private Stream<AbstractMap.SimpleEntry<String, String>> mapValues(final QueryEncodable config, final String key,
                final Object v) {
            if (Collection.class.isInstance(v)) {
                final Stream<String> collection = ((Collection<?>) v)
                        .stream()
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .map(q -> config.isEncode() ? queryEncode(q) : q);
                switch (config.format) {
                    case MULTI:
                        return collection.map(q -> new AbstractMap.SimpleEntry<>(key, q));
                    case CSV:
                        return of(new AbstractMap.SimpleEntry<>(key, String.join(",", collection.collect(toList()))));
                    default:
                        throw new IllegalArgumentException("Unsupported formatting: " + config);
                }
            }

            String value = String.valueOf(v);
            if (config.isEncode()) {
                value = queryEncode(value);
            }
            return of(new AbstractMap.SimpleEntry<>(key, value));
        }
    }

    private static class HeadersProvider implements Function<Object[], Map<String, String>> {

        private final Map<Integer, String> headers = new LinkedHashMap<>();

        @Override
        public Map<String, String> apply(final Object[] args) {

            return headers.entrySet().stream().flatMap(entry -> {
                if (entry.getValue().isEmpty()) {
                    return args[entry.getKey()] == null ? empty()
                            : ((Map<String, String>) args[entry.getKey()]).entrySet().stream();
                }
                return ofNullable(args[entry.getKey()])
                        .map(v -> of(new AbstractMap.SimpleEntry<>(entry.getValue(), String.valueOf(v))))
                        .orElse(null);
            })
                    .filter(Objects::nonNull)
                    .filter(e -> e.getValue() != null) // ignore null values
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
                        throw new IllegalArgumentException("conflictings keys: " + a + '/' + b);
                    }, LinkedHashMap::new));
        }
    }

    @RequiredArgsConstructor
    private static class PathProvider implements BiFunction<String, Object[], String> {

        private final Map<Integer, Encodable> pathParams = new LinkedHashMap<>();

        /**
         * @param original : string with placeholders
         * @param placeholder the placeholder to be replaced
         * @param value the replacement value
         * @param encode true if the value need to be encoded
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

        @Override
        public String apply(final String pathTemplate, final Object[] args) {
            String path = pathTemplate;
            if (path == null || path.isEmpty()) {
                return path;
            }
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            for (final Map.Entry<Integer, Encodable> param : pathParams.entrySet()) {
                path = replacePlaceholder(path, '{' + param.getValue().name + '}', String.valueOf(args[param.getKey()]),
                        param.getValue().encode);
            }
            return path;
        }
    }
}
