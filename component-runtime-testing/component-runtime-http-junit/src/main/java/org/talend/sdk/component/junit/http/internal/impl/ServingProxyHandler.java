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

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.junit.http.internal.impl.Handlers.closeOnFlush;
import static org.talend.sdk.component.junit.http.internal.impl.Handlers.sendError;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import javax.net.ssl.SSLEngine;

import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
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
@ChannelHandler.Sharable
public class ServingProxyHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final HttpApiHandler api;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        final String payload = request.content().toString(StandardCharsets.UTF_8);

        api.getExecutor().execute(() -> {
            final Map<String, String> headers = StreamSupport
                    .stream(Spliterators
                            .spliteratorUnknownSize(request.headers().iteratorAsString(), Spliterator.IMMUTABLE), false)
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            final Attribute<String> baseAttr = ctx.channel().attr(Handlers.BASE);
            Optional<Response> matching = api
                    .getResponseLocator()
                    .findMatching(new RequestImpl(
                            (baseAttr == null || baseAttr.get() == null ? "" : baseAttr.get()) + request.uri(),
                            request.method().name().toString(), payload, headers), api.getHeaderFilter());
            if (!matching.isPresent()) {
                if (HttpMethod.CONNECT.name().equalsIgnoreCase(request.method().name())) {
                    final Map<String, String> responseHeaders = new HashMap<>();
                    responseHeaders.put(HttpHeaderNames.CONNECTION.toString(), HttpHeaderValues.KEEP_ALIVE.toString());
                    responseHeaders.put(HttpHeaderNames.CONTENT_LENGTH.toString(), "0");
                    matching = of(new ResponseImpl(responseHeaders, HttpResponseStatus.OK.code(),
                            Unpooled.EMPTY_BUFFER.array()));
                    if (api.getSslContext() != null) {
                        final SSLEngine sslEngine = api.getSslContext().createSSLEngine();
                        sslEngine.setUseClientMode(false);
                        ctx.channel().pipeline().addFirst("ssl", new SslHandler(sslEngine, true));

                        final String uri = request.uri();
                        final String[] parts = uri.split(":");
                        ctx
                                .channel()
                                .attr(Handlers.BASE)
                                .set("https://" + parts[0]
                                        + (parts.length > 1 && !"443".equals(parts[1]) ? ":" + parts[1] : ""));
                    }
                } else {
                    sendError(ctx, new HttpResponseStatus(HttpURLConnection.HTTP_BAD_REQUEST,
                            "You are in proxy mode. No response was found for the simulated request. Please ensure to capture it for next executions. "
                                    + request.method().name() + " " + request.uri()));
                    return;
                }
            }

            final Response resp = matching.get();
            final ByteBuf bytes = ofNullable(resp.payload()).map(Unpooled::copiedBuffer).orElse(Unpooled.EMPTY_BUFFER);
            final HttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(resp.status()), bytes);
            HttpUtil.setContentLength(response, bytes.array().length);

            if (!api.isSkipProxyHeaders()) {
                response.headers().set("X-Talend-Proxy-JUnit", "true");
            }

            ofNullable(resp.headers()).ifPresent(h -> h.forEach((k, v) -> response.headers().set(k, v)));
            ctx.writeAndFlush(response);
        });
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        log.error(cause.getMessage(), cause);
        closeOnFlush(ctx.channel());
    }
}
