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
package org.talend.sdk.component.proxy;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.test.WithProxy;

@WithProxy
class ProxyTest {

    @Test // todo, just a sample to show how to use it for now
    void listRootConfigs(final WebTarget proxyClient) throws IOException {
        assertTrue(proxyClient.path("configuration/roots").request(APPLICATION_JSON_TYPE).get(String.class).contains(
                "\"familyLabel\":\"TheTestFamily\""));
    }
}
