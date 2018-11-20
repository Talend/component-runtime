/**
 * Copyright (C) 2006-2018 Talend Inc. - www.talend.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.runtime.manager.chain.internal;

import static java.net.URLEncoder.encode;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

class DSLParserTest {

    @Test
    void parseQuery() throws UnsupportedEncodingException {
        assertEquals(emptyMap(), DSLParser.parse("foo://bar").getConfiguration());

        assertEquals(singletonMap("a", "b"), DSLParser.parse("foo://bar?a=b").getConfiguration());

        final String url = "jdbc://foo?bar=dummy&1=2&delegateDriver=" + encode("jdbc://other:path?some=query", "UTF-8");
        assertEquals(singletonMap("configuration.url", url),
                DSLParser.parse("foo://bar?configuration.url=" + encode(url, "UTF-8")).getConfiguration());

        assertEquals(new HashMap<String, String>() {

            {
                put("configuration.url", url);
                put("configuration.password", "vault:v1:something");
            }
        }, DSLParser
                .parse("foo://bar?configuration.url=" + encode(url, "UTF-8")
                        + "&configuration.password=vault:v1:something")
                .getConfiguration());
    }
}
