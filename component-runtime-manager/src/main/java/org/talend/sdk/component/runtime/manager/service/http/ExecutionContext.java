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
package org.talend.sdk.component.runtime.manager.service.http;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.talend.sdk.component.api.service.http.Configurer;
import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.HttpException;
import org.talend.sdk.component.api.service.http.Response;
import org.talend.sdk.component.runtime.manager.service.http.codec.CodecMatcher;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionContext implements BiFunction<String, Object[], Object> {

    private final HttpRequestCreator requestCreator;

    private final Type responseType;

    private final boolean isResponse;

    private final Map<String, Decoder> decoders;

    public Object apply(final String base, final Object[] params) {
        HttpURLConnection urlConnection = null;
        try {
            final HttpRequest request = requestCreator.apply(base, params);
            String queryParams =
                    request.getQueryParams().entrySet().stream().map(kv -> kv.getKey() + "=" + kv.getValue()).collect(
                            joining("&"));
            final URL url = new URL(request.getUrl() + (queryParams.isEmpty() ? "" : "?" + queryParams));
            urlConnection = HttpURLConnection.class.cast(url.openConnection());
            urlConnection.setRequestMethod(request.getMethodType());
            request.getHeaders().forEach(urlConnection::setRequestProperty);
            if (request.getConfigurer() != null) {
                request.getConfigurer().configure(new DefaultConnection(urlConnection),
                        request.getConfigurationOptions());
            }

            if (request.getBody().isPresent()) {
                urlConnection.setDoOutput(true);
                try (final BufferedOutputStream outputStream =
                        new BufferedOutputStream(urlConnection.getOutputStream())) {
                    outputStream.write(request.getBody().get());
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
                if (!isResponse()) {
                    return byte[].class == getResponseType() ? response
                            : decoderMatcher.select(getDecoders(), contentType).decode(response, getResponseType());
                }
                return new ResponseImpl(responseCode,
                        byte[].class == getResponseType() ? PassthroughDecoder.INSTANCE
                                : decoderMatcher.select(getDecoders(), contentType),
                        getResponseType(), headers(urlConnection), response, null);
            } catch (final IOException e) {
                error = ofNullable(urlConnection.getErrorStream()).map(ExecutionContext::slurp).orElseGet(
                        () -> ofNullable(e.getMessage()).map(s -> s.getBytes(StandardCharsets.UTF_8)).orElse(null));
                final Response<Object> errorResponse = new ResponseImpl(responseCode,
                        byte[].class == getResponseType() ? PassthroughDecoder.INSTANCE
                                : decoderMatcher.select(getDecoders(), contentType),
                        getResponseType(), headers(urlConnection), null, error);

                if (isResponse()) {
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

    private Map<String, List<String>> headers(final HttpURLConnection urlConnection) {
        return urlConnection.getHeaderFields().keySet().stream().filter(Objects::nonNull).collect(
                toMap(e -> e, urlConnection.getHeaderFields()::get, (k, v) -> {
                    throw new IllegalArgumentException("Ambiguous key for: '" + k + "'");
                }, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
    }

    @AllArgsConstructor
    private static class DefaultConnection implements Configurer.Connection {

        private final HttpURLConnection urlConnection;

        @Override
        public Configurer.Connection withHeader(final String name, final String value) {
            urlConnection.addRequestProperty(name, value);
            return this;
        }

        @Override
        public Configurer.Connection withReadTimeout(final int timeout) {
            urlConnection.setReadTimeout(timeout);
            return this;
        }

        @Override
        public Configurer.Connection withConnectionTimeout(final int timeout) {
            urlConnection.setConnectTimeout(timeout);
            return this;
        }
    }

    private static class PassthroughDecoder implements Decoder {

        private static final Decoder INSTANCE = new PassthroughDecoder();

        @Override
        public Object decode(final byte[] value, final Type expectedType) {
            return value;
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

            return byte[].class == responseType ? (T) responseBody : (T) decoder.decode(responseBody, responseType);
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
