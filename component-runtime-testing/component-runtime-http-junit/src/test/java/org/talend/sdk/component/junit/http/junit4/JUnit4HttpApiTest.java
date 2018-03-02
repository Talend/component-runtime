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
package org.talend.sdk.component.junit.http.junit4;

import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ziplock.IO;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.talend.sdk.component.junit.http.api.Response;
import org.talend.sdk.component.junit.http.internal.impl.ResponseImpl;

public class JUnit4HttpApiTest {

    @ClassRule
    public static final JUnit4HttpApi API = new JUnit4HttpApi();

    @Rule
    public final JUnit4HttpApiPerMethodConfigurator configurator = new JUnit4HttpApiPerMethodConfigurator(API);

    @Test
    public void direct() throws Exception { // ensure it responds when directly called
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST,
                execute("GET", "http://localhost:" + API.getPort(), null).status());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST,
                execute("POST", "http://localhost:" + API.getPort(), "whatever").status());
    }

    @Test
    public void getProxy() throws Exception {
        final Response response =
                execute("GET", "http://foo.bar.not.existing.talend.com/component/test?api=true", null);
        assertEquals(HttpURLConnection.HTTP_OK, response.status());
        assertEquals(new String(response.payload()), "worked as expected");
        assertEquals("text/plain", response.headers().get("content-type"));
        assertEquals("true", response.headers().get("mocked"));
        assertEquals("true", response.headers().get("X-Talend-Proxy-JUnit"));
    }

    @Test
    public void noSimulationFile() throws Exception {
        final URL url = new URL("http://foo.bar.not.existing.talend.com/component/test?api=true");
        final HttpURLConnection connection = HttpURLConnection.class.cast(url.openConnection());
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod("GET");
        try {
            assertEquals(HttpURLConnection.HTTP_BAD_REQUEST, connection.getResponseCode());
            assertEquals(
                    "You are in proxy mode. No response was found for the simulated request. Please ensure to capture it for next executions. GET http://foo.bar.not.existing.talend.com/component/test?api=true",
                    connection.getResponseMessage());
        } finally {
            connection.disconnect();
        }
    }

    private Response execute(final String method, final String uri, final String payload) throws Exception {
        final URL url = new URL(uri);
        final HttpURLConnection connection = HttpURLConnection.class.cast(url.openConnection());
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(20000);
        connection.setRequestMethod(method);
        if (payload != null) {
            connection.setDoOutput(true);
            connection.getOutputStream().write(payload.getBytes(StandardCharsets.UTF_8));
        }

        final int responseCode = connection.getResponseCode();
        try {
            final Map<String, String> headers =
                    connection.getHeaderFields().entrySet().stream().filter(e -> e.getKey() != null).collect(
                            toMap(Map.Entry::getKey, e -> e.getValue().stream().collect(Collectors.joining(","))));
            return new ResponseImpl(headers, responseCode,
                    responseCode < 399 ? IO.readBytes(connection.getInputStream()) : null);
        } finally {
            connection.disconnect();
        }
    }
}
