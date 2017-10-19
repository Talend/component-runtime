// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.form.internal.spring;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpMessageConverterExtractor;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.talend.components.form.api.Client;
import org.talend.components.form.api.WebException;
import org.talend.components.server.front.model.ComponentDetailList;
import org.talend.components.server.front.model.ComponentIndices;

public class SpringRestClient implements Client {

    private final RestTemplate delegate;

    private final String base;

    private final ParameterizedTypeReference<Map<String, Object>> mapType = new ParameterizedTypeReference<Map<String, Object>>() {
    };

    public SpringRestClient(final String base) {
        this.delegate = new RestTemplate();
        this.base = base;
    }

    @Override
    public Map<String, Object> action(final String family, final String type, final String action,
            final Map<String, Object> params) {
        try {
            return delegate.exchange(
                    UriComponentsBuilder.fromHttpUrl(base).path("action/execute").queryParam("family", family)
                            .queryParam("type", type).queryParam("action", action).build().toUriString(),
                    HttpMethod.POST,
                    new HttpEntity<>(
                            params.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()))),
                            json()),

                    mapType).getBody();
        } catch (final HttpServerErrorException hsee) {
            throw toException(hsee);
        } catch (final RestClientException rce) {
            throw toException(rce);
        }
    }

    @Override
    public ComponentIndices index(final String language) {
        try {
            return delegate
                    .exchange(UriComponentsBuilder.fromHttpUrl(base).path("component/index").queryParam("language", language)
                            .build().toUriString(), HttpMethod.GET, new HttpEntity<>(json()), ComponentIndices.class)
                    .getBody();
        } catch (final HttpServerErrorException hsee) {
            throw toException(hsee);
        } catch (final RestClientException rce) {
            throw toException(rce);
        }
    }

    @Override
    public ComponentDetailList details(final String language, final String identifier, final String... identifiers) {
        try {
            final HttpHeaders headers = json();
            return delegate.exchange(
                    UriComponentsBuilder.fromHttpUrl(base).path("component/details").queryParam("language", language)
                            .queryParam("identifiers",
                                    Stream.concat(Stream.of(identifier),
                                            identifiers == null || identifiers.length == 0 ? Stream.empty()
                                                    : Stream.of(identifiers))
                                            .toArray(Object[]::new))
                            .build().toUriString(),
                    HttpMethod.GET, new HttpEntity<>(headers), ComponentDetailList.class).getBody();
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
                    new HttpMessageConverterExtractor<Map<String, Object>>(mapType.getType(), delegate.getMessageConverters())
                            .extractData(new ClientHttpResponse() {

                                @Override
                                public HttpStatus getStatusCode() throws IOException {
                                    return HttpStatus.OK;
                                }

                                @Override
                                public int getRawStatusCode() throws IOException {
                                    return 200;
                                }

                                @Override
                                public String getStatusText() throws IOException {
                                    return "";
                                }

                                @Override
                                public void close() {
                                    // no-op
                                }

                                @Override
                                public InputStream getBody() throws IOException {
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
