/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.junit.http.junit4;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.RuleChain.emptyRuleChain;
import static org.talend.sdk.component.junit.http.test.json.AssertJson.assertJSONEquals;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CaptureJUnit4HttpApiTest {

    private static final JUnit4HttpApi API = new JUnit4HttpApi();

    private static final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    @ClassRule
    public static final TestRule TEST_SETUP = emptyRuleChain().around(TEMPORARY_FOLDER).around(new ExternalResource() {

        @Override
        protected void before() {
            System.setProperty("talend.junit.http.capture", TEMPORARY_FOLDER.getRoot().toString());
        }

        @Override
        protected void after() {
            System.clearProperty("talend.junit.http.capture");
        }
    }).around(API);

    @Test
    public void doCapture() throws Throwable {
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
            httpExchange.sendResponseHeaders(200, bytes.length);
            httpExchange.getResponseBody().write(bytes);
            httpExchange.close();
        });
        server.start();

        final Path output = TEMPORARY_FOLDER
                .getRoot()
                .toPath()
                .toAbsolutePath()
                .resolve("talend/testing/http/" + getClass().getName() + "_doCapture.json");

        try {
            new JUnit4HttpApiPerMethodConfigurator(API).apply(new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    final URL url = new URL("http://localhost:" + server.getAddress().getPort() + "/supertest");
                    final HttpURLConnection connection = HttpURLConnection.class
                            .cast(url
                                    .openConnection(new Proxy(Proxy.Type.HTTP,
                                            new InetSocketAddress("localhost", API.getPort()))));
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(20000);
                    connection.setRequestProperty("Accept", "*/*");
                    connection.setRequestProperty("Authorization", "should be filtered");
                    connection.setRequestProperty("ok", "yes");
                    assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
                    connection.disconnect();
                }
            }, Description.createTestDescription(getClass(), "doCapture")).evaluate();

            assertTrue(output.toFile().exists());
            final String lines = String.join("\n", Files.readAllLines(output));
            assertJSONEquals("[\n" + "  {\n" + "    \"request\":{\n" + "      \"headers\":{\n"
                    + "        \"content-length\":\"0\",\n"
                    + "        \"Accept\":\"*/*\",\n"
                    + "        \"ok\":\"yes\",\n" + "        \"Proxy-Connection\":\"keep-alive\"\n" + "      },\n"
                    + "      \"method\":\"GET\",\n" + "      \"uri\":\"http://localhost:"
                    + server.getAddress().getPort() + "/supertest\"\n" + "    },\n" + "    \"response\":{\n"
                    + "      \"headers\":{\n" + "        \"Content-length\":\"105\"\n" + "      },\n"
                    + "      \"payload\":\"GET@Authorization=should be filtered/Connection=keep-alive/Ok=yes/Proxy-connection=keep-alive@/supertest@\",\n"
                    + "      \"status\":200\n" + "    }\n" + "  }\n" + "]", lines);
        } finally {
            server.stop(0);
        }
    }
}
