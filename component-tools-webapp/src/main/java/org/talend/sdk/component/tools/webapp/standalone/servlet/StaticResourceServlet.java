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
package org.talend.sdk.component.tools.webapp.standalone.servlet;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.config.BinaryDataStrategy;

import org.talend.sdk.component.path.PathFactory;
import org.talend.sdk.component.tools.webapp.standalone.Route;

// just a plain servlet + jsonb, enables to use graalvm to native-compile it easily or
// integrate it in an existing server
//
// usage: (start server with) system properties
// -Dtalend.component.server.static.repository=/repo -Dtalend.component.server.static.routes=routes.json
public class StaticResourceServlet extends HttpServlet {

    private Collection<Route> routes;

    private Path repository;

    @Override
    public void init() throws ServletException {
        super.init();
        try (final Jsonb jsonb = JsonbBuilder
                .create(new JsonbConfig()
                        .withFormatting(true)
                        .withBinaryDataStrategy(BinaryDataStrategy.BASE_64)
                        .setProperty("johnzon.cdi.activated", false))) {
            routes = jsonb.fromJson(findRoutes(), new ParameterizedType() {

                @Override
                public Type[] getActualTypeArguments() {
                    return new Type[] { Route.class };
                }

                @Override
                public Type getRawType() {
                    return List.class;
                }

                @Override
                public Type getOwnerType() {
                    return null;
                }
            });
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
        repository = PathFactory
                .get(requireNonNull(System.getProperty("talend.component.server.static.repository"),
                        "missing -Dtalend.component.server.static.repository value"));
    }

    private InputStream findRoutes() {
        return ofNullable(System.getProperty("talend.component.server.static.routes")).map(PathFactory::get).map(it -> {
            try {
                return Files.newInputStream(it);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }).orElseGet(() -> Thread.currentThread().getContextClassLoader().getResourceAsStream("routes.json"));
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) {
        final String path = req.getRequestURI().substring(req.getContextPath().length());
        final Supplier<Map<String, String>> queryString = lazyParseQueryString(req.getQueryString());
        routes
                .stream()
                .filter(it -> matches(path, queryString, it, req))
                .findFirst()
                .map(route -> handle(route, req, resp))
                .orElseGet(() -> onMissingRoute(resp));
    }

    private boolean matches(final String path, final Supplier<Map<String, String>> queryString, final Route route,
            final HttpServletRequest req) {
        return Objects.equals(route.getPath(), path) && ofNullable(route.getRequestHeaders())
                .map(h -> h.isEmpty()
                        || h.entrySet().stream().allMatch(e -> Objects.equals(req.getHeader(e.getKey()), e.getValue())))
                .orElse(true)
                && ofNullable(route.getQueries())
                        .map(q -> q.isEmpty() || q
                                .entrySet()
                                .stream()
                                .allMatch(e -> Objects.equals(queryString.get().get(e.getKey()), e.getValue())))
                        .orElse(true);
    }

    private Route onMissingRoute(final HttpServletResponse resp) {
        try {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Not supported");
            return null;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private Route handle(final Route route, final HttpServletRequest req, final HttpServletResponse resp) {
        resp.setStatus(route.getStatus());
        try {
            final Path file = repository.resolve(route.getId());
            final FileTime lastModifiedTime = Files.getLastModifiedTime(file);
            final long lastModifiedMs = lastModifiedTime.toMillis();
            if (lastModifiedMs > 0) {
                long ifModifiedSince;
                try {
                    ifModifiedSince = req.getDateHeader("If-Modified-Since");
                } catch (final IllegalArgumentException iae) {
                    ifModifiedSince = -1;
                }
                if (ifModifiedSince < (lastModifiedMs / 1000 * 1000)) {
                    if (!resp.containsHeader("Last-Modified")) {
                        resp.setDateHeader("Last-Modified", lastModifiedMs);
                    }
                    doSend(route, resp, file);
                } else {
                    resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                }
            } else {
                doSend(route, resp, file);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return route;
    }

    private void doSend(final Route route, final HttpServletResponse resp, final Path file) throws IOException {
        ofNullable(route.getResponseHeaders()).ifPresent(h -> h.forEach(resp::addHeader));
        Files.copy(file, resp.getOutputStream());
    }

    private Supplier<Map<String, String>> lazyParseQueryString(final String queryString) {
        return new Supplier<Map<String, String>>() {

            private Map<String, String> parsed;

            @Override
            public Map<String, String> get() {
                if (parsed == null) {
                    parsed = ofNullable(queryString).map(q -> Stream.of(q.split("&")).map(it -> {
                        final int eq = it.indexOf('=');
                        if (eq >= 0) {
                            return new AbstractMap.SimpleEntry<>(it.substring(0, eq), it.substring(eq + 1));
                        }
                        return new AbstractMap.SimpleEntry<>(it, "");
                    }).collect(toMap(Map.Entry::getKey, Map.Entry::getValue))).orElseGet(Collections::emptyMap);
                }
                return parsed;
            }
        };
    }
}
