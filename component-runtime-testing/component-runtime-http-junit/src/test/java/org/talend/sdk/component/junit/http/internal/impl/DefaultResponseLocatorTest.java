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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.junit.http.api.Request;

class DefaultResponseLocatorTest {

    @Test
    void doesHeadersMatch() {
        final DefaultHeaderFilter headerFilter = new DefaultHeaderFilter();
        final DefaultResponseLocator locator = new DefaultResponseLocator(DefaultResponseLocator.PREFIX, null);
        final String uri = "https://some.location.io/endpoint?test=true&identifier=zorglub";
        final String method = "POST";
        final String payload = "{sample-payload; invalid stuff: true}";
        final Map<String, String> headers = new HashMap<>();
        final String contentLength = String.valueOf(payload.length());
        // add filtered headers
        headers.put("AuthoriZation", "auth");
        headers.put("toKen", "tok0");
        headers.put("pAsswOrd", "pass");
        headers.put("sEcrEt", "secr1t");
        headers.put("User-Agent", "mozilla");
        headers.put("Host", "patator");
        headers.put("Date", "11/18/2022");
        headers.put("Content-Encoding", "utf-8");
        // specific request headers
        headers.put("content-length", contentLength);
        headers.put("X-Talend-Proxy-JUnit", "default-response");
        // model stored in json
        final DefaultResponseLocator.RequestModel model = new DefaultResponseLocator.RequestModel();
        model.setUri(uri);
        model.setMethod(method);
        model.setPayload(payload);
        final Map<String, String> modelHeaders = new HashMap<>();
        modelHeaders.put("Content-Length", contentLength);
        modelHeaders.put("X-Talend-Proxy-JUnit", "default-response");
        model.setHeaders(modelHeaders);
        // all match check
        Request request = new RequestImpl(uri, method, payload, headers);
        assertTrue(locator.doesHeadersMatch(request, model, headerFilter));
        // change a model header value
        model.getHeaders().put("Content-Length", "123");
        assertFalse(locator.doesHeadersMatch(request, model, headerFilter));
        // add header not present in model
        model.getHeaders().put("Content-Length", contentLength);
        request.headers().put("interval", "cyclic");
        assertTrue(locator.doesHeadersMatch(request, model, headerFilter));
        // add header not present in request
        request.headers().remove("interval");
        model.getHeaders().put("interval", "cyclic");
        assertFalse(locator.doesHeadersMatch(request, model, headerFilter));
        // distinct values
        request.headers().put("IntErvAl", "annual");
        assertFalse(locator.doesHeadersMatch(request, model, headerFilter));
        // final check
        request.headers().put("IntErvAl", "cyclic");
        assertTrue(locator.doesHeadersMatch(request, model, headerFilter));
    }

}