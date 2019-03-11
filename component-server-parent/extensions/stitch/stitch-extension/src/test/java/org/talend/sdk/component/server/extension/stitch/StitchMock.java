/**
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.server.extension.stitch;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Path;
import javax.ws.rs.core.HttpHeaders;

@Path("model")
@ApplicationScoped
public class StitchMock {

    @GET
    public String getSourceTypes(@HeaderParam(HttpHeaders.AUTHORIZATION) final String auth) {
        if (!"test-token".equals(auth)) {
            throw new ForbiddenException();
        }
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(getModel()))) {
            return reader.lines().collect(joining("\n")).trim();
        } catch (final IOException e) {
            throw new InternalServerErrorException();
        }
    }

    private InputStream getModel() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("model.json");
    }
}
