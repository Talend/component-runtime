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
package org.talend.sdk.component.starter.server.front;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.rrd4j.core.FetchData;
import org.talend.sdk.component.starter.server.model.ErrorMessage;
import org.talend.sdk.component.starter.server.service.rrd.RRDConfig;
import org.talend.sdk.component.starter.server.service.rrd.RRDStorage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApplicationScoped
@Path("statistics")
public class RRDEndpoint {

    @Inject
    private RRDStorage rrd;

    @Inject
    private RRDConfig config;

    private Semaphore concurrentCalls;

    @PostConstruct
    private void init() {
        concurrentCalls = new Semaphore(config.getAcceptedConcurrentCalls());
    }

    @GET
    @Path("image")
    @Produces("image/png")
    public CompletionStage<InputStream> getPng(@QueryParam("from") @DefaultValue("-1") final long from,
            @QueryParam("to") @DefaultValue("-1") final long to,
            @QueryParam("width") @DefaultValue("1440") final int width,
            @QueryParam("height") @DefaultValue("900") final int height) {
        if (!concurrentCalls.tryAcquire()) {
            return failAcquire();
        }
        try {
            return rrd.render(from, to, width, height).handle(this::handle);
        } catch (final IllegalStateException ise) {
            throw onError(ise);
        }
    }

    @GET
    @Path("json")
    @Produces(MediaType.APPLICATION_JSON)
    public CompletionStage<Points> getPoints(@QueryParam("from") @DefaultValue("-1") final long from,
            @QueryParam("to") @DefaultValue("-1") final long to,
            @QueryParam("skipNan") @DefaultValue("true") final boolean skipNan) {
        if (!concurrentCalls.tryAcquire()) {
            return failAcquire();
        }
        try {
            return rrd
                    .fetch(from, to)
                    .handle(this::handle)
                    .thenApply(data -> new Points(Stream
                            .of(data.getDsNames())
                            .collect(toMap(this::getMetricName, ds -> mapPoints(skipNan, data, ds)))));
        } catch (final IllegalStateException ise) {
            throw onError(ise);
        }
    }

    private Collection<Point> mapPoints(final boolean skipNan, final FetchData data, final String ds) {
        final double[] values = data.getValues(ds);
        final long[] timestamps = data.getTimestamps();
        return IntStream
                .range(0, timestamps.length)
                .filter(idx -> !skipNan || !Double.isNaN(values[idx]))
                .mapToObj(idx -> new Point(timestamps[idx], Double.isNaN(values[idx]) ? 0 : values[idx]))
                .collect(toList());
    }

    private String getMetricName(final String it) {
        if (it.endsWith("!")) {
            return it.substring(0, it.length() - 1);
        }
        return it;
    }

    private <T> T handle(final T r, final Throwable t) {
        concurrentCalls.release();
        if (t != null) {
            throw onError(t);
        }
        return r;
    }

    private WebApplicationException onError(final Throwable ise) {
        return new WebApplicationException(Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorMessage(ise.getMessage()))
                .type(APPLICATION_JSON_TYPE)
                .build());
    }

    private <T> CompletionStage<T> failAcquire() {
        final CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new WebApplicationException(Response.Status.PRECONDITION_FAILED));
        return future;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Point {

        private long timestamp;

        private double value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Points {

        private Map<String, Collection<Point>> points;
    }
}
