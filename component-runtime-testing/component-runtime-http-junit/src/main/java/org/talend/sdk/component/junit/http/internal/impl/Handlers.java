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
import static lombok.AccessLevel.PRIVATE;

import java.io.File;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class Handlers {

    static final AttributeKey<String> BASE = AttributeKey.newInstance(Handlers.class.getName() + "#BASE");

    static void closeOnFlush(final Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER);
        }
    }

    static void sendError(final ChannelHandlerContext ctx, final HttpResponseStatus status) {
        final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("Failure: " + status.toString() + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set("X-Talend-Proxy-JUnit", "default-response");
        ctx.writeAndFlush(response);
    }

    public static boolean isActive(final String handler) {
        return "capture".equals(handler) ? getBaseCapture() != null
                : Boolean.parseBoolean(System.getProperty("talend.junit.http." + handler));
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
