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
package org.talend.sdk.component.junit.http.junit5;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ziplock.IO;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.Response;
import org.talend.sdk.component.junit.http.internal.impl.ResponseImpl;

@HttpApi
class JUnit5HttpApiTest {

    @HttpApiInject
    private HttpApiHandler<?> handler;

    @Test
    void withoutQueryParams() throws Exception {
        {
            final Response response =
                    execute("POST", "http://foo.bar.not.existing.talend.com/component/test?api=false", "ignored");
            assertEquals(HttpURLConnection.HTTP_OK, response.status());
            assertEquals("first", new String(response.payload(), StandardCharsets.UTF_8));
        }
        {
            final Response response =
                    execute("POST", "http://foo.bar.not.existing.talend.com/component/test?api=true", "ignored");
            assertEquals(HttpURLConnection.HTTP_OK, response.status());
            assertEquals("second", new String(response.payload(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void withPayload() throws Exception {
        {
            final Response response =
                    execute("POST", "http://foo.bar.not.existing.talend.com/component/test?api=true", "first");
            assertEquals(HttpURLConnection.HTTP_OK, response.status());
            assertEquals("one", new String(response.payload(), StandardCharsets.UTF_8));
        }
        {
            final Response response =
                    execute("POST", "http://foo.bar.not.existing.talend.com/component/test?api=true", "second");
            assertEquals(HttpURLConnection.HTTP_OK, response.status());
            assertEquals("two", new String(response.payload(), StandardCharsets.UTF_8));
        }
        {
            final Response response = execute("POST", "http://foo.bar.not.existing.talend.com/component/test?api=true",
                    "start something whatever");
            assertEquals(HttpURLConnection.HTTP_OK, response.status());
            assertEquals("fallback", new String(response.payload(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void direct() throws Exception { // ensure it responds when directly called
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST,
                execute("GET", "http://localhost:" + handler.getPort(), null).status());
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST,
                execute("POST", "http://localhost:" + handler.getPort(), "whatever").status());
    }

    @Test
    void getProxy() throws Exception {
        final Response response =
                execute("GET", "http://foo.bar.not.existing.talend.com/component/test?api=true", null);
        assertEquals(HttpURLConnection.HTTP_OK, response.status());
        assertEquals("worked as expected", new String(response.payload()));
        assertEquals("text/plain", response.headers().get("content-type"));
        assertEquals("true", response.headers().get("mocked"));
        assertEquals("true", response.headers().get("X-Talend-Proxy-JUnit"));
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
            final Map<String, String> headers = connection
                    .getHeaderFields()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey() != null)
                    .collect(toMap(Map.Entry::getKey, e -> e.getValue().stream().collect(Collectors.joining(","))));
            return new ResponseImpl(headers, responseCode,
                    responseCode < 399 ? IO.readBytes(connection.getInputStream()) : null);
        } finally {
            connection.disconnect();
        }
    }
}
