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
package org.talend.sdk.component.form.internal.spring;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.WebException;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

public class SpringRestClient implements Client {

    private final AsyncRestTemplate delegate;

    private final String base;

    private final ParameterizedTypeReference<Map<String, Object>> mapType =
            new ParameterizedTypeReference<Map<String, Object>>() {
            };

    public SpringRestClient(final String base) {
        this.delegate = new AsyncRestTemplate();
        this.base = base;
    }

    // poor man way but we'll enhance it on demand
    private <T> CompletableFuture<T> toCompletionStage(final ListenableFuture<ResponseEntity<T>> future) {
        final CompletableFuture<T> result = new CompletableFuture<>();
        future.addCallback(new ListenableFutureCallback<ResponseEntity<T>>() {

            @Override
            public void onFailure(final Throwable throwable) {
                result.completeExceptionally(throwable);
            }

            @Override
            public void onSuccess(final ResponseEntity<T> t) {
                result.complete(t.getBody());
            }
        });
        return result;
    }

    @Override
    public CompletableFuture<Map<String, Object>> action(final String family, final String type, final String action,
            final Map<String, Object> params) {
        try {
            return toCompletionStage(
                    delegate
                            .exchange(
                                    UriComponentsBuilder
                                            .fromHttpUrl(base)
                                            .path("action/execute")
                                            .queryParam("family", family)
                                            .queryParam("type", type)
                                            .queryParam("action", action)
                                            .build()
                                            .toUriString(),
                                    HttpMethod.POST,
                                    new HttpEntity<>(
                                            params.entrySet().stream().collect(
                                                    toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()))),
                                            json()),

                                    mapType));
        } catch (final HttpServerErrorException hsee) {
            throw toException(hsee);
        } catch (final RestClientException rce) {
            throw toException(rce);
        }
    }

    @Override
    public CompletableFuture<ComponentIndices> index(final String language) {
        try {
            return toCompletionStage(delegate.exchange(UriComponentsBuilder
                    .fromHttpUrl(base)
                    .path("component/index")
                    .queryParam("language", language)
                    .build()
                    .toUriString(), HttpMethod.GET, new HttpEntity<>(json()), ComponentIndices.class));
        } catch (final HttpServerErrorException hsee) {
            throw toException(hsee);
        } catch (final RestClientException rce) {
            throw toException(rce);
        }
    }

    @Override
    public CompletableFuture<ComponentDetailList> details(final String language, final String identifier,
            final String... identifiers) {
        try {
            final HttpHeaders headers = json();
            return toCompletionStage(
                    delegate
                            .exchange(
                                    UriComponentsBuilder
                                            .fromHttpUrl(base)
                                            .path("component/details")
                                            .queryParam("language", language)
                                            .queryParam("identifiers",
                                                    Stream
                                                            .concat(Stream.of(identifier),
                                                                    identifiers == null || identifiers.length == 0
                                                                            ? Stream.empty()
                                                                            : Stream.of(identifiers))
                                                            .toArray(Object[]::new))
                                            .build()
                                            .toUriString(),
                                    HttpMethod.GET, new HttpEntity<>(headers), ComponentDetailList.class));
        } catch (final HttpServerErrorException hsee) {
            throw toException(hsee);
        } catch (final RestClientException rce) {
            throw toException(rce);
        }
    }

    @Override
    public void close() {
        // no-op
    }

    private HttpHeaders json() {
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        return httpHeaders;
    }

    private WebException toException(final RestClientException rce) {
        return new WebException(rce, -1, singletonMap("error", rce.getMessage()));
    }

    private WebException toException(final HttpServerErrorException hsee) {
        try {
            return new WebException(hsee, hsee.getRawStatusCode(),
                    new HttpMessageConverterExtractor<Map<String, Object>>(mapType.getType(),
                            delegate.getMessageConverters()).extractData(new ClientHttpResponse() {

                                @Override
                                public HttpStatus getStatusCode() {
                                    return HttpStatus.OK;
                                }

                                @Override
                                public int getRawStatusCode() {
                                    return 200;
                                }

                                @Override
                                public String getStatusText() {
                                    return "";
                                }

                                @Override
                                public void close() {
                                    // no-op
                                }

                                @Override
                                public InputStream getBody() {
                                    return new ByteArrayInputStream(hsee.getResponseBodyAsByteArray());
                                }

                                @Override
                                public HttpHeaders getHeaders() {
                                    final HttpHeaders json = json();
                                    json.add(HttpHeaders.CONTENT_TYPE, "application/json");
                                    return json;
                                }
                            }));
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
