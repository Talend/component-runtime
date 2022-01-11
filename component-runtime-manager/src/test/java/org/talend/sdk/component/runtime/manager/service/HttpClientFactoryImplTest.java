/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
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
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.talend.sdk.component.api.service.http.QueryFormat.MULTI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.bind.JsonbBuilder;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.xbean.propertyeditor.PropertyEditorRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.internationalization.Internationalized;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.http.Codec;
import org.talend.sdk.component.api.service.http.Configurer;
import org.talend.sdk.component.api.service.http.ConfigurerOption;
import org.talend.sdk.component.api.service.http.Decoder;
import org.talend.sdk.component.api.service.http.Encoder;
import org.talend.sdk.component.api.service.http.Header;
import org.talend.sdk.component.api.service.http.HttpClient;
import org.talend.sdk.component.api.service.http.HttpException;
import org.talend.sdk.component.api.service.http.HttpMethod;
import org.talend.sdk.component.api.service.http.Path;
import org.talend.sdk.component.api.service.http.Query;
import org.talend.sdk.component.api.service.http.QueryParams;
import org.talend.sdk.component.api.service.http.Request;
import org.talend.sdk.component.api.service.http.Response;
import org.talend.sdk.component.api.service.http.Url;
import org.talend.sdk.component.api.service.http.UseConfigurer;
import org.talend.sdk.component.api.service.http.configurer.oauth1.OAuth1;
import org.talend.sdk.component.runtime.manager.reflect.ParameterModelService;
import org.talend.sdk.component.runtime.manager.reflect.ReflectionService;
import org.talend.sdk.component.runtime.manager.service.http.HttpClientFactoryImpl;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

class HttpClientFactoryImplTest {

    @Test
    void multiQueryParam() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final MultiQueryParams client = newDefaultFactory()
                    .create(MultiQueryParams.class, "http://localhost:" + server.getAddress().getPort());
            final List<String> values = asList("0", "a b", "c/d");
            {
                final String result = client.csv(values);
                assertTrue(result.contains("@/?multip=0,a+b,c/d@"), result);
            }
            {
                final String result = client.multi(values);
                assertTrue(result.contains("@/?multip=0&multip=a+b&multip=c/d@"), result);
            }
            {
                final String result = client.csvs(singletonMap("multip", values));
                assertTrue(result.contains("@/?multip=0,a+b,c/d@"), result);
            }
            {
                final String result = client.multis(singletonMap("multip", values));
                assertTrue(result.contains("@/?multip=0&multip=a+b&multip=c/d@"), result);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void oauth1() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final OAuth1Client client =
                    newDefaultFactory().create(OAuth1Client.class, "http://localhost:" + server.getAddress().getPort());
            final String result = client
                    .call(OAuth1.Configuration
                            .builder()
                            .consumerKey("efrzfrf")
                            .consumerSecret("frzfrgtgt")
                            .token("gertgteg")
                            .tokenSecret("frzeefezfrf")
                            .timestamp(1736297329L)
                            .nonce("firjfirjiefjpfr")
                            .build());
            assertTrue(result
                    .startsWith("GET@Authorization=OAuth oauth_consumer_key=\"efrzfrf\", "
                            + "oauth_nonce=\"firjfirjiefjpfr\", oauth_signature=\""));
            // signature changes cause we have a dynamic port so don't test it
            assertTrue(result
                    .trim()
                    .endsWith("oauth_signature_method=\"HMAC-SHA1\", "
                            + "oauth_timestamp=\"1736297329\", oauth_token=\"gertgteg\", "
                            + "oauth_version=\"1.0\"/Connection=keep-alive@/1.1/statuses/user_timeline.json@"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ok() {
        assertNoError(HttpClientFactoryImpl.createErrors(ComplexOk.class));
        assertNoError(HttpClientFactoryImpl.createErrors(ResponseString.class));
        assertNoError(HttpClientFactoryImpl.createErrors(ResponseVoid.class));
        assertNoError(HttpClientFactoryImpl.createErrors(ResponseXml.class));
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
            final ComplexOk ok = newDefaultFactory().create(ComplexOk.class, null);
            ok.base("http://localhost:" + server.getAddress().getPort() + "/api");

            final String result = ok.main4(new Payload("test"), "token", 1, "search yes").value;
            assertEquals(
                    "POST@" + "Authorization=token/" + "Connection=keep-alive/" + "Content-length=4/"
                            + "Content-type=application/x-www-form-urlencoded@" + "/api?q=search+yes@" + "test",
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
            final ComplexOk ok = newDefaultFactory().create(ComplexOk.class, null);
            ok.base("http://localhost:" + server.getAddress().getPort() + "/api");

            final String result = ok.defaultMain1(new Payload("test"), "search yes").value;
            assertEquals(
                    "POST@" + "Authorization=token/" + "Connection=keep-alive/" + "Content-length=4/"
                            + "Content-type=application/x-www-form-urlencoded@" + "/api?q=search+yes@" + "test",
                    result);

            final Response<Payload> response = ok.main4Response(new Payload("test"), "token", 1, "search yes");
            assertEquals(
                    "POST@" + "Authorization=token/" + "Connection=keep-alive/" + "Content-length=4/"
                            + "Content-type=application/x-www-form-urlencoded@" + "/api?q=search+yes@" + "test",
                    response.body().value);
            assertEquals(HttpURLConnection.HTTP_OK, response.status());
            assertEquals("133", response.headers().get("content-length").iterator().next());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestWithXML() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/").setHandler(httpExchange -> {
            final Headers headers = httpExchange.getResponseHeaders();
            headers.set("content-type", "application/xml;charset=UTF-8");
            final byte[] bytes;
            try (final BufferedReader in =
                    new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8))) {
                bytes = in.lines().collect(joining("\n")).getBytes(StandardCharsets.UTF_8);
            }
            httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
            httpExchange.getResponseBody().write(bytes);
            httpExchange.close();
        });

        try {
            server.start();
            final ResponseXml client = newDefaultFactory().create(ResponseXml.class, null);
            client.base("http://localhost:" + server.getAddress().getPort() + "/api");

            final Response<XmlRecord> result = client.main("application/xml", new XmlRecord("xml content"));
            assertEquals("xml content", result.body().getValue());
            assertEquals(HttpURLConnection.HTTP_OK, result.status());
            assertEquals("104", result.headers().get("content-length").iterator().next());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestWithJSON() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/").setHandler(httpExchange -> {
            final Headers headers = httpExchange.getResponseHeaders();
            headers.set("content-type", "application/json;charset=UTF-8");
            final byte[] bytes;
            try (final BufferedReader in =
                    new BufferedReader(new InputStreamReader(httpExchange.getRequestBody(), StandardCharsets.UTF_8))) {
                bytes = in.lines().collect(joining("\n")).getBytes(StandardCharsets.UTF_8);
            }
            httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
            httpExchange.getResponseBody().write(bytes);
            httpExchange.close();
        });

        try {
            server.start();
            final ResponseJson client = newDefaultFactory().create(ResponseJson.class, null);
            client.base("http://localhost:" + server.getAddress().getPort() + "/api");

            final Response<Sample> result =
                    client.main("application/json", new Sample(singletonList(new Foo("testJSON"))));
            assertEquals(1, result.body().getFoos().size());
            assertEquals("testJSON", result.body().getFoos().iterator().next().getName());
            assertEquals(HttpURLConnection.HTTP_OK, result.status());
            assertEquals("30", result.headers().get("content-length").iterator().next());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void decoderWithServices() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final PropertyEditorRegistry propertyEditorRegistry = new PropertyEditorRegistry();
            final DecoderWithService client = new HttpClientFactoryImpl("test",
                    new ReflectionService(new ParameterModelService(propertyEditorRegistry), propertyEditorRegistry),
                    JsonbBuilder.create(), new HashMap<Class<?>, Object>() {

                        {
                            put(MyService.class, new MyService());
                            put(MyI18nService.class, (MyI18nService) () -> "error from i18n service");
                        }
                    }).create(DecoderWithService.class, null);
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
            final ComplexOk ok = newDefaultFactory().create(ComplexOk.class, null);
            ok.base("http://localhost:" + server.getAddress().getPort() + "/api");
            ok.main1("search yes");
        } catch (final HttpException e) {
            assertEquals(HttpURLConnection.HTTP_FORBIDDEN, e.getResponse().status());
            assertEquals(
                    "POST@Connection=keep-alive/Content-length=10/Content-type=application/x-www-form-urlencoded@/api@search yes",
                    e.getResponse().error(String.class));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ignoreNullQueryParam() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/").setHandler(httpExchange -> {
            final String query = httpExchange.getRequestURI().getQuery();
            httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, query.getBytes(StandardCharsets.UTF_8).length);
            httpExchange.getResponseBody().write(query.getBytes(StandardCharsets.UTF_8));
            httpExchange.close();
        });
        try {
            server.start();
            final IgnoreNullQueryParam ok = newDefaultFactory().create(IgnoreNullQueryParam.class, null);
            ok.base("http://localhost:" + server.getAddress().getPort() + "/api");
            String query = ok.get("value", "", null);
            final Map<String, String> params = Stream.of(query.split("&")).map(s -> {
                final int equal = s.indexOf('=');
                if (equal > 0) {
                    return new String[] { s.substring(0, equal), s.substring(equal + 1, s.length()) };
                }
                return new String[] { s, "true" };
            }).collect(toMap(s -> s[0], s -> s[1]));
            assertTrue(params.containsKey("param"));
            assertTrue(params.containsKey("emptyParam"));
            assertTrue(!params.containsKey("nullParam"));
        } finally {
            server.stop(0);
        }
    }

    interface SimpleClient extends HttpClient {

        @Request(path = "/api/{userId}")
        Response<byte[]> doRequest(@Path(value = "userId") String id);
    }

    @Test
    void pathPlaceholder() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final SimpleClient httpClient = newDefaultFactory().create(SimpleClient.class, null);
            httpClient.base("http://localhost:" + server.getAddress().getPort());
            Response<byte[]> response = httpClient.doRequest("ABC123");
            assertEquals("GET@Connection=keep-alive@/api/ABC123@", new String(response.body()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rawClient() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final RawClient httpClient = newDefaultFactory().create(RawClient.class, null);
            httpClient.base("http://localhost:" + server.getAddress().getPort());
            {
                final Response<InputStream> response = httpClient.doRequest("ABC123");
                assertEquals("GET@Connection=keep-alive@/api/ABC123@",
                        new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))
                                .lines()
                                .collect(joining("\n")));
            }
            {
                final InputStream response = httpClient.doRequestNoWrapper("ABC123");
                assertEquals("GET@Connection=keep-alive@/api/ABC123@",
                        new BufferedReader(new InputStreamReader(response, StandardCharsets.UTF_8))
                                .lines()
                                .collect(joining("\n")));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestGeneric() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            final Map<String, String> headers = new HashMap<String, String>() {

                {
                    put("Content-type", "application/json");
                }
            };

            final Map<String, String> queries = new HashMap<String, String>() {

                {
                    put("param", "value to be encoded");
                    put("emptyParam", "");
                    put("nullParam", null);
                }
            };

            server.start();
            final GenericClient httpClient = newDefaultFactory().create(GenericClient.class, null);
            Response<byte[]> response = httpClient
                    .execute(2000, 2000, "http://localhost:" + server.getAddress().getPort() + "/api", "POST", headers,
                            queries, "body data");
            assertEquals(
                    "POST@Authorization=Basic ABCD/Connection=keep-alive/Content-length=9/Content-type=application/json@/api?emptyParam=&param=value+to+be+encoded@body data",
                    new String(response.body()));

        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestGenericWithNullPayload() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final GenericClient httpClient = newDefaultFactory().create(GenericClient.class, null);
            Response<byte[]> response = httpClient
                    .execute(2000, 2000, "http://localhost:" + server.getAddress().getPort() + "/api", "POST", null,
                            null, null);
            assertEquals("POST@Authorization=Basic ABCD/Connection=keep-alive@/api@", new String(response.body()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void requestGenericDoNotIgnorePath() throws IOException {
        final HttpServer server = createTestServer(HttpURLConnection.HTTP_OK);
        try {
            server.start();
            final Map<String, String> queries = new HashMap<String, String>() {

                {
                    put("param", "value v2");
                }
            };
            final GenericClient httpClient = newDefaultFactory().create(GenericClient.class, null);
            Response<byte[]> response = httpClient
                    .doNotEncodeQueryParams("http://localhost:" + server.getAddress().getPort() + "/api/", queries);
            assertEquals("PUT@Connection=keep-alive@/api/?param=value@", new String(response.body()));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void noRedirect() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/").setHandler(httpExchange -> {
            if ("/redirect-me".equals(httpExchange.getRequestURI().getPath())) {
                httpExchange
                        .getResponseHeaders()
                        .set("Location", "http://localhost:" + server.getAddress().getPort() + "/done");
                httpExchange.getResponseHeaders().set("From", "redirect");
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_MOVED_TEMP, 0);
                httpExchange.close();
            } else {
                httpExchange.getResponseHeaders().set("Req", '>' + httpExchange.getRequestURI().getPath() + '<');
                httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
                httpExchange.close();
            }
        });
        try {
            server.start();
            final GenericClient httpClient = newDefaultFactory().create(GenericClient.class, null);
            httpClient.base("http://localhost:" + server.getAddress().getPort());
            {
                final Response<byte[]> response = httpClient.execute(true);
                assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, response.status());
                assertEquals("redirect", String.join(",", response.headers().get("from")));
            }
            {
                final Response<byte[]> response = httpClient.execute(false);
                assertEquals(HttpURLConnection.HTTP_OK, response.status());
                assertEquals(">/done<", String.join(",", response.headers().get("req")));
            }
        } finally {
            server.stop(0);
        }
    }

    private HttpClientFactoryImpl newDefaultFactory() {
        final PropertyEditorRegistry propertyEditorRegistry = new PropertyEditorRegistry();
        return new HttpClientFactoryImpl("test",
                new ReflectionService(new ParameterModelService(propertyEditorRegistry), propertyEditorRegistry),
                JsonbBuilder.create(), emptyMap());
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

    public interface MultiQueryParams extends HttpClient {

        @Request
        String csv(@Query("multip") final List<String> values);

        @Request
        String multi(@Query(value = "multip", format = MULTI) final List<String> values);

        @Request
        String csvs(@QueryParams final Map<String, List<String>> values);

        @Request
        String multis(@QueryParams(format = MULTI) final Map<String, List<String>> values);
    }

    public interface OAuth1Client extends HttpClient {

        @Request(path = "/1.1/statuses/user_timeline.json")
        @UseConfigurer(OAuth1.Configurer.class)
        String call(@ConfigurerOption("oauth1") final OAuth1.Configuration configuration);
    }

    interface RawClient extends HttpClient {

        @Request(path = "/api/{userId}")
        Response<InputStream> doRequest(@Path(value = "userId") String id);

        @Request(path = "/api/{userId}")
        InputStream doRequestNoWrapper(@Path(value = "userId") String id);
    }

    private interface GenericClient extends HttpClient {

        @Request(path = "/redirect-me")
        @UseConfigurer(NoRedirectConfigurer.class)
        Response<byte[]> execute(@ConfigurerOption("disableRedirect") boolean disableRedirect);

        @Request
        @UseConfigurer(value = MonConfigurer.class)
        Response<byte[]> execute(@ConfigurerOption("readTimeout") Integer readTimeout,
                @ConfigurerOption("connectionTimeout") Integer connectionTimeout, @Url String url,
                @HttpMethod String method,
                @org.talend.sdk.component.api.service.http.Headers Map<String, String> headers,
                @QueryParams Map<String, String> queryParams, String payload);

        @Request(method = "PUT", path = "/ignored")
        Response<byte[]> doNotEncodeQueryParams(@Url String url,
                @QueryParams(encode = false) Map<String, String> queryParams);

        class NoRedirectConfigurer implements Configurer {

            @Override
            public void configure(final Connection connection, final ConfigurerConfiguration configuration) {
                if (Boolean.class.cast(configuration.get("disableRedirect", Boolean.class))) {
                    connection.withoutFollowRedirects();
                }
            }
        }

        class MonConfigurer implements Configurer {

            @Override
            public void configure(final Connection connection, final ConfigurerConfiguration configuration) {
                connection.withHeader("Authorization", "Basic ABCD");
                connection.withReadTimeout(configuration.get("readTimeout", Integer.class));
                connection.withConnectionTimeout(configuration.get("connectionTimeout", Integer.class));
            }
        }
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

    public interface IgnoreNullQueryParam extends HttpClient {

        @Request
        String get(@Query("param") String param, @Query("emptyParam") String emptyParam,
                @Query("nullParam") String nullParam);
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

    public interface ResponseXml extends HttpClient {

        @Request(method = "POST")
        Response<XmlRecord> main(@Header("content-type") String contentType, XmlRecord payload);
    }

    public interface ResponseJson extends HttpClient {

        @Request(method = "POST")
        Response<Sample> main(@Header("content-type") String contentType, Sample payload);
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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @XmlRootElement
    public static class XmlRecord {

        private String value;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Sample {

        private Collection<Foo> foos;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Foo {

        private String name;
    }
}