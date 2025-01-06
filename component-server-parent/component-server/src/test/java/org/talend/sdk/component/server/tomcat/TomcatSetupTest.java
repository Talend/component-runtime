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
package org.talend.sdk.component.server.tomcat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;
import javax.ws.rs.client.Client;

import org.apache.meecrowave.Meecrowave;
import org.apache.meecrowave.junit5.MonoMeecrowaveConfig;
import org.junit.jupiter.api.Test;

@MonoMeecrowaveConfig
class TomcatSetupTest {

    @Inject
    private Client client;

    @Inject
    private Meecrowave.Builder config;

    @Test
    void ensureErrorDoesNotReportUnexpectedData() {
        final String errorPage = client
                .target(String.format("http://localhost:%d/willfailin404", config.getHttpPort()))
                .request()
                .get()
                .readEntity(String.class);
        {
            final int start = errorPage.indexOf("<title>");
            final int end = errorPage.indexOf("</title>");
            assertTrue(start > 0 && end > start);
            assertEquals("<title>HTTP Status 404 – Not Found", errorPage.substring(start, end));
        }
        {
            final int start = errorPage.indexOf("<body>");
            assertTrue(start > 0);
            assertEquals("<body><h1>HTTP Status 404 – Not Found</h1></body></html>", errorPage.substring(start));
        }
    }
}
