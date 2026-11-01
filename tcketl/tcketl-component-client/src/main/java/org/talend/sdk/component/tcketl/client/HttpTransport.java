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
package org.talend.sdk.component.tcketl.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLException;

import org.talend.sdk.component.tcketl.domain.Preconditions;
import org.talend.sdk.component.tcketl.domain.server.ServerEndpoint;
import org.talend.sdk.component.tcketl.domain.server.ServerException;
import org.talend.sdk.component.tcketl.domain.server.ServerProtocolException;
import org.talend.sdk.component.tcketl.domain.server.ServerTimeouts;
import org.talend.sdk.component.tcketl.domain.server.ServerUnavailableException;

/**
 * The only place where bytes go over the network. It builds the URL below the configured base path, applies the
 * timeouts, inflates gzip answers (the JDK client does not) and reports a call that gets no answer as a
 * {@link ServerUnavailableException}. An HTTP error status is not an exception here: the caller decides which statuses
 * it interprets, {@link Response#toException()} turns the others into a {@link ServerException}.
 */
final class HttpTransport {

    static final String JSON = "application/json";

    static final String BINARY = "application/octet-stream";

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private static final int HTTP_OK_FIRST = 200;

    private static final int HTTP_OK_LAST = 299;

    private final HttpClient client;

    private final ServerEndpoint endpoint;

    private final ServerTimeouts timeouts;

    private final String base;

    HttpTransport(final ServerEndpoint endpoint, final ServerTimeouts timeouts) {
        this.endpoint = Preconditions.requireNonNull(endpoint, "endpoint");
        this.timeouts = Preconditions.requireNonNull(timeouts, "timeouts");
        this.client = HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(timeouts.connect())
                .build();
        final String root = endpoint.baseUri().toASCIIString();
        this.base = root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
    }

    /** Calls {@code GET <base>/<segments>?<query>}, whatever the status of the answer. */
    Response get(final String accept, final Query query, final String... segments) {
        final HttpRequest request = HttpRequest
                .newBuilder(uri(query, segments))
                .timeout(timeouts.request())
                .header("Accept", accept)
                .header("Accept-Encoding", "gzip")
                .GET()
                .build();
        return send(request);
    }

    private Response send(final HttpRequest request) {
        try {
            final HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return new Response(response.statusCode(), body(response),
                    response.headers().firstValue("Content-Type").orElse(""));
        } catch (final IOException e) {
            throw new ServerUnavailableException("Cannot reach " + endpoint + ": " + reason(e), e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerUnavailableException("Interrupted while calling " + endpoint, e);
        }
    }

    /** Calls {@code POST <base>/<segments>?<query>} with a JSON body, whatever the status of the answer. */
    Response post(final Query query, final String json, final String... segments) {
        final HttpRequest request = HttpRequest
                .newBuilder(uri(query, segments))
                .timeout(timeouts.action())
                .header("Accept", JSON)
                .header("Content-Type", JSON)
                .header("Accept-Encoding", "gzip")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        return send(request);
    }

    /** Same as {@link #get} but an error status is raised. */
    Response getOrThrow(final String accept, final Query query, final String... segments) {
        final Response response = get(accept, query, segments);
        if (!response.isSuccess()) {
            throw response.toException();
        }
        return response;
    }

    private URI uri(final Query query, final String... segments) {
        final StringBuilder uri = new StringBuilder(base);
        for (final String segment : segments) {
            uri.append('/').append(Query.encode(segment));
        }
        if (!query.isEmpty()) {
            uri.append('?').append(query);
        }
        return URI.create(uri.toString());
    }

    private static byte[] body(final HttpResponse<byte[]> response) {
        final byte[] raw = response.body();
        final boolean gzip =
                response.headers().firstValue("Content-Encoding").filter("gzip"::equalsIgnoreCase).isPresent();
        if (!gzip || raw.length == 0) {
            return raw;
        }
        try (final GZIPInputStream inflater = new GZIPInputStream(new ByteArrayInputStream(raw))) {
            return inflater.readAllBytes();
        } catch (final IOException e) {
            throw new ServerProtocolException(response.statusCode(),
                    "Cannot inflate the gzip answer: " + e.getMessage(),
                    e);
        }
    }

    private String reason(final IOException error) {
        if (error instanceof HttpConnectTimeoutException) {
            return "no connection within " + timeouts.connect().toMillis() + " ms";
        }
        if (error instanceof HttpTimeoutException) {
            return "no answer within " + timeouts.request().toMillis() + " ms";
        }
        if (hasCause(error, UnknownHostException.class) || hasCause(error, UnresolvedAddressException.class)) {
            return "unknown host " + endpoint.host();
        }
        if (hasCause(error, SSLException.class)) {
            return "TLS failure (" + error.getMessage() + ")";
        }
        final String message = error.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return hasCause(error, ConnectException.class) ? "connection refused" : error.getClass().getSimpleName();
    }

    private static boolean hasCause(final Throwable error, final Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    /** What the server answered: the status, the inflated body and the {@code Content-Type} header (or empty). */
    record Response(int status, byte[] body, String contentType) {

        boolean isSuccess() {
            return status >= HTTP_OK_FIRST && status <= HTTP_OK_LAST;
        }

        /** The failure this answer stands for: the server error payload when there is one, else a generic error. */
        ServerException toException() {
            return Wire
                    .error(body)
                    .map(payload -> new ServerException(status, payload.code(), payload.description()))
                    .orElseGet(() -> new ServerException(status, "UNEXPECTED", snippet(body)));
        }

        /** Content type without its parameters, e.g. {@code image/svg+xml}. */
        String mediaType() {
            final int end = contentType.indexOf(';');
            return (end < 0 ? contentType : contentType.substring(0, end)).trim();
        }

        private static String snippet(final byte[] body) {
            final String text = new String(body, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
            return text.length() > 200 ? text.substring(0, 200) + "..." : text;
        }
    }

    /** Query string under construction: names and values are percent-encoded, repeated names are kept. */
    static final class Query {

        private final List<String> parameters = new ArrayList<>();

        Query add(final String name, final String value) {
            parameters.add(encode(name) + '=' + encode(value));
            return this;
        }

        boolean isEmpty() {
            return parameters.isEmpty();
        }

        @Override
        public String toString() {
            return String.join("&", parameters);
        }

        /** Percent-encodes every byte but the unreserved ones, a space becoming {@code %20} (never {@code +}). */
        static String encode(final String value) {
            final StringBuilder encoded = new StringBuilder(value.length());
            for (final byte current : value.getBytes(StandardCharsets.UTF_8)) {
                final int code = current & 0xFF;
                if (code >= 'A' && code <= 'Z' || code >= 'a' && code <= 'z' || code >= '0' && code <= '9'
                        || code == '-'
                        || code == '.' || code == '_' || code == '~') {
                    encoded.append((char) code);
                } else {
                    encoded.append('%').append(HEX[code >> 4]).append(HEX[code & 0x0F]);
                }
            }
            return encoded.toString();
        }
    }
}
