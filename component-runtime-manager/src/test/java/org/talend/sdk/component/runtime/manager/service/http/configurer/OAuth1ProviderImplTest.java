/**
 * Copyright (C) 2006-2024 Talend Inc. - www.talend.com
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
package org.talend.sdk.component.runtime.manager.service.http.configurer;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.service.http.Configurer;
import org.talend.sdk.component.api.service.http.configurer.oauth1.OAuth1;

class OAuth1ProviderImplTest {

    @Test
    void oauth1Hmac() {
        final OAuth1.Configurer configurer = new OAuth1.Configurer();
        final OAuth1.Configuration configuration = OAuth1.Configuration
                .builder()
                .consumerKey("key")
                .consumerSecret("secret")
                .token("requestkey")
                .tokenSecret("requestsecret")
                .header("Auth")
                .timestamp(1536181682L)
                .nonce("bf5830e5b01de3a4090a32137d3e8937")
                .build();
        final StringBuilder ref = new StringBuilder();
        configurer.configure(new Configurer.Connection() {

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public String getUrl() {
                return "http://term.ie/oauth/example/request_token.php?querywillbeignored=true";
            }

            @Override
            public Map<String, List<String>> getHeaders() {
                return emptyMap();
            }

            @Override
            public Configurer.Connection withHeader(final String name, final String value) {
                ref.append(name).append('=').append(value);
                return this;
            }

            @Override
            public byte[] getPayload() {
                return null;
            }

            @Override
            public Configurer.Connection withReadTimeout(final int timeout) {
                return this;
            }

            @Override
            public Configurer.Connection withConnectionTimeout(final int timeout) {
                return this;
            }

            @Override
            public Configurer.Connection withoutFollowRedirects() {
                throw new UnsupportedOperationException();
            }
        }, new Configurer.ConfigurerConfiguration() {

            @Override
            public Object[] configuration() {
                return new Object[] { configuration };
            }

            @Override
            public <T> T get(final String name, final Class<T> type) {
                throw new UnsupportedOperationException();
            }
        });
        assertEquals(
                "Auth=OAuth oauth_consumer_key=\"key\", oauth_nonce=\"bf5830e5b01de3a4090a32137d3e8937\", "
                        + "oauth_signature=\"6Y3OZPrZhcL3DbxOaoaIopDLDUM%3D\", oauth_signature_method=\"HMAC-SHA1\", "
                        + "oauth_timestamp=\"1536181682\", oauth_token=\"requestkey\", oauth_version=\"1.0\"",
                ref.toString());
    }
}
