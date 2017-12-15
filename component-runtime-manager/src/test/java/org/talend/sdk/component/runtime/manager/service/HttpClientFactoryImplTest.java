/**
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.talend.sdk.component.api.service.http.Codec;
import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.Encoder;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.Request;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class HttpClientFactoryImplTest {

    @Test
    public void ok() {
        assertTrue(HttpClientFactoryImpl.createErrors(ComplexOk.class).isEmpty());
    }

    @Test
    public void encoderKo() {
        assertEquals(
                singletonList("public abstract java.lang.String "
                        + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$EncoderKo.main("
                        + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$Payload) "
                        + "defines a payload without an adapted coder"),
                HttpClientFactoryImpl.createErrors(EncoderKo.class));
    }

    @Test
    public void decoderKo() {
        assertEquals(singletonList("public abstract "
                + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$Payload "
                + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$DecoderKo.main(java.lang.String) "
                + "defines a payload without an adapted coder"), HttpClientFactoryImpl.createErrors(DecoderKo.class));
    }

    @Test
    public void methodKo() {
        assertEquals(singletonList("No @Request on public abstract java.lang.String "
                + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$MethodKo.main(java.lang.String)"),
                HttpClientFactoryImpl.createErrors(MethodKo.class));
    }

    @Test
    public void request() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/").setHandler(httpExchange -> {
            final Headers headers = httpExchange.getRequestHeaders();
            final byte[] bytes;
            try (final BufferedReader in =
                    new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8))) {
                bytes = (httpExchange.getRequestMethod() + "@"
                        + headers
                                .keySet()
                                .stream()
                                .sorted()
                                .filter(k -> !asList("Accept", "Host", "User-agent").contains(k))
                                .map(k -> k + "=" + headers.getFirst(k))
                                .collect(joining("/"))
                        + "@" + httpExchange.getRequestURI().toASCIIString() + "@" + in.lines().collect(joining("\n")))
                                .getBytes(StandardCharsets.UTF_8);
            }
            httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
            httpExchange.getResponseBody().write(bytes);
            httpExchange.close();
        });
        try {
            server.start();
            final ComplexOk ok = new HttpClientFactoryImpl("test").create(ComplexOk.class, null);
            HttpClient.class.cast(ok).base("http://localhost:" + server.getAddress().getPort() + "/api");

            final String result = ok.main4(new Payload("test"), "token", 1, "search yes").value;
            assertEquals(
                    "POST@" + "Authorization=token/" + "Connection=keep-alive/" + "Content-length=4/"
                            + "Content-type=application/x-www-form-urlencoded@" + "/api/?q=search+yes@" + "test",
                    result);
        } finally {
            server.stop(0);
        }
    }

    public interface ComplexOk extends HttpClient {

        @Request(method = "POST")
        String main1(String ok);

        @Request
        @Codec(decoder = PayloadCodec.class)
        Payload main2(String ok);

        @Request
        @Codec(decoder = PayloadCodec.class, encoder = PayloadCodec.class)
        Payload main3(Payload ok);

        @Request
        @Codec(decoder = PayloadCodec.class, encoder = PayloadCodec.class)
        Payload main4(Payload ok, @Header("Authorization") String auth, @Path("id") int id, @Query("q") String q);
    }

    public interface DecoderKo {

        @Request
        Payload main(String ok);
    }

    public interface EncoderKo {

        @Request
        String main(Payload payload);
    }

    public interface MethodKo {

        String main(String payload);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {

        private String value;
    }

    public static class PayloadCodec implements Decoder, Encoder {

        @Override
        public Object decode(final byte[] value, final Type expectedType) {
            return new Payload(new String(value));
        }

        @Override
        public byte[] encode(final Object value) {
            return Payload.class.cast(value).value.getBytes(StandardCharsets.UTF_8);
        }
    }
}
