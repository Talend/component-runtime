/**
 * Copyright (C) 2006-2026 Talend Inc. - www.talend.com
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

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.ziplock.IO;
import org.junit.jupiter.api.RepeatedTest;
import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.Response;
import org.talend.sdk.component.junit.http.internal.impl.ResponseImpl;

@HttpApi(useSsl = true)
class JUnit5HttpsApiTest {

    @HttpApiInject
    private HttpApiHandler<?> handler;

    @HttpApiName("${class}_${method}")
    @RepeatedTest(5) // just ensure it is reentrant
    void getProxy() throws Exception {
        final Response response = get();
        assertEquals(HttpURLConnection.HTTP_OK, response.status());
        assertEquals("worked as expected", new String(response.payload()));
        assertEquals("text/plain", response.headers().get("content-type"));
        assertEquals("true", response.headers().get("mocked"));
        assertEquals("true", response.headers().get("X-Talend-Proxy-JUnit"));
    }

    private Response get() throws Exception {
        final URL url = new URL("https://foo.bar.not.existing.talend.com/component/test?api=true");
        final HttpsURLConnection connection = HttpsURLConnection.class.cast(url.openConnection());
        connection.setSSLSocketFactory(handler.getSslContext().getSocketFactory());
        connection.setConnectTimeout(300000);
        connection.setReadTimeout(20000);
        connection.connect();
        final int responseCode = connection.getResponseCode();
        try {
            final Map<String, String> headers = connection
                    .getHeaderFields()
                    .entrySet()
                    .stream()
                    .filter(e -> e.getKey() != null)
                    .collect(toMap(Map.Entry::getKey, e -> e.getValue().stream().collect(joining(","))));
            return new ResponseImpl(headers, responseCode, IO.readBytes(connection.getInputStream()));
        } finally {
            connection.disconnect();
        }
    }
}
