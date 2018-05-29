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
package org.talend.sdk.component.proxy.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.client.Invocation;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.proxy.test.WithServer;

@WithServer
class ConfigurationResourceProxyTest {

    @Test
    void checkHeaders() {
        final ProxyConfiguration configuration = CDI.current().select(ProxyConfiguration.class).get();
        final Map<String, String> headers = new HashMap<>();
        configuration.getHeaderAppender().apply(
                Invocation.Builder.class.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class<?>[] { Invocation.Builder.class }, (proxy, method, args) -> {
                            if (method.getName().equals("header")) {
                                headers.put(args[0].toString(), args[1].toString());
                            }
                            return proxy;
                        })),
                k -> "done");
        assertEquals(new HashMap<String, String>() {

            {
                put("Talend-Test", "true");
                put("Talend-Replaced", "done");
            }
        }, headers);
    }
}
