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
package org.talend.sdk.component.runtime.manager.service;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.http.Codec;
import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.Encoder;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.HttpException;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.http.Response;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class HttpClientFactoryImplTest {

    @Test
    void ok() {
        assertNoError(HttpClientFactoryImpl.createErrors(ComplexOk.class));
        assertNoError(HttpClientFactoryImpl.createErrors(ResponseString.class));
        assertNoError(HttpClientFactoryImpl.createErrors(ResponseVoid.class));
    }

    @Test
    void encoderKo() {
        assertEquals(
                singletonList("public abstract java.lang.String "
                        + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$EncoderKo.main("
                        + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$Payload) "
                        + "defines a request payload without an adapted coder"),
                HttpClientFactoryImpl.createErrors(EncoderKo.class));
    }

    @Test
    void decoderKo() {
        assertEquals(singletonList("public abstract "
                        + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$Payload "
                        + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$DecoderKo.main(java.lang.String) "
                        + "defines a response payload without an adapted coder"),
                HttpClientFactoryImpl.createErrors(DecoderKo.class));
    }

    @Test
    void methodKo() {
        assertEquals(singletonList("No @Request on public abstract java.lang.String "
                        + "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest$MethodKo.main(java.lang.String)"),
                HttpClientFactoryImpl.createErrors(MethodKo.class));
    }

    @Test
    void clientKo() {
        assertEquals(singletonList(
                "org.talend.sdk.component.runtime.manager.service.HttpClientFactoryImplTest.ClientKo should extends HttpClient"),
                HttpClientFactoryImpl.createErrors(ClientKo.class));
    }

    @Test
    void request() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final ComplexOk ok =
                    new HttpClientFactoryImpl("test", new ReflectionService(new ParameterModelService()), emptyMap())
                            .create(ComplexOk.class, null);
            ok.base("http://localhost:" + server.getAddress().getPort() + "/api");

            final String result = ok.main4(new Payload("test"), "token", 1, "search yes").value;
            assertEquals(
                    "POST@" + "Authorization=token/" + "Connection=keep-alive/" + "Content-length=4/"
                            + "Content-type=application/x-www-form-urlencoded@" + "/api/?q=search+yes@" + "test",
                    result);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestDefault() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final ComplexOk ok =
                    new HttpClientFactoryImpl("test", new ReflectionService(new ParameterModelService()), emptyMap())
                            .create(ComplexOk.class, null);
            ok.base("http://localhost:" + server.getAddress().getPort() + "/api");

            final String result = ok.defaultMain1(new Payload("test"), "search yes").value;
            assertEquals(
                    "POST@" + "Authorization=token/" + "Connection=keep-alive/" + "Content-length=4/"
                            + "Content-type=application/x-www-form-urlencoded@" + "/api/?q=search+yes@" + "test",
                    result);

            final Response<Payload> response = ok.main4Response(new Payload("test"), "token", 1, "search yes");
            assertEquals(
                    "POST@" + "Authorization=token/" + "Connection=keep-alive/" + "Content-length=4/"
                            + "Content-type=application/x-www-form-urlencoded@" + "/api/?q=search+yes@" + "test",
                    response.body().value);
            assertEquals(HttpURLConnection.HTTP_OK, response.status());
            assertEquals("134", response.headers().get("content-length").iterator().next());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void decoderWithServices() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final DecoderWithService client =
                    new HttpClientFactoryImpl("test", new ReflectionService(new ParameterModelService()),
                            new HashMap<Class<?>, Object>() {{
                                put(MyService.class, new MyService());
                                put(MyI18nService.class, (MyI18nService) () -> "error from i18n service");
                            }})
                            .create(DecoderWithService.class, null);
            client.base("http://localhost:" + server.getAddress().getPort() + "/api");

            assertThrows(IllegalStateException.class, () -> client.error("search yes"));
            assertEquals(MyService.class.getCanonicalName(), client.ok().value);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void handleHttpError() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_FORBIDDEN);
        try {
            server.start();
            final ComplexOk ok =
                    new HttpClientFactoryImpl("test", new ReflectionService(new ParameterModelService()), emptyMap())
                            .create(ComplexOk.class, null);
            ok.base("http://localhost:" + server.getAddress().getPort() + "/api");
            ok.main1("search yes");
        } catch (final HttpException e) {
            assertEquals(HttpURLConnection.HTTP_FORBIDDEN, e.getResponse().status());
            assertEquals(
                    "POST@Connection=keep-alive/Content-length=10/Content-type=application/x-www-form-urlencoded@/api/@search yes",
                    e.getResponse().error(String.class));
        } finally {
            server.stop(0);
        }
    }

    private void assertNoError(final Collection<String> errors) {
        assertTrue(errors.isEmpty(), errors.toString());
    }

    private HttpServer createTestServer(int responseStatus) throws IOException {
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
            httpExchange.sendResponseHeaders(responseStatus, bytes.length);
            httpExchange.getResponseBody().write(bytes);
            httpExchange.close();
        });

        return server;
    }

    public interface ComplexOk extends HttpClient {

        @Request(method = "POST")
        String main1(String ok);

        @Request
        @Codec(decoder = { PayloadCodec.class })
        Payload main2(String ok);

        @Request(method = "POST")
        @Codec(decoder = PayloadCodec.class, encoder = PayloadCodec.class)
        Payload main3(Payload ok);

        @Request
        @Codec(decoder = PayloadCodec.class, encoder = PayloadCodec.class)
        Payload main4(Payload ok, @Header("Authorization") String auth, @Path("id") int id, @Query("q") String q);

        @Request
        @Codec(decoder = PayloadCodec.class, encoder = PayloadCodec.class)
        Response<Payload> main4Response(Payload ok, @Header("Authorization") String auth, @Path("id") int id,
                @Query("q") String q);

        default Payload defaultMain1(Payload ok, String q) {
            return main4(ok, "token", 1, q);
        }
    }

    public interface DecoderKo extends HttpClient {

        @Request
        Payload main(String ok);
    }

    public interface EncoderKo extends HttpClient {

        @Request
        String main(Payload payload);
    }

    public interface DecoderWithService extends HttpClient {

        @Request
        @Codec(decoder = CodecWithService.class, encoder = CodecWithService.class)
        Payload error(String ok);

        @Request
        @Codec(decoder = CodecWithService.class)
        Payload ok();
    }

    public interface MethodKo extends HttpClient {

        String main(String payload);
    }

    public interface ClientKo {

        @Request
        String main();
    }

    public interface ResponseString extends HttpClient {

        @Request
        Response<String> main();
    }

    public interface ResponseVoid extends HttpClient {

        @Request
        Response<Void> main();
    }

    @Internationalized
    public interface MyI18nService {

        String error();
    }

    @Service
    public static class MyService {

        public String decode() {
            return MyService.class.getCanonicalName();
        }
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

    @AllArgsConstructor
    public static class CodecWithService implements Decoder, Encoder {

        public final MyService myService;

        public final MyI18nService myI18nService;

        @Override
        public Object decode(final byte[] value, final Type expectedType) {
            return new Payload(myService.decode());
        }

        @Override
        public byte[] encode(final Object value) {
            throw new IllegalStateException(myI18nService.error());
        }
    }
}