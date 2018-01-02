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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.Response;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
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
public class DefaultResponseLocatorCapturingHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final HttpApiHandler api;

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest request) {
        api.getExecutor().execute(() -> {
            final DefaultResponseLocator.RequestModel requestModel = new DefaultResponseLocator.RequestModel();
            requestModel.setMethod(request.method().name());
            requestModel.setUri(request.uri());
            requestModel.setHeaders(StreamSupport
                    .stream(Spliterators.spliteratorUnknownSize(request.headers().iteratorAsString(),
                            Spliterator.IMMUTABLE), false)
                    .filter(h -> !api.getHeaderFilter().test(h.getKey()))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
            final DefaultResponseLocator.Model model = new DefaultResponseLocator.Model();
            model.setRequest(requestModel);

            // do the remote request with all the incoming data and save it
            // note: this request must be synchronous for now
            final Response resp;
            try {
                final URL url = new URL(request.uri());
                final HttpURLConnection connection = HttpURLConnection.class.cast(url.openConnection(Proxy.NO_PROXY));
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(20000);
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

            final DefaultResponseLocator.ResponseModel responseModel = new DefaultResponseLocator.ResponseModel();
            responseModel.setStatus(resp.status());
            responseModel.setHeaders(resp.headers());
            // todo: support as byte[] for not text responses
            responseModel.setPayload(new String(resp.payload(), StandardCharsets.UTF_8));
            model.setResponse(responseModel);

            if (DefaultResponseLocator.class.isInstance(api.getResponseLocator())) {
                DefaultResponseLocator.class.cast(api.getResponseLocator()).getCapturingBuffer().add(model);
            }

            final ByteBuf bytes = ofNullable(resp.payload()).map(Unpooled::copiedBuffer).orElse(Unpooled.EMPTY_BUFFER);
            final HttpResponse response =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(resp.status()), bytes);
            HttpUtil.setContentLength(response, bytes.array().length);

            ofNullable(resp.headers()).ifPresent(h -> h.forEach((k, v) -> response.headers().set(k, v)));
            ctx.writeAndFlush(response);
        });
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

    public static boolean isActive() {
        return getBaseCapture() != null;
    }

    // yes, it could be in the API but to avoid to keep it and pass tests in capture mode
    // we hide it this way for now
    public static String getBaseCapture() {
        return ofNullable(System.getProperty("talend.junit.http.capture")).map(value -> {
            if ("true".equalsIgnoreCase(value)) { // try to guess
                final File file = new File("src/test/resources");
                if (file.isDirectory()) {
                    return file.getAbsolutePath();
                }
            }
            return value;
        }).orElse(null);
    }
}
