/**
 * Copyright (C) 2006-2023 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.http.internal.impl;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.PropertyOrderStrategy;

import org.talend.sdk.component.junit.http.api.Request;
import org.talend.sdk.component.junit.http.api.Response;
import org.talend.sdk.component.junit.http.api.ResponseLocator;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class DefaultResponseLocator implements ResponseLocator, AutoCloseable {

    public static final String PREFIX = "talend/testing/http/";

    private static final ParameterizedTypeImpl MODEL_TYPE = new ParameterizedTypeImpl(Collection.class, Model.class);

    private final Jsonb jsonb;

    private String prefix;

    private String test;

    private final Collection<DefaultResponseLocator.Model> capturingBuffer = new ArrayList<>();

    public DefaultResponseLocator(final String prefix, final String test) {
        this.prefix = prefix;
        this.test = test;
        this.jsonb = JsonbBuilder
                .create(new JsonbConfig()
                        .withPropertyOrderStrategy(PropertyOrderStrategy.LEXICOGRAPHICAL)
                        .withFormatting(true)
                        .setProperty("johnzon.cdi.activated", false));
    }

    @Override
    public Optional<Response> findMatching(final Request request, final Predicate<String> headerFilter) {
        final String pref = ofNullable(prefix).orElse(PREFIX);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Optional<Response> response = doFind(request, pref, loader, headerFilter, true);
        if (response.isPresent()) {
            return response;
        }
        return doFind(request, pref, loader, headerFilter, false);
    }

    protected Optional<Response> doFind(final Request request, final String pref, final ClassLoader loader,
            final Predicate<String> headerFilter, final boolean exactMatching) {
        return Stream
                .of(pref + test + ".json", pref + stripQuery(request.uri()) + ".json")
                .map(loader::getResource)
                .filter(Objects::nonNull)
                .flatMap(url -> {
                    final Collection<Model> models;
                    try (final InputStream stream = url.openStream()) {
                        models = Collection.class.cast(jsonb.fromJson(stream, MODEL_TYPE));
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                    return models
                            .stream()
                            .filter(m -> m.request != null && matches(request, m.request, exactMatching, headerFilter));
                })
                .findFirst()
                .map(model -> new ResponseImpl(model.response.headers, model.response.status, getPayload(model)));
    }

    protected byte[] getPayload(final Model model) {
        if (model.response.payload == null) {
            return null;
        }
        return model.response.payload.getBytes(StandardCharsets.UTF_8);
    }

    private String stripQuery(final String uri) {
        try {
            return new URL(uri).getPath();
        } catch (final MalformedURLException e) {
            // no-op
        }
        return uri;
    }

    protected boolean matches(final Request request, final RequestModel model, final boolean exact,
            final Predicate<String> headerFilter) {
        final String method = ofNullable(model.method).orElse("GET");
        final String requestUri = request.uri();
        boolean headLineMatches = requestUri.equals(model.uri) && request.method().equalsIgnoreCase(method);
        final String payload = request.payload();
        final boolean headersMatch = doesHeadersMatch(request, model, headerFilter);
        if (headLineMatches && headersMatch && (model.payload == null || model.payload.equals(payload))) {
            return true;
        } else if (exact) {
            return false;
        }

        if (log.isDebugEnabled()) {
            log.debug("Matching test: {} for {}", request, model);
        }

        if (!headLineMatches && requestUri.contains("?")) { // strip the query
            headLineMatches = requestUri.substring(0, requestUri.indexOf('?')).equals(model.uri)
                    && request.method().equalsIgnoreCase(method);
        }

        return headLineMatches && headersMatch && (model.payload == null
                || (payload != null && (payload.matches(model.payload) || payload.equals(model.payload))));
    }

    protected boolean doesHeadersMatch(final Request request, final RequestModel model,
            final Predicate<String> headerFilter) {
        final Map<String, String> notcased = request.headers()
                .entrySet()
                .stream()
                .collect(toMap(e -> e.getKey().toLowerCase(), Entry::getValue));
        return model.headers == null || model.headers
                .entrySet()
                .stream()
                .filter(h -> !headerFilter.test(h.getKey()))
                .allMatch(h -> h.getValue().equals(notcased.get(h.getKey().toLowerCase())));
    }

    @Override
    public void close() {
        try {
            jsonb.close();
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void flush(final String baseCapture) {
        final File output = new File(baseCapture,
                ofNullable(test).map(t -> prefix + t + ".json").orElseGet(() -> prefix + ".json"));
        output.getParentFile().mkdirs();
        try (final OutputStream outputStream = new FileOutputStream(output)) {
            jsonb.toJson(capturingBuffer, outputStream);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } finally {
            capturingBuffer.clear();
        }
    }

    public void addModel(final Model model) {
        getCapturingBuffer().add(model);
    }

    @Data
    public static class Model {

        private RequestModel request;

        private ResponseModel response;
    }

    @Data
    public static class RequestModel {

        private String uri;

        private String method;

        private String payload;

        private Map<String, String> headers;
    }

    @Data
    public static class ResponseModel {

        private int status;

        private Map<String, String> headers;

        private String payload;
    }

    private static final class ParameterizedTypeImpl implements ParameterizedType {

        private final Type rawType;

        private final Type[] types;

        private ParameterizedTypeImpl(final Type raw, final Type... types) {
            this.rawType = raw;
            this.types = types;
        }

        @Override
        public Type[] getActualTypeArguments() {
            return types.clone();
        }

        @Override
        public Type getOwnerType() {
            return null;
        }

        @Override
        public Type getRawType() {
            return rawType;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(types) ^ (rawType == null ? 0 : rawType.hashCode());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            } else if (obj instanceof ParameterizedType) {
                final ParameterizedType that = (ParameterizedType) obj;
                final Type thatRawType = that.getRawType();
                return that.getOwnerType() == null
                        && (rawType == null ? thatRawType == null : rawType.equals(thatRawType))
                        && Arrays.equals(types, that.getActualTypeArguments());
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(((Class<?>) rawType).getSimpleName());
            final Type[] actualTypes = getActualTypeArguments();
            if (actualTypes.length > 0) {
                buffer.append("<");
                int length = actualTypes.length;
                for (int i = 0; i < length; i++) {
                    buffer.append(actualTypes[i].toString());
                    if (i != actualTypes.length - 1) {
                        buffer.append(",");
                    }
                }

                buffer.append(">");
            }
            return buffer.toString();
        }
    }
}
