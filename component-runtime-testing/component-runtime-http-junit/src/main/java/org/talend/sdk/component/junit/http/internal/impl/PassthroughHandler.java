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
package org.talend.sdk.component.junit.http.internal.impl;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.junit.http.internal.impl.Handlers.BASE;
import static org.talend.sdk.component.junit.http.internal.impl.Handlers.closeOnFlush;
import static org.talend.sdk.component.junit.http.internal.impl.Handlers.sendError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLEngine;

import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.Attribute;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class PassthroughHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    protected final HttpApiHandler api;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        if (HttpMethod.CONNECT.name().equalsIgnoreCase(request.method().name())) {
            final FullHttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
            HttpUtil.setKeepAlive(response, true);
            HttpUtil.setContentLength(response, 0);
            if (api.getSslContext() != null) {
                final SSLEngine sslEngine = api.getSslContext().createSSLEngine();
                sslEngine.setUseClientMode(false);
                ctx.channel().pipeline().addFirst("ssl", new SslHandler(sslEngine, true));

                final String uri = request.uri();
                final String[] parts = uri.split(":");
                ctx
                        .channel()
                        .attr(BASE)
                        .set("https://" + parts[0]
                                + (parts.length > 1 && !"443".equals(parts[1]) ? ":" + parts[1] : ""));
            }
            ctx.writeAndFlush(response);
            return;
        }
        final FullHttpRequest req = request.copy(); // copy to use in a separated thread
        api.getExecutor().execute(() -> doHttpRequest(req, ctx));
    }

    private void doHttpRequest(final FullHttpRequest request, final ChannelHandlerContext ctx) {
        try {
            final Attribute<String> baseAttr = ctx.channel().attr(Handlers.BASE);
            final String requestUri =
                    (baseAttr == null || baseAttr.get() == null ? "" : baseAttr.get()) + request.uri();

            // do the remote request with all the incoming data and save it
            // note: this request must be synchronous for now
            final Response resp;
            final Map<String, String> otherHeaders = new HashMap<>();
            try {
                final URL url = new URL(requestUri);
                final HttpURLConnection connection = HttpURLConnection.class.cast(url.openConnection(Proxy.NO_PROXY));
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(20000);
                if (HttpsURLConnection.class.isInstance(connection) && api.getSslContext() != null) {
                    final HttpsURLConnection httpsURLConnection = HttpsURLConnection.class.cast(connection);
                    httpsURLConnection.setHostnameVerifier((h, s) -> true);
                    httpsURLConnection.setSSLSocketFactory(api.getSslContext().getSocketFactory());
                }
                request.headers().entries().forEach(e -> connection.setRequestProperty(e.getKey(), e.getValue()));
                if (request.method() != null) {
                    final String requestMethod = request.method().name().toString();
                    connection.setRequestMethod(requestMethod);

                    if (!"HEAD".equalsIgnoreCase(requestMethod) && request.content().readableBytes() > 0) {
                        connection.setDoOutput(true);
                        request.content().readBytes(connection.getOutputStream(), request.content().readableBytes());
                    }
                }

                final int responseCode = connection.getResponseCode();
                final int defaultLength =
                        ofNullable(connection.getHeaderField("content-length")).map(Integer::parseInt).orElse(8192);
                resp = new ResponseImpl(collectHeaders(connection, api.getHeaderFilter().negate()), responseCode,
                        responseCode <= 399 ? slurp(connection.getInputStream(), defaultLength)
                                : slurp(connection.getErrorStream(), defaultLength));

                otherHeaders.putAll(collectHeaders(connection, api.getHeaderFilter()));

                beforeResponse(requestUri, request, resp,
                        new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER) {

                            {
                                connection
                                        .getHeaderFields()
                                        .entrySet()
                                        .stream()
                                        .filter(it -> it.getKey() != null && it.getValue() != null)
                                        .forEach(e -> put(e.getKey(), e.getValue()));
                            }
                        });
            } catch (final Exception e) {
                log.error(e.getMessage(), e);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            final ByteBuf bytes = ofNullable(resp.payload()).map(Unpooled::copiedBuffer).orElse(Unpooled.EMPTY_BUFFER);
            final HttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(resp.status()), bytes);
            HttpUtil.setContentLength(response, bytes.array().length);

            Stream
                    .of(resp.headers(), otherHeaders)
                    .filter(Objects::nonNull)
                    .forEach(h -> h.forEach((k, v) -> response.headers().set(k, v)));
            ctx.writeAndFlush(response);

        } finally {
            request.release();
        }
    }

    private TreeMap<String, String> collectHeaders(final HttpURLConnection connection, final Predicate<String> filter) {
        return connection
                .getHeaderFields()
                .entrySet()
                .stream()
                .filter(e -> e.getKey() != null)
                .filter(h -> filter.test(h.getKey()))
                .collect(toMap(Map.Entry::getKey, e -> e.getValue().stream().collect(Collectors.joining(",")),
                        (a, b) -> a, () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
    }

    protected void beforeResponse(final String requestUri, final FullHttpRequest request, final Response resp,
            final Map<String, List<String>> headerFields) {
        // no-op
    }

    private byte[] slurp(final InputStream inputStream, final int defaultLen) throws IOException {
        if (inputStream == null) {
            return null;
        }

        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(defaultLen);
        final byte[] bytes = new byte[defaultLen];
        int read;
        while ((read = inputStream.read(bytes)) >= 0) {
            byteArrayOutputStream.write(bytes, 0, read);
        }
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        log.error(cause.getMessage(), cause);
        closeOnFlush(ctx.channel());
    }
}
