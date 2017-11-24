/**
 *  Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.talend.sdk.component.form.internal.jaxrs;

import static java.util.stream.Collectors.toMap;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.talend.sdk.component.form.api.Client;
import org.talend.sdk.component.form.api.WebException;
import org.talend.sdk.component.server.front.model.ComponentDetailList;
import org.talend.sdk.component.server.front.model.ComponentIndices;

public class JAXRSClient implements Client {

    private final javax.ws.rs.client.Client delegate;

    private final WebTarget target;

    private final GenericType<Map<String, Object>> mapType;

    public JAXRSClient(final String base) {
        delegate = ClientBuilder.newClient();
        target = delegate.target(base);
        mapType = new GenericType<Map<String, Object>>() {
        };
    }

    @Override
    public Map<String, Object> action(final String family, final String type, final String action,
        final Map<String, Object> params) {
        try {
            return target.path("action/execute").queryParam("family", family).queryParam("type", type)
                .queryParam("action", action).request(APPLICATION_JSON_TYPE)
                .post(entity(
                    params.entrySet().stream().collect(toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue()))),
                    APPLICATION_JSON_TYPE), mapType);
        } catch (final WebApplicationException wae) {
            throw toException(wae);
        }
    }

    @Override
    public ComponentIndices index(final String language) {
        try {
            return target.path("component/index").queryParam("language", language).request(APPLICATION_JSON_TYPE)
                .get(ComponentIndices.class);
        } catch (final WebApplicationException wae) {
            throw toException(wae);
        }
    }

    @Override
    public ComponentDetailList details(final String language, final String identifier, final String... identifiers) {
        try {
            return target.path("component/details").queryParam("language", language)
                .queryParam("identifiers",
                    Stream
                        .concat(Stream.of(identifier),
                            identifiers == null || identifiers.length == 0 ? Stream.empty() : Stream.of(identifiers))
                        .toArray(Object[]::new))
                .request(APPLICATION_JSON_TYPE).get(ComponentDetailList.class);
        } catch (final WebApplicationException wae) {
            throw toException(wae);
        }
    }

    @Override
    public void close() {
        delegate.close();
    }

    private WebException toException(final WebApplicationException wae) {
        final Response response = wae.getResponse();
        return new WebException(wae, response == null ? -1 : response.getStatus(),
            response == null ? null : response.readEntity(mapType));
    }
}
