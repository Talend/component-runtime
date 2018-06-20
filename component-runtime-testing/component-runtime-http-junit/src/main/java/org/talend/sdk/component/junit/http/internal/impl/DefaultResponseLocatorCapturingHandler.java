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

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

import org.talend.sdk.component.junit.http.api.HttpApiHandler;
import org.talend.sdk.component.junit.http.api.Response;

import io.netty.handler.codec.http.FullHttpRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultResponseLocatorCapturingHandler extends PassthroughHandler {

    public DefaultResponseLocatorCapturingHandler(final HttpApiHandler api) {
        super(api);
    }

    @Override
    protected void beforeResponse(final String requestUri, final FullHttpRequest request, final Response resp,
            final Map<String, List<String>> responseHeaderFields) {
        final DefaultResponseLocator.RequestModel requestModel = new DefaultResponseLocator.RequestModel();
        requestModel.setMethod(request.method().name());
        requestModel.setUri(requestUri);
        requestModel.setHeaders(filterHeaders(request.headers()));
        final DefaultResponseLocator.Model model = new DefaultResponseLocator.Model();
        model.setRequest(requestModel);

        final DefaultResponseLocator.ResponseModel responseModel = new DefaultResponseLocator.ResponseModel();
        responseModel.setStatus(resp.status());
        responseModel.setHeaders(filterHeaders(resp.headers().entrySet()));
        // todo: support as byte[] for not text responses
        if (resp.payload() != null) {
            if (responseHeaderFields.getOrDefault("Content-Encoding", emptyList()).stream().anyMatch(
                    it -> it.contains("gzip"))) {
                responseModel.setPayload(new String(degzip(resp.payload()), StandardCharsets.UTF_8));
            } else {
                responseModel.setPayload(new String(resp.payload(), StandardCharsets.UTF_8));
            }
        }
        model.setResponse(responseModel);

        if (DefaultResponseLocator.class.isInstance(api.getResponseLocator())) {
            DefaultResponseLocator.class.cast(api.getResponseLocator()).getCapturingBuffer().add(model);
        }
    }

    private byte[] degzip(final byte[] payloadBytes) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] bytes = new byte[1024 * 8];
        int read;
        try (final InputStream stream = new GZIPInputStream(new ByteArrayInputStream(payloadBytes))) {
            while ((read = stream.read(bytes)) >= 0) {
                if (read == 0) {
                    continue;
                }
                out.write(bytes, 0, read);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    private Map<String, String> filterHeaders(final Iterable<Map.Entry<String, String>> headers) {
        return StreamSupport
                .stream(headers.spliterator(), false)
                .filter(h -> !api.getHeaderFilter().test(h.getKey()))
                .sorted(comparing(Map.Entry::getKey))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
