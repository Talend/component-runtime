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

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static org.talend.sdk.component.junit.http.internal.impl.Handlers.closeOnFlush;
import static org.talend.sdk.component.junit.http.internal.impl.Handlers.sendError;

import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

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

        api.getExecutor().execute(() -> {
            final Map<String, String> headers = StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(request.headers().iteratorAsString(),
                            Spliterator.IMMUTABLE), false)
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
            final Optional<Response> matching = api.getResponseLocator().findMatching(
                    new RequestImpl(request.uri(), request.method().name(), headers), api.getHeaderFilter());
            if (!matching.isPresent()) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }

            final Response resp = matching.get();
            final ByteBuf bytes = ofNullable(resp.payload()).map(Unpooled::copiedBuffer).orElse(Unpooled.EMPTY_BUFFER);
            final HttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(resp.status()), bytes);
            HttpUtil.setContentLength(response, bytes.array().length);

            // todo: config for it
            response.headers().set("X-Talend-Proxy-JUnit", "true");

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
