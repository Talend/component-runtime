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
package org.talend.sdk.component.server.front;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.servlet.ServletController;
import org.apache.cxf.transport.servlet.servicelist.ServiceListGeneratorServlet;
import org.talend.sdk.component.server.api.BulkReadResource;
import org.talend.sdk.component.server.front.cxf.CxfExtractor;
import org.talend.sdk.component.server.front.memory.InMemoryRequest;
import org.talend.sdk.component.server.front.memory.InMemoryResponse;
import org.talend.sdk.component.server.front.memory.MemoryInputStream;
import org.talend.sdk.component.server.front.memory.SimpleServletConfig;
import org.talend.sdk.component.server.front.model.BulkRequests;
import org.talend.sdk.component.server.front.model.BulkResponses;
import org.talend.sdk.component.server.front.model.ErrorDictionary;
import org.talend.sdk.component.server.front.model.error.ErrorPayload;
import org.talend.sdk.component.server.service.qualifier.ComponentServer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class BulkReadResourceImpl implements BulkReadResource {

    private static final CompletableFuture[] EMPTY_PROMISES = new CompletableFuture[0];

    @Inject
    private CxfExtractor cxf;

    @Inject
    private Bus bus;

    @Inject
    @Context
    private ServletContext servletContext;

    @Inject
    @Context
    private HttpServletRequest httpServletRequest;

    @Inject
    @Context
    private UriInfo uriInfo;

    @Inject
    @Context
    private HttpServletRequest request;

    @Inject
    @ComponentServer
    private Jsonb defaultMapper;

    private ServletController controller;

    private final String appPrefix = "/api/v1";

    private final Collection<String> blacklisted =
            Stream.of(appPrefix + "/component/icon/", appPrefix + "/component/dependency/").collect(toSet());

    private final BulkResponses.Result forbiddenInBulkModeResponse =
            new BulkResponses.Result(Response.Status.FORBIDDEN.getStatusCode(), emptyMap(),
                    "{\"code\":\"UNAUTHORIZED\",\"description\":\"Forbidden endpoint in bulk mode.\"}"
                            .getBytes(StandardCharsets.UTF_8),
                    "{\"code\":\"UNAUTHORIZED\",\"description\":\"Forbidden endpoint in bulk mode.\"}");

    private final BulkResponses.Result forbiddenResponse =
            new BulkResponses.Result(Response.Status.FORBIDDEN.getStatusCode(), emptyMap(),
                    "{\"code\":\"UNAUTHORIZED\",\"description\":\"Secured endpoint, ensure to pass the right token.\"}"
                            .getBytes(StandardCharsets.UTF_8),
                    "{\"code\":\"UNAUTHORIZED\",\"description\":\"Secured endpoint, ensure to pass the right token.\"}");

    private final BulkResponses.Result invalidResponse =
            new BulkResponses.Result(Response.Status.BAD_REQUEST.getStatusCode(), emptyMap(),
                    "{\"code\":\"UNEXPECTED\",\"description\":\"unknownEndpoint.\"}".getBytes(StandardCharsets.UTF_8),
                    "{\"code\":\"UNEXPECTED\",\"description\":\"unknownEndpoint.\"}");

    @PostConstruct
    private void init() {
        final DestinationRegistry registry = cxf.getRegistry();
        controller = new ServletController(registry,
                new SimpleServletConfig(servletContext, "Talend Component Kit Bulk Transport"),
                new ServiceListGeneratorServlet(registry, bus));
    }

    @Override
    public CompletionStage<BulkResponses> bulk(final BulkRequests requests) {
        final Collection<CompletableFuture<BulkResponses.Result>> responses =
                ofNullable(requests.getRequests()).map(Collection::stream).orElseGet(Stream::empty).map(request -> {
                    if (isBlacklisted(request)) {
                        return completedFuture(forbiddenInBulkModeResponse);
                    }
                    if (request.getPath() == null || !request.getPath().startsWith(appPrefix)
                            || request.getPath().contains("?")) {
                        return completedFuture(invalidResponse);
                    }
                    return doExecute(request, uriInfo);
                }).collect(toList());
        return CompletableFuture
                .allOf(responses.toArray(EMPTY_PROMISES))
                .handle((ignored, error) -> new BulkResponses(responses.stream().map(it -> {
                    try {
                        return it.get();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    } catch (final ExecutionException e) {
                        throw new WebApplicationException(Response
                                .serverError()
                                .entity(new ErrorPayload(ErrorDictionary.UNEXPECTED, e.getMessage()))
                                .build());
                    }
                }).collect(toList())));
    }

    private boolean isBlacklisted(final BulkRequests.Request request) {
        return blacklisted.stream().anyMatch(it -> request.getPath() == null || request.getPath().startsWith(it));
    }

    private CompletableFuture<BulkResponses.Result> doExecute(final BulkRequests.Request inputRequest,
            final UriInfo info) {
        final Map<String, List<String>> headers =
                ofNullable(inputRequest.getHeaders()).orElseGet(Collections::emptyMap);
        final String path = ofNullable(inputRequest.getPath()).map(it -> it.substring(appPrefix.length())).orElse("/");

        // theorically we should encode these params but should be ok this way for now - due to the param we can accept
        final String queryString = ofNullable(inputRequest.getQueryParameters())
                .map(Map::entrySet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .flatMap(it -> ofNullable(it.getValue())
                        .map(Collection::stream)
                        .orElseGet(Stream::empty)
                        .map(value -> it.getKey() + '=' + value))
                .collect(joining("&"));

        final int port = info.getBaseUri().getPort();
        final Principal userPrincipal = request.getUserPrincipal(); // this is ap proxy so ready it early
        final InMemoryRequest request = new InMemoryRequest(ofNullable(inputRequest.getVerb()).orElse(HttpMethod.GET),
                headers, path, appPrefix + path, appPrefix, queryString, port < 0 ? 8080 : port, servletContext,
                new MemoryInputStream(ofNullable(inputRequest.getPayload())
                        .map(it -> it.getBytes(StandardCharsets.UTF_8))
                        .map(ByteArrayInputStream::new)
                        .map(InputStream.class::cast)
                        .orElse(null)),
                () -> userPrincipal, controller);
        final BulkResponses.Result result = new BulkResponses.Result();
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final CompletableFuture<BulkResponses.Result> promise = new CompletableFuture<>();
        final InMemoryResponse response = new InMemoryResponse(() -> true, () -> {
            result.setResponse(outputStream.toByteArray());
            result.setResponseString(new String(outputStream.toByteArray()));
            promise.complete(result);
        }, bytes -> {
            try {
                outputStream.write(bytes);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }, (status, responseHeaders) -> {
            result.setStatus(status);
            result.setHeaders(headers);
            return "";
        });
        request.setResponse(response);
        try {
            controller.invoke(request, response);
        } catch (final ServletException e) {
            result.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            result
                    .setResponse(defaultMapper
                            .toJson(new ErrorPayload(ErrorDictionary.UNEXPECTED, e.getMessage()))
                            .getBytes(StandardCharsets.UTF_8));
            promise.complete(result);
            throw new IllegalStateException(e);
        }
        return promise;
    }
}
