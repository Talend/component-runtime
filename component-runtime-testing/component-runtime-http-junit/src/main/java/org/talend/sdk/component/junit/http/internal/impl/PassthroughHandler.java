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
package org.talend.sdk.component.junit.http.internal.impl;

import static io.netty.handler.codec.http.HttpUtil.setContentLength;
import static io.netty.handler.codec.http.HttpUtil.setKeepAlive;
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
import java.util.Map;
import java.util.stream.Collectors;

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
            setKeepAlive(response, true);
            setContentLength(response, 0);
            if (api.getSslContext() != null) {
                final SSLEngine sslEngine = api.getSslContext().createSSLEngine();
                sslEngine.setUseClientMode(false);
                ctx.channel().pipeline().addFirst("ssl", new SslHandler(sslEngine, true));

                final String uri = request.uri();
                final String[] parts = uri.split(":");
                ctx.channel().attr(BASE).set(
                        "https://" + parts[0] + (parts.length > 1 && !"443".equals(parts[1]) ? ":" + parts[1] : ""));
            }
            ctx.writeAndFlush(response);
            return;
        }

        api.getExecutor().execute(() -> {
            final Attribute<String> baseAttr = ctx.channel().attr(Handlers.BASE);
            final String requestUri =
                    (baseAttr == null || baseAttr.get() == null ? "" : baseAttr.get()) + request.uri();

            // do the remote request with all the incoming data and save it
            // note: this request must be synchronous for now
            final Response resp;
            try {
                final URL url = new URL(requestUri);
                final HttpURLConnection connection = HttpURLConnection.class.cast(url.openConnection(Proxy.NO_PROXY));
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(20000);
                if (HttpsURLConnection.class.isInstance(connection)) {
                    final HttpsURLConnection httpsURLConnection = HttpsURLConnection.class.cast(connection);
                    httpsURLConnection.setHostnameVerifier((h, s) -> true);
                    httpsURLConnection.setSSLSocketFactory(api.getSslContext().getSocketFactory());
                }
                if (request.method() != null) {
                    final String requestMethod = request.method().name();
                    connection.setRequestMethod(requestMethod);

                    if (!"HEAD".equalsIgnoreCase(requestMethod) && request.content().readableBytes() > 0) {
                        connection.setDoOutput(true);
                        request.content().readBytes(connection.getOutputStream(), request.content().readableBytes());
                    }
                }

                final int responseCode = connection.getResponseCode();
                final int defaultLength =
                        ofNullable(connection.getHeaderField("content-length")).map(Integer::parseInt).orElse(8192);
                resp = new ResponseImpl(
                        connection
                                .getHeaderFields()
                                .entrySet()
                                .stream()
                                .filter(e -> e.getKey() != null)
                                .filter(h -> !api.getHeaderFilter().test(h.getKey()))
                                .collect(toMap(Map.Entry::getKey,
                                        e -> e.getValue().stream().collect(Collectors.joining(",")))),

                        responseCode, responseCode <= 399 ? slurp(connection.getInputStream(), defaultLength)
                                : slurp(connection.getErrorStream(), defaultLength));
            } catch (final Exception e) {
                log.error(e.getMessage(), e);
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            beforeResponse(requestUri, request, resp);

            final ByteBuf bytes = ofNullable(resp.payload()).map(Unpooled::copiedBuffer).orElse(Unpooled.EMPTY_BUFFER);
            final HttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(resp.status()), bytes);
            HttpUtil.setContentLength(response, bytes.array().length);

            ofNullable(resp.headers()).ifPresent(h -> h.forEach((k, v) -> response.headers().set(k, v)));
            ctx.writeAndFlush(response);
        });
    }

    protected void beforeResponse(final String requestUri, final FullHttpRequest request, final Response resp) {
        // no-op
    }

    private byte[] slurp(final InputStream inputStream, final int defaultLen) throws IOException {
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
